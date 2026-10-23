package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text

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
    val iconRes = when {
        item.isDirectory -> R.drawable.format_folder
        item.extension == "apk" -> R.drawable.format_apk
        item.extension in setOf("jpg", "jpeg", "png", "webp", "gif") -> R.drawable.format_picture
        item.extension in setOf("mp4", "mkv", "avi", "mov") -> R.drawable.format_video
        item.extension in setOf("mp3", "flac", "wav", "m4a") -> R.drawable.format_music
        item.extension in setOf("zip", "rar", "7z", "tar") -> R.drawable.format_zip
        item.extension == "pdf" -> R.drawable.format_pdf
        item.extension in setOf("txt", "log", "json", "xml", "md") -> R.drawable.format_text
        else -> R.drawable.format_document
    }

    val backgroundColor = if (isSelected) {
        Color(0xFF007AFF).copy(alpha = 0.22f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Real ES File Explorer Icon or Media Thumbnail
        if (isMediaFile(item.file)) {
            FileThumbnail(
                file = item.file,
                fallbackRes = iconRes,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White,
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
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Selection checkbox
        if (isSelectionMode) {
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                state = if (isSelected) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                onClick = onClick
            )
        }
    }
}
