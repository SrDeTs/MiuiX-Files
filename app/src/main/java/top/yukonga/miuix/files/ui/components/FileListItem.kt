package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    modifier: Modifier = Modifier,
    isDropTarget: Boolean = false,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var itemWindowOffset by remember { mutableStateOf(Offset.Zero) }
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
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
        isDropTarget -> Color(0xFF3FE0F0).copy(alpha = 0.30f)
        isSelected -> Color(0xFF007AFF).copy(alpha = 0.22f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                itemWindowOffset = bounds.topLeft
                onBoundsChanged?.invoke(bounds)
            }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isDropTarget) {
                    Modifier.border(1.5.dp, Color(0xFF3FE0F0), RoundedCornerShape(8.dp))
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (onDragStart != null) {
                    Modifier.pointerInput(item.path) {
                        var dragStarted = false
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localPos ->
                                currentOnLongClick()
                                dragStarted = false
                                currentOnDragStart?.invoke(itemWindowOffset + localPos)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragStarted = true
                                currentOnDragMove?.invoke(itemWindowOffset + change.position)
                            },
                            onDragEnd = {
                                if (dragStarted) currentOnDragEnd?.invoke()
                                else currentOnDragEnd?.invoke()
                                dragStarted = false
                            },
                            onDragCancel = {
                                currentOnDragEnd?.invoke()
                                dragStarted = false
                            }
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Real ES File Explorer Icon or Media Thumbnail
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
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
            if (item.isDirectory && folderBadgeIcon != null) {
                Image(
                    painter = painterResource(folderBadgeIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = 2.dp)
                )
            } else if (item.isDirectory && folderAppIcon != null) {
                Image(
                    bitmap = folderAppIcon!!,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .offset(y = 2.dp)
                        .clip(CircleShape)
                )
            }
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
                color = EsTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = if (item.isDirectory) {
                val count = remember(item.path, item.lastModified) {
                    item.file.list()?.size ?: 0
                }
                "$count itens | ${StorageManager.formatDate(item.lastModified)}"
            } else {
                "${StorageManager.formatFileSize(item.size)} | ${StorageManager.formatDate(item.lastModified)}"
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = EsTextSecondary,
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
