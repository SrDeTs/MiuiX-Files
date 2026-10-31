package miuix.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import miuix.files.R
import miuix.files.core.model.FileItem
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

fun getEsFolderBadgeRes(folderName: String): Int? = when (folderName.lowercase()) {
    "dcim", "camera" -> R.drawable.logo_dcim
    "download", "downloads" -> R.drawable.logo_download
    "android" -> R.drawable.logo_android
    ".android_secure" -> R.drawable.logo_android_secure
    "backups", "estrongs" -> R.drawable.logo_estrongs
    "movies" -> R.drawable.logo_movies
    "video", "videos" -> R.drawable.logo_video
    "music" -> R.drawable.logo_music
    "pictures", "screenshots", "images", "photo", "photos" -> R.drawable.logo_pictures
    "ringtones" -> R.drawable.logo_ringtones
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    isDropTarget: Boolean = false,
    onBoundsChanged: ((Rect) -> Unit)? = null
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    val folderBadgeIcon: Int? = if (item.isDirectory) getEsFolderBadgeRes(item.name) else null
    var folderAppIcon by remember(item.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.path) {
        if (item.isDirectory && folderBadgeIcon == null) {
            folderAppIcon = withContext(Dispatchers.IO) {
                loadFolderAppBitmap(context, item.file)
            }
        }
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

    val backgroundColor = when {
        isBeingDragged -> Color.Transparent
        isDropTarget -> Color(0xFF3FE0F0).copy(alpha = 0.34f)
        isSelected -> Color(0xFF007AFF).copy(alpha = 0.28f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                onBoundsChanged?.invoke(coords.boundsInWindow())
            }
            .graphicsLayer { alpha = if (isBeingDragged) 0f else 1f }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isDropTarget) {
                    Modifier.border(1.5.dp, Color(0xFF3FE0F0), RoundedCornerShape(8.dp))
                } else Modifier
            )
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
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isMediaFile(item.file)) {
                    FileThumbnail(
                        file = item.file,
                        fallbackRes = iconRes,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = item.name,
                        modifier = Modifier.size(64.dp)
                    )
                }

                if (item.isDirectory && folderBadgeIcon != null) {
                    Image(
                        painter = painterResource(folderBadgeIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .offset(y = 3.dp)
                    )
                } else if (item.isDirectory && folderAppIcon != null) {
                    Image(
                        bitmap = folderAppIcon!!,
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .offset(y = 3.dp)
                            .clip(CircleShape)
                    )
                }

                // 1:1 ES File Explorer bottom-right white/blue circular selection badge
                if (isSelectionMode && !isBeingDragged) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF2F7DF6) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFFB5BAC0),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text(
                                text = "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.name,
                fontSize = 13.5.sp,
                color = EsTextPrimary.copy(alpha = 0.88f),
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
