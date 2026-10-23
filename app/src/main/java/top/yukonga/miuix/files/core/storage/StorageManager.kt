package top.yukonga.miuix.files.core.storage

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.SortOption
import top.yukonga.miuix.files.core.model.StorageVolumeInfo
import java.io.File

object StorageManager {

    fun getStorageVolumes(context: Context): List<StorageVolumeInfo> {
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
                // Discover mount root
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

        return volumes
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortOption: SortOption = SortOption.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val items = rawFiles
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                val count = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
                FileItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified(),
                    itemCount = count,
                    isHidden = file.name.startsWith(".")
                )
            }

        // Directories first, then files sorted according to SortOption
        val dirs = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val sortedDirs = sortList(dirs, sortOption)
        val sortedFiles = sortList(files, sortOption)

        sortedDirs + sortedFiles
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

    suspend fun getCategoryFiles(
        category: FileCategory,
        rootDir: File = Environment.getExternalStorageDirectory(),
        maxItems: Int = 500
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return@withContext listFiles(downloadDir)
        }
        if (category == FileCategory.RECYCLE_BIN) {
            val trashDir = File(Environment.getExternalStorageDirectory(), ".estrongs/recycle")
            if (!trashDir.exists()) trashDir.mkdirs()
            return@withContext listFiles(trashDir, showHidden = true)
        }

        val result = mutableListOf<FileItem>()
        fun scan(dir: File) {
            if (result.size >= maxItems) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".") && child.name != ".trash") continue
                if (child.isDirectory) {
                    // Skip Android/data or hidden deep dirs for fast category preview
                    if (!child.absolutePath.contains("/Android/data") && !child.absolutePath.contains("/Android/obb")) {
                        scan(child)
                    }
                } else {
                    val ext = child.extension.lowercase()
                    if (category.extensions.contains(ext)) {
                        result.add(
                            FileItem(
                                file = child,
                                name = child.name,
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

        scan(rootDir)
        result.sortedByDescending { it.lastModified }
    }

    suspend fun getRecentFiles(
        rootDir: File = Environment.getExternalStorageDirectory(),
        limit: Int = 20
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<FileItem>()
        fun scan(dir: File, depth: Int) {
            if (depth > 4) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (!child.absolutePath.contains("/Android/")) {
                        scan(child, depth + 1)
                    }
                } else {
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
        scan(rootDir, 0)
        candidates.sortedByDescending { it.lastModified }.take(limit)
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
