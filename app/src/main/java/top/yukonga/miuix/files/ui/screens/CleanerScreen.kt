package top.yukonga.miuix.files.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.core.cleaner.CleanScanResult
import top.yukonga.miuix.files.core.cleaner.CleanerEngine
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanResult by remember { mutableStateOf<CleanScanResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var isCleaning by remember { mutableStateOf(false) }
    var cleanedTotalBytes by remember { mutableStateOf<Long?>(null) }

    fun runScan() {
        scope.launch {
            isScanning = true
            cleanedTotalBytes = null
            scanResult = CleanerEngine.scanJunk(context)
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        runScan()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = "Limpeza de Sistema",
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNavigateBack)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = "←", fontSize = 20.sp)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Status Header
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (cleanedTotalBytes != null) Color(0xFF30D158).copy(alpha = 0.2f)
                                    else MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (cleanedTotalBytes != null) "✨" else "🧹",
                                fontSize = 34.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isScanning) {
                            Text(
                                text = "Analisando arquivos inúteis...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        } else if (cleanedTotalBytes != null) {
                            Text(
                                text = "Limpeza concluída!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF30D158)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Você liberou ${StorageManager.formatFileSize(cleanedTotalBytes!!)} de espaço.",
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        } else {
                            val totalBytes = scanResult?.totalReclaimableBytes ?: 0L
                            Text(
                                text = StorageManager.formatFileSize(totalBytes),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalBytes > 0) Color(0xFFFF5964) else MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalBytes > 0) "Espaço que pode ser liberado com segurança" else "Seu dispositivo está limpo!",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            // Categories Breakdown
            if (!isScanning && scanResult != null && cleanedTotalBytes == null) {
                item {
                    SmallTitle(text = "Detalhes da Análise")
                }

                items(scanResult!!.categories) { cat ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${cat.files.size} itens • ${cat.description}",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            Text(
                                text = StorageManager.formatFileSize(cat.totalSize),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurface
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(if (isCleaning) "Limpando..." else "Limpar Arquivos Inúteis Agora")
                    }
                }
            }
        }
    }
}
