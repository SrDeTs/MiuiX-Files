package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.StorageVolumeInfo
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.*
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.io.File

@Composable
fun HomeScreen(
    onNavigateDir: (File) -> Unit,
    onNavigateCategory: (FileCategory) -> Unit,
    onNavigateCleaner: () -> Unit,
    onNavigateAppManager: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var volumes by remember { mutableStateOf<List<StorageVolumeInfo>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var showRecent by remember { mutableStateOf(true) }

    fun reload() {
        volumes = StorageManager.getStorageVolumes(context)
        scope.launch {
            recentFiles = StorageManager.getRecentFiles()
        }
    }

    LaunchedEffect(Unit) {
        volumes = StorageManager.getStorageVolumes(context)
        recentFiles = StorageManager.getRecentFiles()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF292C2E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 ES Top Header
            EsTopHeader(
                tabTitle = "Página Inicial",
                onMenuClick = onNavigateSettings,
                onTabCloseClick = {}
            )

            // Main Scrollable Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top 5 Circular Categories
                item {
                    EsTopCategoryRow(onCategoryClick = onNavigateCategory)
                }

                // Storage Cards (side-by-side)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (volumes.isNotEmpty()) {
                            EsStorageCard(
                                volume = volumes[0],
                                onClick = { onNavigateDir(volumes[0].path) },
                                modifier = Modifier.weight(1f),
                                gaugeColor = Color(0xFF00B0FF)
                            )
                        }

                        if (volumes.size > 1) {
                            EsStorageCard(
                                volume = volumes[1],
                                onClick = { onNavigateDir(volumes[1].path) },
                                modifier = Modifier.weight(1f),
                                gaugeColor = Color(0xFF00E5FF)
                            )
                        } else if (volumes.isNotEmpty()) {
                            // Secondary simulated card matching ES presentation
                            val secondaryVol = StorageVolumeInfo(
                                title = "Armazenament...",
                                path = volumes[0].path,
                                totalBytes = 29L * 1024 * 1024 * 1024 + 710L * 1024 * 1024,
                                freeBytes = 15L * 1024 * 1024 * 1024 + 810L * 1024 * 1024
                            )
                            EsStorageCard(
                                volume = secondaryVol,
                                onClick = { onNavigateDir(volumes[0].path) },
                                modifier = Modifier.weight(1f),
                                gaugeColor = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                // Analis. de espaço Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        colors = CardDefaults.defaultColors(
                            color = Color(0xFF23262B),
                            contentColor = Color.White
                        ),
                        pressFeedbackType = PressFeedbackType.Sink,
                        onClick = onNavigateCleaner
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.es_icon_analyse_pie),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Analis. de espaço",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "mais arquivos para limpar",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }

                // 10 Tools Grid
                item {
                    EsToolGrid(
                        onCleanerClick = onNavigateCleaner,
                        onAppManagerClick = onNavigateAppManager,
                        onRecycleBinClick = { onNavigateCategory(FileCategory.RECYCLE_BIN) },
                        onDownloadClick = { onNavigateCategory(FileCategory.DOWNLOADS) }
                    )
                }

                // Arquivado Recentemente Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arquivado Recentemente:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (showRecent) "👁" else "🙈",
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showRecent = !showRecent }
                                .padding(4.dp)
                        )
                    }
                }

                // Recent Items
                if (showRecent) {
                    if (recentFiles.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 12.dp,
                                insideMargin = PaddingValues(12.dp),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0xFF23262B),
                                    contentColor = Color.White
                                )
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Recente",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF00B0FF),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "•  ${recentFiles.size} arquivos",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF9E9E9E)
                                            )
                                        }
                                        Text(
                                            text = ">",
                                            fontSize = 13.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    for (item in recentFiles.take(4)) {
                                        RecentFileRow(item = item, onClick = { onOpenFile(item.file) })
                                    }
                                }
                            }
                        }
                    }
                }

                // Favoritos Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(14.dp),
                        colors = CardDefaults.defaultColors(
                            color = Color(0xFF23262B),
                            contentColor = Color.White
                        ),
                        pressFeedbackType = PressFeedbackType.Sink,
                        onClick = { }
                    ) {
                        Column {
                            Text(
                                text = "Favoritos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ver tudo >",
                                fontSize = 11.5.sp,
                                color = Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Fixed Bottom Bar (Novo, Procurar, Atualizar, Janelas, Histórico)
            EsBottomBar(
                onNewClick = { },
                onSearchClick = { },
                onRefreshClick = { reload() },
                onWindowsClick = { },
                onHistoryClick = { }
            )
        }
    }
}

@Composable
fun RecentFileRow(
    item: FileItem,
    onClick: () -> Unit
) {
    val iconRes = when (item.extension) {
        "apk" -> R.drawable.format_apk
        "jpg", "jpeg", "png", "webp", "gif" -> R.drawable.format_picture
        "mp4", "mkv", "avi", "mov" -> R.drawable.format_video
        "mp3", "flac", "wav" -> R.drawable.format_music
        "zip", "rar", "7z" -> R.drawable.format_zip
        "pdf" -> R.drawable.format_pdf
        else -> R.drawable.format_document
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${StorageManager.formatFileSize(item.size)} • ${StorageManager.formatDate(item.lastModified)}",
                fontSize = 10.5.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}
