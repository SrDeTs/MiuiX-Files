package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun EsTopHeader(
    tabTitle: String = "Página Inicial",
    onMenuClick: () -> Unit,
    onTabCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF31353D))
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger Menu Icon (☰)
        val menuInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .pressable(
                    interactionSource = menuInteraction,
                    indication = SinkFeedback()
                )
                .clickable(
                    interactionSource = menuInteraction,
                    indication = null,
                    onClick = onMenuClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.toolbar_menu),
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Tab Pill Capsule
        val tabInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF464A51))
                .pressable(
                    interactionSource = tabInteraction,
                    indication = SinkFeedback()
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.icon_home_tab_home),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = tabTitle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTabCloseClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0)
                )
            }
        }
    }
}
