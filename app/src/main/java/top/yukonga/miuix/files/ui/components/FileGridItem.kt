package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val folderBadgeIcon: Int? = when (item.name.lowercase()) {
        "dcim", "camera" -> R.drawable.toolbar_image
        "download", "downloads" -> R.drawable.toolbar_download
        "android" -> R.drawable.toolbar_setting
        "movies", "video", "videos" -> R.drawable.toolbar_video
        "music", "audio", "podcasts", "recordings", "ringtones", "notifications", "alarms" -> R.drawable.toolbar_music
        "pictures", "screenshots", "images" -> R.drawable.toolbar_image
        "backups" -> R.drawable.toolbar_backup
        else -> null
    }

    val iconRes = when {
        item.isDirectory -> R.drawable.format_folder
        item.extension == "apk" -> R.drawable.format_apk
        item.extension in setOf("jpg", "jpeg", "png", "webp", "gif") -> R.drawable.format_picture
        item.extension in setOf("mp4", "mkv", "avi", "mov") -> R.drawable.format_video
        item.extension in setOf("mp3", "flac", "wav", "m4a") -> R.drawable.format_music
        item.extension in setOf("zip", "rar", "7z", "tar") -> R.drawable.format_zip
        item.extension == "pdf" -> R.drawable.format_pdf
        item.extension in setOf("doc", "docx") -> R.drawable.format_word
        item.extension in setOf("xls", "xlsx", "csv") -> R.drawable.format_excel
        item.extension in setOf("ppt", "pptx") -> R.drawable.format_ppt
        item.extension in setOf("txt", "log", "json", "xml", "md", "ini", "conf", "prop") -> R.drawable.format_text
        item.extension in setOf("html", "htm") -> R.drawable.format_html
        item.extension == "epub" -> R.drawable.format_ebook
        item.extension == "torrent" -> R.drawable.format_torrent
        else -> R.drawable.format_unkown
    }

    val backgroundColor = if (isSelected) {
        Color(0xFF007AFF).copy(alpha = 0.28f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .pressable(
                interactionSource = interactionSource,
                indication = SinkFeedback()
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isMediaFile(item.file)) {
                    FileThumbnail(
                        file = item.file,
                        fallbackRes = iconRes,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = item.name,
                        modifier = Modifier.size(56.dp)
                    )
                }

                if (item.isDirectory && folderBadgeIcon != null) {
                    Image(
                        painter = painterResource(folderBadgeIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = 2.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.name,
                fontSize = 11.5.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                Checkbox(
                    state = if (isSelected) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                    onClick = onClick
                )
            }
        }
    }
}
