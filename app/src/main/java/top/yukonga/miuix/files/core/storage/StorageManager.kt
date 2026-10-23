package top.yukonga.miuix.files.core.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.SortOption
import top.yukonga.miuix.files.core.model.StorageVolumeInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object StorageManager {

    private var cachedVolumes: List<StorageVolumeInfo>? = null
    private var cachedRecentFiles: List<FileItem>? = null
    private val cachedCategories = ConcurrentHashMap<FileCategory, List<FileItem>>()
    private val cachedDirs = ConcurrentHashMap<String, List<FileItem>>()

    fun getCachedCategory(category: FileCategory): List<FileItem>? = cachedCategories[category]
    fun getCachedDir(dir: File): List<FileItem>? = cachedDirs[dir.absolutePath]
    fun getCachedRecents(): List<FileItem>? = cachedRecentFiles
    fun getCachedVolumes(): List<StorageVolumeInfo>? = cachedVolumes

    fun invalidateCaches() {
        cachedRecentFiles = null
        cachedCategories.clear()
        cachedDirs.clear()
    }

    fun getStorageVolumes(context: Context): List<StorageVolumeInfo> {
        cachedVolumes?.let { return it }
        val volumes = mutableListOf<StorageVolumeInfo>()

        // Internal Primary Storage
        val primaryDir = Environment.getExternalStorageDirectory()
        val totalPrimary = primaryDir.totalSpace
        val freePrimary = primaryDir.freeSpace
        volumes.add(
            StorageVolumeInfo(
                title = "Armazenamento Interno",
                path = primaryDir,
                totalBytes = totalPrimary,
                freeBytes = freePrimary,
                isRemovable = false
            )
        )

        // Secondary / Removable SD Cards
        val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (f in externalFilesDirs) {
            if (f != null && Environment.isExternalStorageRemovable(f)) {
                var root = f
                while (root.parentFile != null && !root.parentFile!!.absolutePath.equals("/storage", ignoreCase = true)) {
                    root = root.parentFile ?: break
                }
                if (root.exists() && root.canRead()) {
                    volumes.add(
                        StorageVolumeInfo(
                            title = "Cartão SD (${root.name})",
                            path = root,
                            totalBytes = root.totalSpace,
                            freeBytes = root.freeSpace,
                            isRemovable = true
                        )
                    )
                }
            }
        }

        cachedVolumes = volumes
        return volumes
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortOption: SortOption = SortOption.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val items = ArrayList<FileItem>(rawFiles.size)
        for (file in rawFiles) {
            val name = file.name
            val hidden = name.startsWith(".")
            if (!showHidden && hidden) continue
            val isDir = file.isDirectory
            items.add(
                FileItem(
                    file = file,
                    name = name,
                    path = file.absolutePath,
                    isDirectory = isDir,
                    size = if (isDir) 0L else file.length(),
                    lastModified = file.lastModified(),
                    itemCount = 0, // Evita N chamadas extras de listFiles() que travam pastas grandes
                    isHidden = hidden
                )
            )
        }

        val dirs = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val result = sortList(dirs, sortOption) + sortList(files, sortOption)
        if (!showHidden && sortOption == SortOption.NAME_ASC) {
            cachedDirs[directory.absolutePath] = result
        }
        result
    }

    private fun sortList(list: List<FileItem>, sortOption: SortOption): List<FileItem> {
        return when (sortOption) {
            SortOption.NAME_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.NAME_DESC -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.DATE_DESC -> list.sortedByDescending { it.lastModified }
            SortOption.DATE_ASC -> list.sortedBy { it.lastModified }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOption.SIZE_ASC -> list.sortedBy { it.size }
            SortOption.TYPE_ASC -> list.sortedWith(compareBy<FileItem> { it.extension }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }

    private fun queryMediaStore(
        context: Context,
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        limit: Int = 500
    ): List<FileItem> {
        val out = ArrayList<FileItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext() && out.size < limit) {
                    val path = cursor.getString(dataCol) ?: continue
                    val slashIdx = path.lastIndexOf('/')
                    val name = if (slashIdx >= 0) path.substring(slashIdx + 1) else path
                    if (name.isEmpty() || name.startsWith(".")) continue
                    val size = cursor.getLong(sizeCol)
                    val modified = cursor.getLong(dateCol) * 1000L
                    out.add(
                        FileItem(
                            file = File(path),
                            name = name,
                            path = path,
                            isDirectory = false,
                            size = size,
                            lastModified = modified
                        )
                    )
                }
            }
        }
        return out
    }

    suspend fun getCategoryFiles(
        category: FileCategory,
        rootDir: File = Environment.getExternalStorageDirectory(),
        maxItems: Int = 500,
        context: Context? = null,
        forceRefresh: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cachedCategories[category]?.let { return@withContext it }
        }

        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return@withContext listFiles(downloadDir).also { cachedCategories[category] = it }
        }
        if (category == FileCategory.RECYCLE_BIN) {
            val trashDir = File(Environment.getExternalStorageDirectory(), ".estrongs/recycle")
            if (!trashDir.exists()) trashDir.mkdirs()
            return@withContext listFiles(trashDir, showHidden = true).also { cachedCategories[category] = it }
        }

        // Busca ultra-rápida via MediaStore quando Context está disponível (<15ms em vez de varrer árvore de pastas)
        if (context != null) {
            val mediaResult = when (category) {
                FileCategory.IMAGES -> queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, limit = maxItems)
                FileCategory.VIDEOS -> queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, limit = maxItems)
                FileCategory.AUDIO -> queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, limit = maxItems)
                else -> {
                    val exts = category.extensions.toList()
                    val placeholders = exts.joinToString(" OR ") { "${MediaStore.MediaColumns.DATA} LIKE ?" }
                    val args = exts.map { "%.$it" }.toTypedArray()
                    queryMediaStore(context, MediaStore.Files.getContentUri("external"), placeholders, args, maxItems)
                }
            }
            if (mediaResult.isNotEmpty()) {
                cachedCategories[category] = mediaResult
                return@withContext mediaResult
            }
        }

        // Fallback leve em diretórios rasos caso MediaStore esteja vazio
        val result = ArrayList<FileItem>()
        fun scan(dir: File, depth: Int) {
            if (depth > 3 || result.size >= maxItems) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                val name = child.name
                if (name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (name != "Android") scan(child, depth + 1)
                } else {
                    val ext = child.extension.lowercase()
                    if (category.extensions.contains(ext)) {
                        result.add(
                            FileItem(
                                file = child,
                                name = name,
                                path = child.absolutePath,
                                isDirectory = false,
                                size = child.length(),
                                lastModified = child.lastModified()
                            )
                        )
                        if (result.size >= maxItems) return
                    }
                }
            }
        }

        scan(rootDir, 0)
        result.sortedByDescending { it.lastModified }.also { cachedCategories[category] = it }
    }

    suspend fun getRecentFiles(
        rootDir: File = Environment.getExternalStorageDirectory(),
        limit: Int = 24,
        context: Context? = null,
        forceRefresh: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && limit <= 30) {
            cachedRecentFiles?.let { return@withContext it.take(limit) }
        }

        if (context != null) {
            val fast = (
                queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, limit = limit) +
                queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, limit = limit)
            ).sortedByDescending { it.lastModified }.take(limit)
            if (fast.isNotEmpty()) {
                if (limit <= 30) cachedRecentFiles = fast
                return@withContext fast
            }
        }

        // Fallback rápido apenas nas pastas principais de mídia/downloads (sem varrer milhares de subpastas)
        val targetDirs = listOf(
            File(rootDir, "DCIM/Camera"),
            File(rootDir, "DCIM/Screenshots"),
            File(rootDir, "Pictures"),
            File(rootDir, "Pictures/Screenshots"),
            File(rootDir, "Download"),
            File(rootDir, "Movies"),
            File(rootDir, "WhatsApp/Media/WhatsApp Images"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        )
        val candidates = ArrayList<FileItem>()
        for (dir in targetDirs) {
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (!child.isDirectory && !child.name.startsWith(".")) {
                    candidates.add(
                        FileItem(
                            file = child,
                            name = child.name,
                            path = child.absolutePath,
                            isDirectory = false,
                            size = child.length(),
                            lastModified = child.lastModified()
                        )
                    )
                }
            }
        }
        val sorted = candidates.sortedByDescending { it.lastModified }.take(limit)
        if (limit <= 30) cachedRecentFiles = sorted
        sorted
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val formatted = String.format(java.util.Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
