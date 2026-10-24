package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }

    var showHidden by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, false)) }
    var themeMode by remember { mutableIntStateOf(prefs.getInt(MiuiXFilesPreferences.KEY_THEME_MODE, 0)) }

    val themeLabels = listOf("Seguir Sistema", "Claro", "Escuro", "Monet MIUI")

    Column(modifier = modifier.fillMaxSize().background(top.yukonga.miuix.files.ui.components.EsBgColor)) {
        Row(
            Modifier.fillMaxWidth().background(top.yukonga.miuix.files.ui.components.EsTopColor).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onNavigateBack), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(top.yukonga.miuix.files.R.drawable.toolbar_return), null, Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.ui.graphics.Color.White))
            }
            Spacer(Modifier.width(8.dp))
            Text("Configurações", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().overScrollVertical(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SmallTitle(text = "Aparência e Exibição")
                Spacer(modifier = Modifier.height(6.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ArrowPreference(
                            title = "Tema da Interface",
                            summary = themeLabels.getOrElse(themeMode) { "Sistema" },
                            onClick = {
                                val nextMode = (themeMode + 1) % 4
                                themeMode = nextMode
                                prefs.edit().putInt(MiuiXFilesPreferences.KEY_THEME_MODE, nextMode).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Arquivos Ocultos",
                            summary = "Exibir arquivos e pastas iniciados por ponto",
                            checked = showHidden,
                            onCheckedChange = { checked ->
                                showHidden = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, checked).apply()
                            }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "Sobre o MiuiX Files")
                Spacer(modifier = Modifier.height(6.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MiuiX Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Versão 1.0.0",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gerenciador de arquivos moderno construído com Jetpack Compose e compose-miuix-ui, inspirado nas funcionalidades e potência do ES File Explorer com o design minimalista da MIUI / HyperOS.",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}
