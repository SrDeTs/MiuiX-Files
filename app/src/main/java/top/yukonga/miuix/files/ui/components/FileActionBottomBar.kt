package top.yukonga.miuix.files.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun FileActionBottomBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    restoreMode: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EsBgColor)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EsDividerColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionItem(iconRes = R.drawable.toolbar_copy, label = "Copiar", onClick = onCopy, modifier = Modifier.weight(1f))
            ActionItem(iconRes = R.drawable.toolbar_cut, label = "Cortar", onClick = onCut, modifier = Modifier.weight(1f))
            ActionItem(iconRes = R.drawable.toolbar_delete, label = "Excluir", onClick = onDelete, modifier = Modifier.weight(1f))
            if (restoreMode) {
                ActionItem(iconRes = R.drawable.toolbar_refresh, label = "Restaurar", onClick = onRename, modifier = Modifier.weight(1f))
            } else if (selectedCount == 1) {
                ActionItem(iconRes = R.drawable.toolbar_rename, label = "Renomear", onClick = onRename, modifier = Modifier.weight(1f))
            }
            ActionItem(iconRes = R.drawable.toolbar_more, label = "Mais", onClick = onMore, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 1:1 ES File Explorer Paste Bottom Bar:
 * [ Colar ]   [ + Novo ]   [ ↩ Cancelar ]
 */
@Composable
fun EsClipboardBottomBar(
    onPaste: () -> Unit,
    onNewFolder: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EsBgColor)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EsDividerColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionItem(
                iconRes = R.drawable.toolbar_paste,
                label = "Colar",
                onClick = onPaste,
                modifier = Modifier.weight(1f)
            )
            ActionItem(
                iconRes = R.drawable.toolbar_new,
                label = "Novo",
                onClick = onNewFolder,
                modifier = Modifier.weight(1f)
            )
            ActionItem(
                iconRes = R.drawable.toolbar_return,
                label = "Cancelar",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 1:1 ES File Explorer right-edge semi-circular clipboard handle tab (`📋`).
 * Tapping expands a compact preview of queued items with Folme spring animation.
 */
@Composable
fun EsClipboardSideTab(
    clipboard: FileOperations.ClipboardItem?,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(clipboard) {
        if (clipboard == null) expanded = false
    }

    AnimatedVisibility(
        visible = clipboard != null,
        enter = slideInHorizontally(animationSpec = folmeSpring(damping = 0.82f, response = 0.32f)) { it } + fadeIn(),
        exit = slideOutHorizontally(animationSpec = folmeSpring(damping = 0.9f, response = 0.25f)) { it } + fadeOut(),
        modifier = modifier
    ) {
        val clip = clipboard ?: return@AnimatedVisibility
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(animationSpec = folmeSpring(damping = 0.85f, response = 0.3f), expandFrom = Alignment.End) + fadeIn(),
                exit = shrinkHorizontally(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f), shrinkTowards = Alignment.End) + fadeOut()
            ) {
                val modeLabel = if (clip.mode == FileOperations.ClipboardMode.CUT) "Recortar" else "Copiar"
                Row(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xEE31353D))
                        .clickable { onPaste() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.widthIn(max = 170.dp)) {
                        Text(
                            text = "$modeLabel (${clip.files.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = clip.files.firstOrNull()?.name ?: "",
                            fontSize = 10.5.sp,
                            color = Color(0xFFB0B3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClear() }
                            .padding(4.dp)
                    )
                }
            }

            // Right-edge D-shaped handle tab matching ES File Explorer screenshot 3
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                    .background(Color(0xE6383C42))
                    .pressable(interactionSource = interactionSource, indication = SinkFeedback())
                    .clickable(interactionSource = interactionSource, indication = null) {
                        expanded = !expanded
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.toolbar_clipboard),
                    contentDescription = "Área de transferência",
                    modifier = Modifier.size(19.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFD0D3D8))
                )
            }
        }
    }
}

/**
 * Compact inline transfer progress bar using genuine MIUIX LinearProgressIndicator + Folme spring.
 * Never blocks the full screen.
 */
@Composable
fun InlineTransferProgressStrip(
    state: FileOperations.TransferState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = folmeSpring(damping = 0.9f, response = 0.25f),
        label = "transfer_progress"
    )

    AnimatedVisibility(
        visible = state.active,
        enter = fadeIn(animationSpec = folmeSpring(damping = 0.9f, response = 0.25f)),
        exit = fadeOut(animationSpec = folmeSpring(damping = 0.9f, response = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EsTopColor)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.title}: ${state.currentFile}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = EsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}% (${state.completedCount}/${state.totalCount})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = animatedProgress,
                height = 4.dp
            )
        }
    }
}

@Composable
private fun ActionItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .pressable(
                interactionSource = interactionSource,
                indication = SinkFeedback()
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(23.dp),
            colorFilter = ColorFilter.tint(EsTextPrimary.copy(alpha = 0.88f))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = EsTextSecondary
        )
    }
}
