package top.yukonga.miuix.files.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

/** Grupo de recentes no estilo ES: "2 hora atrás  2 imagem de .Statuses". */
data class RecentGroup(val time: String, val label: String, val folder: File, val items: List<FileItem>, val media: Boolean)

fun relativeTime(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60000
    return when {
        m < 1 -> "agora"
        m < 60 -> "$m min atrás"
        m < 1440 -> "${m / 60} hora atrás"
        else -> "${m / 1440} dia atrás"
    }
}

fun kindLabel(items: List<FileItem>): String {
    val e = items.first().extension.lowercase()
    val n = items.size
    return when {
        e in setOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> "$n imagens"
        e in setOf("mp4", "mkv", "avi", "mov", "3gp", "webm") -> "$n vídeos"
        e in setOf("mp3", "flac", "wav", "m4a", "ogg", "opus") -> "$n músicas"
        e == "apk" -> "$n APKs"
        else -> "$n arquivos"
    }
}

fun groupRecents(files: List<FileItem>): List<RecentGroup> =
    files.sortedByDescending { it.lastModified }
        .groupBy { (it.file.parentFile?.path ?: "") + "|" + relativeTime(it.lastModified) }
        .values.map { g ->
            val parent = g.first().file.parentFile ?: File("/")
            RecentGroup(relativeTime(g.first().lastModified), "${kindLabel(g)} de ${parent.name}", parent, g, g.all { isMediaFile(it.file) })
        }

fun iconFor(item: FileItem): Int = when (item.extension.lowercase()) {
    "apk" -> R.drawable.format_apk
    "jpg", "jpeg", "png", "webp", "gif" -> R.drawable.format_picture
    "mp4", "mkv", "avi", "mov" -> R.drawable.format_video
    "mp3", "flac", "wav", "m4a" -> R.drawable.format_music
    "zip", "rar", "7z" -> R.drawable.format_zip
    "pdf" -> R.drawable.format_pdf
    "doc", "docx" -> R.drawable.format_word
    "xls", "xlsx", "csv" -> R.drawable.format_excel
    "ppt", "pptx" -> R.drawable.format_ppt
    "txt", "log", "json", "xml", "md" -> R.drawable.format_text
    else -> if (item.isDirectory) R.drawable.format_folder else R.drawable.format_unkown
}

