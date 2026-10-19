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

@Composable
fun EsStorageCard(
    volume: StorageVolumeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gaugeColor: Color = Color(0xFF00B0FF)
) {
    val percentage = (volume.progress * 100).toInt()
    val usedGb = volume.usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val totalGb = volume.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val formattedSubtitle = String.format(Locale.getDefault(), "%.2f GB / %.2f GB", usedGb, totalGb)

    Card(
        modifier = modifier,
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        colors = CardDefaults.defaultColors(
            color = Color(0xFF23262B),
            contentColor = Color.White
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularStorageGauge(
                progress = volume.progress,
                percentage = percentage,
                progressColor = gaugeColor,
                size = 44.dp,
                strokeWidth = 3.8.dp,
                trackColor = Color(0xFF383C45)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Armazenament...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = formattedSubtitle,
                    fontSize = 10.5.sp,
                    color = Color(0xFF9E9EA0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
