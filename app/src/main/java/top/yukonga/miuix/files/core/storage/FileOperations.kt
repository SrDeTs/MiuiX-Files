package top.yukonga.miuix.files.core.storage

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

    var currentClipboard: ClipboardItem? = null

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
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun paste(targetDir: File): Result<Int> = withContext(Dispatchers.IO) {
        val clip = currentClipboard ?: return@withContext Result.failure(Exception("Área de transferência vazia"))
        var count = 0
        try {
            for (src in clip.files) {
                val dest = File(targetDir, src.name)
                if (clip.mode == ClipboardMode.CUT) {
                    if (src.renameTo(dest)) {
                        count++
                    } else {
                        copyRecursive(src, dest)
                        src.deleteRecursively()
                        count++
                    }
                } else {
                    copyRecursive(src, dest)
                    count++
                }
            }
            if (clip.mode == ClipboardMode.CUT) {
                currentClipboard = null
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
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
