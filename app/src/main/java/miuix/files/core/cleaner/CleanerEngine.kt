package miuix.files.core.cleaner

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

    private var cachedScan: CleanScanResult? = null
    fun getCachedScan(): CleanScanResult? = cachedScan

    suspend fun scanJunk(context: Context, forceRefresh: Boolean = false): CleanScanResult = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cachedScan?.let { return@withContext it }
        }
        val root = Environment.getExternalStorageDirectory()
        val tempFiles = mutableListOf<File>()
        val logFiles = mutableListOf<File>()
        val emptyDirs = mutableListOf<File>()
        val cacheFiles = mutableListOf<File>()

        // App Internal & External Cache
        fun collectCache(dir: File?) {
            if (dir == null || !dir.exists()) return
            dir.walkTopDown().maxDepth(3).forEach {
                if (it.isFile) cacheFiles.add(it)
            }
        }

        collectCache(context.cacheDir)
        collectCache(context.externalCacheDir)

        // Storage Junk Scan (up to 2 levels for instant response)
        fun scanDir(dir: File, depth: Int) {
            if (depth > 2) return
            val list = dir.listFiles() ?: return
            for (f in list) {
                val name = f.name
                if (f.isDirectory) {
                    if (name.equals(".thumbnails", ignoreCase = true) || name.equals(".cache", ignoreCase = true)) {
                        f.listFiles()?.forEach { if (it.isFile) cacheFiles.add(it) }
                    } else if (!name.startsWith(".") && name != "Android") {
                        val sub = f.list()
                        if (sub != null && sub.isEmpty()) {
                            emptyDirs.add(f)
                        } else if (depth < 2) {
                            scanDir(f, depth + 1)
                        }
                    }
                } else {
                    val lower = name.lowercase()
                    if (lower.endsWith(".tmp") || lower.endsWith(".temp") || lower.endsWith("~")) {
                        tempFiles.add(f)
                    } else if (lower.endsWith(".log")) {
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

        CleanScanResult(categories).also { cachedScan = it }
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
        cachedScan = null
        cleanedBytes
    }
}
