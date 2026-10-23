package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.core.model.StorageVolumeInfo
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.util.Locale

val EsCardColor = Color(0xFF2D3032)
val EsBgColor = Color(0xFF292C2E)
val EsTopColor = Color(0xFF31353D)

@Composable
fun EsStorageCard(
    volume: StorageVolumeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gaugeColor: Color = Color(0xFF2F7DF6)
) {
    val gb = 1024.0 * 1024.0 * 1024.0
    val sub = String.format(Locale("pt", "BR"), "%.2f GB / %.2f GB", volume.usedBytes / gb, volume.totalBytes / gb)
    Card(
        modifier = modifier,
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(start = 10.dp, end = 8.dp, top = 15.dp, bottom = 15.dp),
        colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = Color.White),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularStorageGauge(volume.progress, (volume.progress * 100).toInt(), progressColor = gaugeColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(volume.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(sub, fontSize = 10.sp, color = Color(0xFFA0A3A6), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
