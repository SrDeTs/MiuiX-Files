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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.util.Locale

val EsCardColor: Color
    @Composable get() = MiuixTheme.colorScheme.surfaceContainer

val EsBgColor: Color
    @Composable get() = MiuixTheme.colorScheme.background

val EsTopColor: Color
    @Composable get() = MiuixTheme.colorScheme.surfaceContainerHigh

val EsTextPrimary: Color
    @Composable get() = MiuixTheme.colorScheme.onSurface

val EsTextSecondary: Color
    @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantSummary

val EsDividerColor: Color
    @Composable get() = MiuixTheme.colorScheme.dividerLine

val EsAccentColor: Color
    @Composable get() = MiuixTheme.colorScheme.primaryVariant

@Composable
fun EsStorageCard(
    volume: StorageVolumeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gaugeColor: Color = MiuixTheme.colorScheme.primary
) {
    val gb = 1024.0 * 1024.0 * 1024.0
    val sub = String.format(Locale("pt", "BR"), "%.2f GB / %.2f GB", volume.usedBytes / gb, volume.totalBytes / gb)
    Card(
        modifier = modifier,
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 13.dp),
        colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularStorageGauge(volume.progress, (volume.progress * 100).toInt(), progressColor = gaugeColor)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(volume.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EsTextPrimary.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(sub, fontSize = 10.sp, color = EsTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
