package top.yukonga.miuix.files.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text

/** Medidor circular igual ao do ES: anel branco grosso com arco colorido. */
@Composable
fun CircularStorageGauge(
    progress: Float,
    percentage: Int,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    strokeWidth: Dp = 5.dp,
    trackColor: Color = Color(0xFFEDEDED),
    progressColor: Color = Color(0xFF2F7DF6)
) {
    val animated by animateFloatAsState(targetValue = progress, label = "gauge")
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val s = strokeWidth.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(trackColor, -90f, 360f, false, tl, arc, style = Stroke(s))
            drawArc(progressColor, -90f, 360f * animated, false, tl, arc, style = Stroke(s, cap = StrokeCap.Butt))
        }
        Text(text = "$percentage%", fontSize = 9.5.sp, color = Color.White)
    }
}