@Composable
fun HomeScreen(
    onNavigateDir: (File) -> Unit,
    onNavigateCategory: (FileCategory) -> Unit,
    onNavigateCleaner: () -> Unit,
    onNavigateAppManager: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenFile: (File) -> Unit,
    onSearch: () -> Unit = {},
    onHistory: () -> Unit = {},
    onCloud: () -> Unit = {},
    onSender: () -> Unit = {},
    onTextEditor: () -> Unit = {},
    onNetwork: () -> Unit = {},
    onViewPc: () -> Unit = {},
    onAnalyzer: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    tabs: List<EsWindowTab> = emptyList(),
    activeTabId: String = "home",
    onTabSelect: (EsWindowTab) -> Unit = {},
    onTabClose: (EsWindowTab) -> Unit = {},
    onAddTab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    var volumes by remember { mutableStateOf(StorageManager.getCachedVolumes() ?: StorageManager.getStorageVolumes(context)) }
    var recentFiles by remember { mutableStateOf(StorageManager.getCachedRecents() ?: emptyList()) }
    var showRecent by remember { mutableStateOf(prefs.getBoolean(top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences.KEY_SHOW_RECENTS_HOME, true)) }
    var showNew by remember { mutableStateOf(false) }
    var showWindows by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val groups = remember(recentFiles) { groupRecents(recentFiles) }

    fun reload(force: Boolean = false) {
        volumes = StorageManager.getStorageVolumes(context)
        scope.launch {
            recentFiles = StorageManager.getRecentFiles(context = context, forceRefresh = force)
        }
    }
    LaunchedEffect(Unit) {
        if (recentFiles.isEmpty()) reload(force = false)
        // Pré-aquece em background as categorias e lista de apps para abrir com 0ms de espera
        scope.launch {
            FileCategory.entries.forEach { cat ->
                StorageManager.getCategoryFiles(cat, context = context)
            }
            top.yukonga.miuix.files.core.apps.AppManager.getInstalledApps(context)
            top.yukonga.miuix.files.core.cleaner.CleanerEngine.scanJunk(context)
        }
    }

    Box(modifier.fillMaxSize().background(EsBgColor)) {
        Column(Modifier.fillMaxSize()) {
            EsTopHeader(
                tabTitle = "Página Inicial",
                onMenuClick = onMenuClick,
                onTabCloseClick = { tabs.firstOrNull { it.id == activeTabId }?.let(onTabClose) },
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelect = onTabSelect,
                onTabClose = onTabClose,
                onAddTab = onAddTab
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().overScrollVertical(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { EsTopCategoryRow(onCategoryClick = onNavigateCategory) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        volumes.getOrNull(0)?.let { v -> EsStorageCard(v, { onNavigateDir(v.path) }, Modifier.weight(1f), Color(0xFF2F7DF6)) }
                        volumes.getOrNull(1)?.let { v -> EsStorageCard(v, { onNavigateDir(v.path) }, Modifier.weight(1f), Color(0xFF3FE0F0)) }
                    }
                }
                item {
                    Card(
                        Modifier.fillMaxWidth(), cornerRadius = 10.dp,
                        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 13.dp),
                        colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = Color.White),
                        pressFeedbackType = PressFeedbackType.Sink, onClick = onAnalyzer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.es_icon_analyse_pie), null, Modifier.size(38.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Analis. de espaço", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xCCFFFFFF))
                                Spacer(Modifier.height(2.dp))
                                Text("mais arquivos para limpar", fontSize = 10.sp, color = Color(0xFF999999))
                            }
                        }
                    }
                }
                item {
                    EsToolGrid(
                        onCleanerClick = onNavigateCleaner,
                        onCloudClick = onCloud,
                        onArchiveClick = { onNavigateCategory(FileCategory.ARCHIVES) },
                        onSenderClick = onSender,
                        onTextEditorClick = onTextEditor,
                        onNetworkClick = onNetwork,
                        onRecycleBinClick = { onNavigateCategory(FileCategory.RECYCLE_BIN) },
                        onDownloadClick = { onNavigateCategory(FileCategory.DOWNLOADS) },
                        onViewPcClick = onViewPc,
                        onAppManagerClick = onNavigateAppManager
                    )
                }
                // Arquivado Recentemente: tudo dentro de um único card, como no ES
                item {
                    Card(
                        Modifier.fillMaxWidth(), cornerRadius = 10.dp,
                        insideMargin = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 6.dp),
                        colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = Color.White)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Arquivado Recentemente:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6E6E6), modifier = Modifier.weight(1f))
                            Image(
                                painterResource(if (showRecent) R.drawable.toolbar_show else R.drawable.toolbar_hide), null,
                                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).clickable {
                                    showRecent = !showRecent
                                    prefs.edit().putBoolean(top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences.KEY_SHOW_RECENTS_HOME, showRecent).apply()
                                },
                                colorFilter = ColorFilter.tint(Color(0xFF8A8D91))
                            )
                        }
                        AnimatedVisibility(
                            visible = showRecent,
                            enter = expandVertically(animationSpec = folmeSpring(damping = 0.85f, response = 0.30f)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)) + fadeOut()
                        ) {
                            Column {
                                if (groups.isEmpty()) {
                                    Text("Nenhum arquivo recente", fontSize = 12.sp, color = Color(0xFF8A8D91), modifier = Modifier.padding(vertical = 16.dp))
                                }
                                groups.take(6).forEachIndexed { i, g ->
                                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A3D40)))
                                    RecentGroupBlock(g, onOpenFile, onNavigateDir)
                                }
                            }
                        }
                    }
                }
            }
            EsBottomBar(
                onNewClick = { newName = ""; showNew = true },
                onSearchClick = onSearch,
                onRefreshClick = { reload(force = true) },
                onWindowsClick = { showWindows = true },
                onHistoryClick = onHistory
            )
        }
    }

    if (showNew) {
        OverlayDialog(show = showNew, title = "Nova Pasta", onDismissRequest = { showNew = false }) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = "Digite o nome da pasta",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(text = "Cancelar", onClick = { showNew = false }, modifier = Modifier.weight(1f))
                TextButton(
                    text = "Criar",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val root = volumes.firstOrNull()?.path
                        if (root != null && newName.isNotBlank()) {
                            val d = File(root, newName.trim())
                            if (d.exists() || d.mkdirs()) onNavigateDir(d)
                        }
                        showNew = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    if (showWindows) {
        OverlayDialog(show = showWindows, title = "Janelas (${tabs.size})", onDismissRequest = { showWindows = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { tab ->
                    val active = tab.id == activeTabId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Color(0x332EC7E0) else Color(0x22FFFFFF))
                            .clickable {
                                showWindows = false
                                onTabSelect(tab)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painterResource(tab.iconRes),
                            null,
                            Modifier.size(18.dp),
                            colorFilter = if (tab.tintIcon) ColorFilter.tint(Color.White) else null
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(tab.title, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
                        if (tabs.size > 1) {
                            Text(
                                "✕",
                                fontSize = 14.sp,
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onTabClose(tab) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (onAddTab != null) {
                    TextButton(
                        text = "+ Nova Janela (Local)",
                        onClick = {
                            showWindows = false
                            onAddTab()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(text = "Fechar", onClick = { showWindows = false }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RecentGroupBlock(g: RecentGroup, onOpenFile: (File) -> Unit, onNavigateDir: (File) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onNavigateDir(g.folder) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(g.time, fontSize = 12.sp, color = Color(0xFF2EC7E0))
            Spacer(Modifier.width(12.dp))
            Text(g.label, fontSize = 12.sp, color = Color(0xFFA0A3A6), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("›", fontSize = 18.sp, color = Color(0xFF8A8D91))
        }
        Spacer(Modifier.height(10.dp))
        if (g.media) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                g.items.take(4).forEach { f ->
                    FileThumbnail(
                        f.file, iconFor(f),
                        Modifier.size(width = 72.dp, height = 72.dp).clip(RoundedCornerShape(4.dp)).clickable { onOpenFile(f.file) }
                    )
                }
            }
        } else {
            g.items.take(3).forEach { f ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenFile(f.file) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(iconFor(f)), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(StorageManager.formatFileSize(f.size), fontSize = 10.5.sp, color = Color(0xFFA0A3A6))
                    }
                }
            }
        }
    }
}

@Composable
fun EsDrawer(
    onClose: () -> Unit, onHome: () -> Unit, onLocal: () -> Unit, onRoot: () -> Unit,
    onCategory: (FileCategory) -> Unit, onCleaner: () -> Unit, onAnalyzer: () -> Unit, onApps: () -> Unit, onSettings: () -> Unit
) {
    Column(Modifier.fillMaxHeight().fillMaxWidth(0.8f).background(EsBgColor)) {
        Row(
            Modifier.fillMaxWidth().background(EsTopColor).statusBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(Color(0xFFEDEDED)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.toolbar_user), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color(0xFF31353D)))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("MiuiX Files", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Gerenciador de arquivos", fontSize = 11.sp, color = Color(0xFFB0B3B8))
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            item { DrawerSection("Favoritos") }
            item { DrawerItem(R.drawable.icon_home_tab_home, "Página Inicial", onHome) }
            item { DrawerItem(R.drawable.toolbar_local, "Armazenamento interno", onLocal) }
            item { DrawerItem(R.drawable.toolbar_root, "Dispositivo ( / )", onRoot) }
            item { DrawerSection("Biblioteca") }
            items(listOf(
                Triple(R.drawable.toolbar_image, "Fotografias", FileCategory.IMAGES),
                Triple(R.drawable.toolbar_music, "Música", FileCategory.AUDIO),
                Triple(R.drawable.toolbar_video, "Filmes", FileCategory.VIDEOS),
                Triple(R.drawable.toolbar_document, "Livros", FileCategory.DOCUMENTS),
                Triple(R.drawable.toolbar_app, "APP", FileCategory.APPS),
                Triple(R.drawable.toolbar_download, "Download", FileCategory.DOWNLOADS),
                Triple(R.drawable.toolbar_delete, "Reciclagem", FileCategory.RECYCLE_BIN)
            )) { (ic, t, c) -> DrawerItem(ic, t) { onCategory(c) } }
            item { DrawerSection("Ferramentas") }
            item { DrawerItem(R.drawable.toolbar_clean, "Cleaner", onCleaner) }
            item { DrawerItem(R.drawable.toolbar_analyer, "Analis. de espaço", onAnalyzer) }
            item { DrawerItem(R.drawable.toolbar_app, "Gerenciador de APPs", onApps) }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            DrawerBottom(R.drawable.toolbar_message, "Mensagem", Modifier.weight(1f), onClose)
            DrawerBottom(R.drawable.toolbar_theme, "Tema", Modifier.weight(1f), onSettings)
            DrawerBottom(R.drawable.toolbar_setting, "Configurações", Modifier.weight(1f), onSettings)
        }
    }
}

@Composable
private fun DrawerSection(t: String) =
    Text(t, fontSize = 12.sp, color = Color(0xFF8A8D91), modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp))

@Composable
private fun DrawerItem(icon: Int, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Color(0xFFD0D2D6)))
        Spacer(Modifier.width(18.dp))
        Text(title, fontSize = 14.sp, color = Color(0xFFE6E6E6))
    }
}

@Composable
private fun DrawerBottom(icon: Int, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color(0xFF8A8D91)))
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 12.sp, color = Color(0xFFE6E6E6), textAlign = TextAlign.Center)
    }
}
