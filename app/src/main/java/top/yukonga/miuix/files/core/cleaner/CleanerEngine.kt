package top.yukonga.miuix.files.core.cleaner

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class JunkCategory(
    val title: String,
    val description: String,
    val files: List<File>,
    val totalSize: Long = files.sumOf { it.length() }
)

data class CleanScanResult(
    val categories: List<JunkCategory>,
    val totalReclaimableBytes: Long = categories.sumOf { it.totalSize },
    val totalFileCount: Int = categories.sumOf { it.files.size }
)

object CleanerEngine {

    suspend fun scanJunk(context: Context): CleanScanResult = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val tempFiles = mutableListOf<File>()
        val logFiles = mutableListOf<File>()
        val emptyDirs = mutableListOf<File>()
        val cacheFiles = mutableListOf<File>()

        // App Internal & External Cache
        fun collectCache(dir: File?) {
            if (dir == null || !dir.exists()) return
            dir.walkTopDown().forEach {
                if (it.isFile) cacheFiles.add(it)
            }
        }

        collectCache(context.cacheDir)
        collectCache(context.externalCacheDir)

        // Storage Junk Scan (up to 4 levels)
        fun scanDir(dir: File, depth: Int) {
            if (depth > 4) return
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    if (f.name.equals(".thumbnails", ignoreCase = true) || f.name.equals(".cache", ignoreCase = true)) {
                        f.walkTopDown().forEach { if (it.isFile) cacheFiles.add(it) }
                    } else if (f.list()?.isEmpty() == true && !f.name.startsWith(".")) {
                        emptyDirs.add(f)
                    } else if (!f.absolutePath.contains("/Android/data")) {
                        scanDir(f, depth + 1)
                    }
                } else {
                    val name = f.name.lowercase()
                    if (name.endsWith(".tmp") || name.endsWith(".temp") || name.endsWith("~")) {
                        tempFiles.add(f)
                    } else if (name.endsWith(".log")) {
                        logFiles.add(f)
                    }
                }
            }
        }

        scanDir(root, 0)

        val categories = mutableListOf<JunkCategory>()
        if (cacheFiles.isNotEmpty()) {
            categories.add(JunkCategory("Cache de Aplicativos & Miniaturas", "Arquivos temporários e miniaturas geradas", cacheFiles))
        }
        if (tempFiles.isNotEmpty()) {
            categories.add(JunkCategory("Arquivos Temporários", "Arquivos .tmp e sobras de processos", tempFiles))
        }
        if (logFiles.isNotEmpty()) {
            categories.add(JunkCategory("Arquivos de Log", "Registros de erros e logs acumulados", logFiles))
        }
        if (emptyDirs.isNotEmpty()) {
            categories.add(JunkCategory("Pastas Vazias", "Diretórios órfãos sem arquivos", emptyDirs))
        }

        CleanScanResult(categories)
    }

    suspend fun cleanJunk(files: List<File>): Long = withContext(Dispatchers.IO) {
        var cleanedBytes = 0L
        for (f in files) {
            try {
                if (f.isDirectory) {
                    f.delete()
                } else {
                    val len = f.length()
                    if (f.delete()) {
                        cleanedBytes += len
                    }
                }
            } catch (_: Exception) {}
        }
        cleanedBytes
    }
}
