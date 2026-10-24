package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
            .background(Color(0xFF292C2E))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF383C42))
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

@Composable
fun ClipboardFloatingBar(
    clipboard: FileOperations.ClipboardItem,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeText = if (clipboard.mode == FileOperations.ClipboardMode.CUT) "Mover" else "Copiar"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF31353D))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$modeText ${clipboard.files.size} itens",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "Navegue até a pasta de destino",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B3B8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = "Cancelar",
                    onClick = onCancel
                )
                Button(
                    onClick = onPaste
                ) {
                    Text("Colar")
                }
            }
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
            colorFilter = ColorFilter.tint(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFB0B3B8)
        )
    }
}
