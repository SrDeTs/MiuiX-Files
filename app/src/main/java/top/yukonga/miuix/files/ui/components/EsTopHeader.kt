package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

data class EsWindowTab(
    val id: String,
    val title: String,
    val iconRes: Int,
    val tintIcon: Boolean = true,
    val closable: Boolean = true
)

@Composable
fun EsTopHeader(
    tabTitle: String = "Página Inicial",
    onMenuClick: () -> Unit,
    onTabCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuIconRes: Int = R.drawable.toolbar_menu,
    tabs: List<EsWindowTab> = emptyList(),
    activeTabId: String = "",
    onTabSelect: (EsWindowTab) -> Unit = {},
    onTabClose: (EsWindowTab) -> Unit = {},
    onAddTab: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(activeTabId, tabs.size) {
        if (tabs.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val activePillBg = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHighest
    val inactivePillBg = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer
    val primaryText = EsTextPrimary
    val secondaryText = EsTextSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EsTopColor)
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger Menu Icon (☰) — abre a gaveta lateral em todas as telas
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
                painter = painterResource(menuIconRes),
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(primaryText)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (tabs.isEmpty()) {
            // Fallback single Tab Pill Capsule
            val tabInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(activePillBg)
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
                    colorFilter = ColorFilter.tint(primaryText)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = tabTitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryText
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
                        color = secondaryText
                    )
                }
            }
        } else {
            // 1:1 Multi-tab strip matching ES File Explorer:
            // Active tab shows full pill [icon + title + ✕]; inactive tabs show compact icon or subtle pill so user can switch freely!
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    val isActive = tab.id == activeTabId
                    val tabInteraction = remember { MutableInteractionSource() }
                    if (isActive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(activePillBg)
                                .pressable(
                                    interactionSource = tabInteraction,
                                    indication = SinkFeedback()
                                )
                                .clickable(
                                    interactionSource = tabInteraction,
                                    indication = null,
                                    onClick = { onTabSelect(tab) }
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                colorFilter = if (tab.tintIcon) ColorFilter.tint(primaryText) else null
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = tab.title,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = primaryText
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onTabClose(tab) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 11.sp,
                                    color = secondaryText
                                )
                            }
                        }
                    } else {
                        // Inactive tab: compact clickable pill with icon + title
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(inactivePillBg)
                                .pressable(
                                    interactionSource = tabInteraction,
                                    indication = SinkFeedback()
                                )
                                .clickable(
                                    interactionSource = tabInteraction,
                                    indication = null,
                                    onClick = { onTabSelect(tab) }
                                )
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(tab.iconRes),
                                contentDescription = tab.title,
                                modifier = Modifier.size(15.dp),
                                colorFilter = if (tab.tintIcon) ColorFilter.tint(secondaryText) else null
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = secondaryText
                            )
                        }
                    }
                }

                if (onAddTab != null) {
                    val addInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(inactivePillBg)
                            .pressable(interactionSource = addInteraction, indication = SinkFeedback())
                            .clickable(interactionSource = addInteraction, indication = null, onClick = onAddTab),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.toolbar_new),
                            contentDescription = "Nova janela",
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(primaryText)
                        )
                    }
                }
            }
        }
    }
}

