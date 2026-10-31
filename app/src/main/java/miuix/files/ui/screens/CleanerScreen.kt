package miuix.files.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import miuix.files.R
import miuix.files.core.cleaner.CleanerEngine
import miuix.files.core.storage.StorageManager
import miuix.files.ui.components.EsBgColor
import miuix.files.ui.components.EsCardColor
import miuix.files.ui.components.EsTextPrimary
import miuix.files.ui.components.EsTextSecondary
import miuix.files.ui.components.EsTopColor
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cached = remember { CleanerEngine.getCachedScan() }
    var scanResult by remember { mutableStateOf(cached) }
    var isScanning by remember { mutableStateOf(cached == null) }
    var isCleaning by remember { mutableStateOf(false) }
    var cleanedTotalBytes by remember { mutableStateOf<Long?>(null) }

    fun runScan(force: Boolean = false) {
        scope.launch {
            if (scanResult == null || force) isScanning = true
            cleanedTotalBytes = null
            scanResult = CleanerEngine.scanJunk(context, forceRefresh = force)
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        runScan(force = false)
    }

    Column(modifier = modifier.fillMaxSize().background(EsBgColor)) {
        Row(
            Modifier.fillMaxWidth().background(EsTopColor).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onNavigateBack), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.toolbar_return), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(EsTextPrimary))
            }
            Spacer(Modifier.width(8.dp))
            Text("Analis. de espaço / Cleaner", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EsTextPrimary, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable { runScan(force = true) }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.toolbar_refresh), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(EsTextPrimary))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().overScrollVertical().navigationBarsPadding(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.es_tool_cleaner),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isScanning) {
                            Text(
                                text = "Analisando arquivos residuais...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = EsTextPrimary
                            )
                        } else if (cleanedTotalBytes != null) {
                            Text(
                                text = "Limpeza concluída!",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Liberados ${StorageManager.formatFileSize(cleanedTotalBytes!!)} de espaço.",
                                fontSize = 13.sp,
                                color = EsTextSecondary
                            )
                        } else {
                            val totalBytes = scanResult?.totalReclaimableBytes ?: 0L
                            Text(
                                text = StorageManager.formatFileSize(totalBytes),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalBytes > 0) "Lixo eletrônico e cache que podem ser limpos" else "Nenhum arquivo residual encontrado",
                                fontSize = 12.5.sp,
                                color = EsTextSecondary
                            )
                        }
                    }
                }
            }

            if (!isScanning && scanResult != null && cleanedTotalBytes == null) {
                items(scanResult!!.categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 10.dp,
                        colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = EsTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${cat.files.size} itens • ${cat.description}",
                                    fontSize = 11.5.sp,
                                    color = EsTextSecondary
                                )
                            }
                            Text(
                                text = StorageManager.formatFileSize(cat.totalSize),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = !isCleaning && (scanResult?.totalReclaimableBytes ?: 0L) > 0L,
                        onClick = {
                            scope.launch {
                                isCleaning = true
                                val allFiles = scanResult!!.categories.flatMap { it.files }
                                val reclaimed = CleanerEngine.cleanJunk(allFiles)
                                cleanedTotalBytes = reclaimed
                                isCleaning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isCleaning) "Limpando..." else "Limpar Agora")
                    }
                }
            }
        }
    }
}
