package miuix.files.core.model

import java.io.File

/**
 * File model representing a file or directory on device storage.
 */
data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val itemCount: Int = 0,
    val isHidden: Boolean = file.name.startsWith("."),
    val isSelected: Boolean = false
) {
    val extension: String
        get() = if (isDirectory) "" else file.extension.lowercase()
}

/**
 * Storage volume representing an internal or external partition.
 */
data class StorageVolumeInfo(
    val title: String,
    val path: File,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean = false
) {
    val usedBytes: Long
        get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    val progress: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * ES File Explorer file categories.
 */
enum class FileCategory(val label: String, val extensions: Set<String>) {
    IMAGES("Imagens", setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic")),
    VIDEOS("Vídeos", setOf("mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "ts")),
    AUDIO("Músicas", setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus", "wma")),
    DOCUMENTS("Documentos", setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "epub", "csv")),
    APPS("Aplicativos", setOf("apk", "xapk", "apks")),
    ARCHIVES("Compactados", setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")),
    DOWNLOADS("Downloads", emptySet()),
    RECYCLE_BIN("Lixeira", emptySet())
}

/**
 * Sorting criteria.
 */
enum class SortOption {
    NAME_ASC, NAME_DESC,
    DATE_DESC, DATE_ASC,
    SIZE_DESC, SIZE_ASC,
    TYPE_ASC
}
