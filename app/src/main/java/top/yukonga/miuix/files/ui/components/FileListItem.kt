package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = when {
        item.isDirectory -> Color(0xFFFFB703) // MIUI Folder Yellow
        item.extension in setOf("jpg", "jpeg", "png", "webp", "gif") -> Color(0xFF00B4D8)
        item.extension in setOf("mp4", "mkv", "avi", "mov") -> Color(0xFFFF5964)
        item.extension in setOf("mp3", "flac", "wav", "m4a") -> Color(0xFF9D4EDD)
        item.extension in setOf("pdf", "doc", "docx", "txt", "md") -> Color(0xFFFF9E00)
        item.extension in setOf("zip", "rar", "7z", "tar") -> Color(0xFF3A86FF)
        item.extension in setOf("apk", "xapk") -> Color(0xFF2EC4B6)
        else -> Color(0xFF8E8E93)
    }

    val iconEmoji = when {
        item.isDirectory -> "📁"
        item.extension in setOf("jpg", "jpeg", "png", "webp", "gif") -> "🖼️"
        item.extension in setOf("mp4", "mkv", "avi", "mov") -> "🎬"
        item.extension in setOf("mp3", "flac", "wav", "m4a") -> "🎵"
        item.extension in setOf("pdf") -> "📕"
        item.extension in setOf("doc", "docx", "txt", "md") -> "📄"
        item.extension in setOf("zip", "rar", "7z", "tar") -> "🗜️"
        item.extension in setOf("apk", "xapk") -> "📱"
        else -> "📄"
    }

    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = if (item.isDirectory) {
                "${item.itemCount} itens | ${StorageManager.formatDate(item.lastModified)}"
            } else {
                "${StorageManager.formatFileSize(item.size)} | ${StorageManager.formatDate(item.lastModified)}"
            }
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Selection checkbox if in selection mode
        if (isSelectionMode) {
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                state = if (isSelected) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                onClick = onClick
            )
        }
    }
}
