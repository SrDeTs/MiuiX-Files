package top.yukonga.miuix.files.core.storage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOperations {

    enum class ClipboardMode { COPY, CUT }

    data class ClipboardItem(
        val files: List<File>,
        val mode: ClipboardMode
    )

    data class TransferState(
        val active: Boolean = false,
        val title: String = "",
        val currentFile: String = "",
        val completedCount: Int = 0,
        val totalCount: Int = 0,
        val progress: Float = 0f
    )

    var currentClipboard by mutableStateOf<ClipboardItem?>(null)
    var transferState by mutableStateOf(TransferState())

    suspend fun createDirectory(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        val newDir = File(parent, name)
        if (newDir.exists()) {
            return@withContext Result.failure(Exception("Pasta já existe"))
        }
        if (newDir.mkdirs()) {
            Result.success(newDir)
        } else {
            Result.failure(Exception("Não foi possível criar a pasta"))
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val dest = File(file.parentFile, newName)
        if (dest.exists()) {
            return@withContext Result.failure(Exception("Já existe um arquivo com este nome"))
        }
        if (file.renameTo(dest)) {
            Result.success(dest)
        } else {
            Result.failure(Exception("Falha ao renomear"))
        }
    }

    /** Lixeira estilo ES: /sdcard/.estrongs/recycle + índice com caminho original. */
    val recycleDir: File get() = File(android.os.Environment.getExternalStorageDirectory(), ".estrongs/recycle").apply { mkdirs() }
    private val recycleIndex: File get() = File(recycleDir, ".index")

    private fun readIndex(): MutableMap<String, String> =
        runCatching {
            recycleIndex.readLines().mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap().toMutableMap()
        }.getOrDefault(mutableMapOf())

    private fun writeIndex(map: Map<String, String>) {
        runCatching { recycleIndex.writeText(map.entries.joinToString("\n") { "${it.key}\t${it.value}" }) }
    }

    fun isInRecycle(f: File) = f.absolutePath.startsWith(recycleDir.absolutePath)

    /** Move para a lixeira; arquivos já na lixeira são excluídos de vez. */
    suspend fun moveToRecycle(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        val idx = readIndex()
        var count = 0
        for (f in files) {
            if (isInRecycle(f)) {
                f.deleteRecursively(); idx.remove(f.name); count++; continue
            }
            val dest = File(recycleDir, "${System.currentTimeMillis()}_${f.name}")
            val ok = f.renameTo(dest) || runCatching { copyRecursive(f, dest); f.deleteRecursively() }.getOrDefault(false)
            if (ok) { idx[dest.name] = f.absolutePath; count++ }
        }
        writeIndex(idx)
        StorageManager.invalidateCaches()
        Result.success(count)
    }

    suspend fun restoreFromRecycle(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        val idx = readIndex()
        var count = 0
        for (f in files) {
            val orig = idx[f.name]?.let { File(it) } ?: File(android.os.Environment.getExternalStorageDirectory(), f.name.substringAfter('_'))
            orig.parentFile?.mkdirs()
            val target = if (orig.exists()) File(orig.parentFile, "restaurado_${orig.name}") else orig
            if (f.renameTo(target) || runCatching { copyRecursive(f, target); f.deleteRecursively() }.getOrDefault(false)) {
                idx.remove(f.name); count++
            }
        }
        writeIndex(idx)
        StorageManager.invalidateCaches()
        Result.success(count)
    }

    suspend fun emptyRecycle(): Result<Int> = withContext(Dispatchers.IO) {
        val list = recycleDir.listFiles().orEmpty()
        list.forEach { it.deleteRecursively() }
        StorageManager.invalidateCaches()
        Result.success(list.size)
    }

    suspend fun deleteFiles(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            for (f in files) {
                if (f.isDirectory) {
                    f.deleteRecursively()
                } else {
                    f.delete()
                }
                count++
            }
            StorageManager.invalidateCaches()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun paste(targetDir: File): Result<Int> = withContext(Dispatchers.IO) {
        val clip = currentClipboard ?: return@withContext Result.failure(Exception("Área de transferência vazia"))
        val totalFiles = clip.files.size.coerceAtLeast(1)
        val totalBytes = clip.files.sumOf { estimateSize(it) }.coerceAtLeast(1L)
        var copiedBytes = 0L
        val actionTitle = if (clip.mode == ClipboardMode.CUT) "Movendo" else "Copiando"

        transferState = TransferState(
            active = true,
            title = actionTitle,
            currentFile = clip.files.firstOrNull()?.name ?: "",
            completedCount = 0,
            totalCount = totalFiles,
            progress = 0.02f
        )

        var count = 0
        try {
            for ((index, src) in clip.files.withIndex()) {
                val dest = resolveTargetFile(targetDir, src, clip.mode)
                transferState = transferState.copy(
                    currentFile = src.name,
                    completedCount = index,
                    progress = (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.98f)
                )
                if (clip.mode == ClipboardMode.CUT) {
                    if (src.absolutePath == dest.absolutePath) {
                        copiedBytes += estimateSize(src)
                        count++
                    } else if (src.renameTo(dest)) {
                        copiedBytes += estimateSize(dest)
                        count++
                    } else {
                        copyRecursiveWithProgress(src, dest) { delta ->
                            copiedBytes += delta
                            transferState = transferState.copy(
                                progress = (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.99f)
                            )
                        }
                        src.deleteRecursively()
                        count++
                    }
                } else {
                    copyRecursiveWithProgress(src, dest) { delta ->
                        copiedBytes += delta
                        transferState = transferState.copy(
                            progress = (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.99f)
                        )
                    }
                    count++
                }
            }
            transferState = transferState.copy(
                completedCount = totalFiles,
                progress = 1f
            )
            currentClipboard = null
            StorageManager.invalidateCaches()
            kotlinx.coroutines.delay(320)
            transferState = TransferState(active = false)
            Result.success(count)
        } catch (e: Exception) {
            transferState = TransferState(active = false)
            Result.failure(e)
        }
    }

    private fun resolveTargetFile(targetDir: File, src: File, mode: ClipboardMode): File {
        var candidate = File(targetDir, src.name)
        if (!candidate.exists()) return candidate
        if (mode == ClipboardMode.CUT && src.absolutePath == candidate.absolutePath) return candidate
        val base = src.nameWithoutExtension.ifEmpty { src.name }
        val ext = src.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var idx = 1
        while (candidate.exists()) {
            candidate = File(targetDir, "$base ($idx)$ext")
            idx++
        }
        return candidate
    }

    private fun estimateSize(file: File): Long {
        if (!file.exists()) return 0L
        if (!file.isDirectory) return file.length().coerceAtLeast(1024L)
        return file.listFiles()?.sumOf { estimateSize(it) } ?: 4096L
    }

    private fun copyRecursiveWithProgress(source: File, destination: File, onBytes: (Long) -> Unit) {
        if (source.isDirectory) {
            if (!destination.exists()) destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursiveWithProgress(child, File(destination, child.name), onBytes)
            }
        } else {
            destination.parentFile?.mkdirs()
            val buffer = ByteArray(32 * 1024)
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        onBytes(read.toLong())
                    }
                }
            }
        }
    }

    private fun copyRecursive(source: File, destination: File) {
        if (source.isDirectory) {
            if (!destination.exists()) destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursive(child, File(destination, child.name))
            }
        } else {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    suspend fun zipFiles(sourceFiles: List<File>, targetZip: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZip))).use { out ->
                for (file in sourceFiles) {
                    addFileToZip(out, file, file.name)
                }
            }
            Result.success(targetZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(out: ZipOutputStream, file: File, baseName: String) {
        if (file.isDirectory) {
            val entryName = if (baseName.endsWith("/")) baseName else "$baseName/"
            out.putNextEntry(ZipEntry(entryName))
            out.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(out, child, "$entryName${child.name}")
            }
        } else {
            val buffer = ByteArray(1024 * 32)
            FileInputStream(file).use { input ->
                out.putNextEntry(ZipEntry(baseName))
                var len: Int
                while (input.read(buffer).also { len = it } > 0) {
                    out.write(buffer, 0, len)
                }
                out.closeEntry()
            }
        }
    }

    suspend fun unzip(zipFile: File, targetDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            val buffer = ByteArray(1024 * 32)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
