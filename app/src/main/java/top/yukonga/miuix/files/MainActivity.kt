package top.yukonga.miuix.files

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.EsWindowTab
import top.yukonga.miuix.files.ui.screens.*
import top.yukonga.miuix.files.ui.theme.MiuiXFilesTheme
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Scaffold
import java.io.File

sealed class Screen {
    data object Home : Screen()
    data class Explorer(val directory: File) : Screen()
    data class Category(val category: FileCategory) : Screen()
    data object Cleaner : Screen()
    data object AppManager : Screen()
    data object Settings : Screen()
    data object Search : Screen()
    data object History : Screen()
    data class TextEditor(val file: File? = null) : Screen()
    data class ImageViewer(val file: File) : Screen()
    data class ZipViewer(val file: File) : Screen()
    data class AudioPlayer(val file: File) : Screen()
    data object ViewOnPc : Screen()
    data class Network(val title: String) : Screen()
    data object Sender : Screen()
    data object Analyzer : Screen()
}

data class WindowSession(
    val id: String,
    val screen: Screen
) {
    fun toTab(): EsWindowTab = when (val s = screen) {
        is Screen.Home -> EsWindowTab(id = id, title = "Página Inicial", iconRes = R.drawable.icon_home_tab_home, tintIcon = true)
        is Screen.Explorer -> {
            val isRoot = s.directory.absolutePath == "/"
            val label = when {
                isRoot -> "Dispositivo"
                s.directory.name == "0" || s.directory.absolutePath == "/sdcard" -> "Local"
                else -> s.directory.name.ifBlank { "Local" }
            }
            EsWindowTab(id = id, title = label, iconRes = R.drawable.icon_home_tab_sdcard, tintIcon = true)
        }
        is Screen.Category -> {
            val catIcon = when (s.category) {
                FileCategory.IMAGES -> R.drawable.es_cat_photo
                FileCategory.VIDEOS -> R.drawable.es_cat_movie
                FileCategory.DOCUMENTS -> R.drawable.es_cat_doc
                FileCategory.AUDIO -> R.drawable.es_cat_music
                FileCategory.APPS -> R.drawable.es_cat_app
                FileCategory.ARCHIVES -> R.drawable.format_zip
                FileCategory.DOWNLOADS -> R.drawable.es_tool_downloader
                FileCategory.RECYCLE_BIN -> R.drawable.es_tool_recycle
            }
            EsWindowTab(id = id, title = s.category.label, iconRes = catIcon, tintIcon = false)
        }
        else -> EsWindowTab(id = id, title = "Janela", iconRes = R.drawable.icon_home_tab_sdcard, tintIcon = true)
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkStoragePermissions()

        setContent {
            MiuiXFilesTheme {
                val defaultLocalDir = remember { Environment.getExternalStorageDirectory() }
                val windows = remember {
                    mutableStateListOf(
                        WindowSession(id = "home", screen = Screen.Home)
                    )
                }
                var activeWindowIndex by remember { mutableIntStateOf(0) }
                var previousWindowIndex by remember { mutableIntStateOf(0) }

                // Overlay screens (Search, Cleaner, Viewers, Settings, etc.) pushed above the active window
                val overlayStack = remember { mutableStateListOf<Screen>() }
                var previousOverlayDepth by remember { mutableIntStateOf(0) }
                var drawerOpen by remember { mutableStateOf(false) }
                var nextTabCounter by remember { mutableIntStateOf(1) }

                val currentOverlay = overlayStack.lastOrNull()
                val activeWindow = windows.getOrNull(activeWindowIndex.coerceIn(0, (windows.size - 1).coerceAtLeast(0)))
                    ?: WindowSession("home", Screen.Home)
                val currentScreen = currentOverlay ?: activeWindow.screen

                val isForward = if (overlayStack.size != previousOverlayDepth) {
                    overlayStack.size > previousOverlayDepth
                } else {
                    activeWindowIndex >= previousWindowIndex
                }
                SideEffect {
                    previousOverlayDepth = overlayStack.size
                    previousWindowIndex = activeWindowIndex
                }

                fun openOrSwitchWindow(targetScreen: Screen) {
                    overlayStack.clear()
                    val existingIdx = windows.indexOfFirst { w ->
                        when {
                            w.screen is Screen.Home && targetScreen is Screen.Home -> true
                            w.screen is Screen.Category && targetScreen is Screen.Category -> w.screen.category == targetScreen.category
                            w.screen is Screen.Explorer && targetScreen is Screen.Explorer -> w.screen.directory.absolutePath == targetScreen.directory.absolutePath
                            else -> false
                        }
                    }
                    if (existingIdx >= 0) {
                        activeWindowIndex = existingIdx
                    } else {
                        val newId = "win_${nextTabCounter++}"
                        windows.add(WindowSession(id = newId, screen = targetScreen))
                        activeWindowIndex = windows.lastIndex
                    }
                }

                fun closeWindow(tabId: String) {
                    if (windows.size <= 1) {
                        // Never leave 0 windows; reset the single window to Home
                        windows[0] = WindowSession(id = "home", screen = Screen.Home)
                        activeWindowIndex = 0
                        return
                    }
                    val idx = windows.indexOfFirst { it.id == tabId }
                    if (idx >= 0) {
                        windows.removeAt(idx)
                        if (activeWindowIndex >= windows.size) {
                            activeWindowIndex = windows.lastIndex
                        } else if (activeWindowIndex > idx) {
                            activeWindowIndex--
                        }
                    }
                }

                BackHandler(enabled = drawerOpen || overlayStack.isNotEmpty() || activeWindowIndex > 0) {
                    when {
                        drawerOpen -> drawerOpen = false
                        overlayStack.isNotEmpty() -> overlayStack.removeAt(overlayStack.lastIndex)
                        activeWindowIndex > 0 -> activeWindowIndex = 0
                    }
                }

                val windowTabs = remember(windows.toList()) { windows.map { it.toTab() } }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = top.yukonga.miuix.files.ui.components.EsBgColor,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val textExts = remember { setOf("txt", "md", "json", "xml", "log", "csv", "ini", "conf", "prop", "sh", "kt", "java", "html", "css", "js") }
                        val imgExts = remember { setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic") }
                        val audioExts = remember { setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus") }
                        fun handleOpen(file: File) {
                            val ext = file.extension.lowercase()
                            when {
                                ext in textExts -> overlayStack.add(Screen.TextEditor(file))
                                ext in imgExts -> overlayStack.add(Screen.ImageViewer(file))
                                ext in audioExts -> overlayStack.add(Screen.AudioPlayer(file))
                                ext == "zip" || ext == "apk" -> overlayStack.add(Screen.ZipViewer(file))
                                else -> openFile(file)
                            }
                        }

                        val onSelectTab: (EsWindowTab) -> Unit = { tab ->
                            val idx = windows.indexOfFirst { it.id == tab.id }
                            if (idx >= 0) {
                                overlayStack.clear()
                                activeWindowIndex = idx
                            }
                        }
                        val onCloseTab: (EsWindowTab) -> Unit = { tab ->
                            closeWindow(tab.id)
                        }
                        val onAddNewLocalTab: () -> Unit = {
                            val newId = "win_${nextTabCounter++}"
                            windows.add(WindowSession(id = newId, screen = Screen.Explorer(defaultLocalDir)))
                            overlayStack.clear()
                            activeWindowIndex = windows.lastIndex
                        }

                        AnimatedContent(
                            targetState = Pair(activeWindow.id, currentScreen),
                            transitionSpec = {
                                if (isForward) {
                                    (slideInHorizontally(animationSpec = folmeSpring(damping = 0.88f, response = 0.34f)) { it } +
                                        fadeIn(animationSpec = folmeSpring(damping = 0.9f, response = 0.25f))) togetherWith
                                        (slideOutHorizontally(animationSpec = folmeSpring(damping = 0.9f, response = 0.34f)) { -(it / 4) } +
                                            fadeOut(animationSpec = folmeSpring(damping = 0.9f, response = 0.30f), targetAlpha = 0.85f))
                                } else {
                                    (slideInHorizontally(animationSpec = folmeSpring(damping = 0.88f, response = 0.32f)) { -(it / 4) } +
                                        fadeIn(animationSpec = folmeSpring(damping = 0.9f, response = 0.25f), initialAlpha = 0.85f)) togetherWith
                                        (slideOutHorizontally(animationSpec = folmeSpring(damping = 0.9f, response = 0.30f)) { it } +
                                            fadeOut(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)))
                                }
                            },
                            label = "miuix_nav_screen_transition"
                        ) { (_, screen) ->
                            when (screen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        onNavigateDir = { dir -> openOrSwitchWindow(Screen.Explorer(dir)) },
                                        onNavigateCategory = { cat -> openOrSwitchWindow(Screen.Category(cat)) },
                                        onNavigateCleaner = { overlayStack.add(Screen.Cleaner) },
                                        onNavigateAppManager = { overlayStack.add(Screen.AppManager) },
                                        onNavigateSettings = { overlayStack.add(Screen.Settings) },
                                        onOpenFile = { file -> handleOpen(file) },
                                        onSearch = { overlayStack.add(Screen.Search) },
                                        onHistory = { overlayStack.add(Screen.History) },
                                        onCloud = { overlayStack.add(Screen.Network("Unidade de Nuvem")) },
                                        onSender = { overlayStack.add(Screen.Sender) },
                                        onTextEditor = { overlayStack.add(Screen.TextEditor(null)) },
                                        onNetwork = { overlayStack.add(Screen.Network("Rede Local (LAN)")) },
                                        onViewPc = { overlayStack.add(Screen.ViewOnPc) },
                                        onAnalyzer = { overlayStack.add(Screen.Analyzer) },
                                        onMenuClick = { drawerOpen = true },
                                        tabs = windowTabs,
                                        activeTabId = activeWindow.id,
                                        onTabSelect = onSelectTab,
                                        onTabClose = onCloseTab,
                                        onAddTab = onAddNewLocalTab
                                    )
                                }
                                is Screen.Analyzer -> AnalyzerScreen(
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) },
                                    onOpenDir = {
                                        overlayStack.clear()
                                        openOrSwitchWindow(Screen.Explorer(it))
                                    },
                                    onOpenFile = { handleOpen(it) }
                                )
                                is Screen.Search -> SearchScreen(
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) },
                                    onOpen = { f ->
                                        if (f.isDirectory) {
                                            overlayStack.clear()
                                            openOrSwitchWindow(Screen.Explorer(f.file))
                                        } else handleOpen(f.file)
                                    }
                                )
                                is Screen.History -> HistoryScreen(
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) },
                                    onOpen = { f ->
                                        if (f.isDirectory) {
                                            overlayStack.clear()
                                            openOrSwitchWindow(Screen.Explorer(f.file))
                                        } else handleOpen(f.file)
                                    }
                                )
                                is Screen.TextEditor -> TextEditorScreen(
                                    initialFile = screen.file,
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                )
                                is Screen.ImageViewer -> ImageViewerScreen(
                                    file = screen.file,
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                )
                                is Screen.ZipViewer -> ZipViewerScreen(
                                    zipFile = screen.file,
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) },
                                    onNavigateDir = { dir ->
                                        overlayStack.clear()
                                        openOrSwitchWindow(Screen.Explorer(dir))
                                    }
                                )
                                is Screen.AudioPlayer -> AudioPlayerScreen(
                                    file = screen.file,
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                )
                                is Screen.ViewOnPc -> ViewOnPcScreen(
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                )
                                is Screen.Network -> NetworkScreen(
                                    title = screen.title,
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) },
                                    onOpenPcServer = { overlayStack.add(Screen.ViewOnPc) }
                                )
                                is Screen.Sender -> SenderScreen(
                                    onBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                )
                                is Screen.Explorer -> {
                                    ExplorerScreen(
                                        initialDir = screen.directory,
                                        onNavigateBack = {
                                            val homeIdx = windows.indexOfFirst { it.screen is Screen.Home }
                                            if (homeIdx >= 0) activeWindowIndex = homeIdx
                                            else closeWindow(activeWindow.id)
                                        },
                                        onOpenFile = { file -> handleOpen(file) },
                                        onMenuClick = { drawerOpen = true },
                                        tabs = windowTabs,
                                        activeTabId = activeWindow.id,
                                        onTabSelect = onSelectTab,
                                        onTabClose = onCloseTab,
                                        onAddTab = onAddNewLocalTab,
                                        onCurrentDirChange = { newDir ->
                                            val idx = windows.indexOfFirst { it.id == activeWindow.id }
                                            if (idx >= 0) {
                                                windows[idx] = windows[idx].copy(screen = Screen.Explorer(newDir))
                                            }
                                        },
                                        onAnalyzerClick = { overlayStack.add(Screen.Analyzer) }
                                    )
                                }
                                is Screen.Category -> {
                                    CategoryScreen(
                                        category = screen.category,
                                        onNavigateBack = {
                                            val homeIdx = windows.indexOfFirst { it.screen is Screen.Home }
                                            if (homeIdx >= 0) activeWindowIndex = homeIdx
                                            else closeWindow(activeWindow.id)
                                        },
                                        onOpenFile = { file -> handleOpen(file) },
                                        onMenuClick = { drawerOpen = true },
                                        tabs = windowTabs,
                                        activeTabId = activeWindow.id,
                                        onTabSelect = onSelectTab,
                                        onTabClose = onCloseTab,
                                        onAddTab = onAddNewLocalTab,
                                        onAnalyzerClick = { overlayStack.add(Screen.Analyzer) }
                                    )
                                }
                                is Screen.Cleaner -> {
                                    CleanerScreen(
                                        onNavigateBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                    )
                                }
                                is Screen.AppManager -> {
                                    AppManagerScreen(
                                        onNavigateBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                    )
                                }
                                is Screen.Settings -> {
                                    SettingsScreen(
                                        onNavigateBack = { if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) }
                                    )
                                }
                            }
                        }

                        // Gaveta lateral global (☰) acessível de qualquer aba (Home, Local, Categorias)
                        AnimatedVisibility(
                            visible = drawerOpen,
                            enter = fadeIn(animationSpec = folmeSpring(damping = 0.9f, response = 0.28f)),
                            exit = fadeOut(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0x99000000))
                                    .clickable(remember { MutableInteractionSource() }, null) { drawerOpen = false }
                            )
                        }
                        AnimatedVisibility(
                            visible = drawerOpen,
                            enter = slideInHorizontally(animationSpec = folmeSpring(damping = 0.86f, response = 0.32f)) { -it },
                            exit = slideOutHorizontally(animationSpec = folmeSpring(damping = 0.92f, response = 0.25f)) { -it }
                        ) {
                            EsDrawer(
                                onClose = { drawerOpen = false },
                                onHome = {
                                    drawerOpen = false
                                    openOrSwitchWindow(Screen.Home)
                                },
                                onLocal = {
                                    drawerOpen = false
                                    val root = StorageManager.getCachedVolumes()?.firstOrNull()?.path ?: defaultLocalDir
                                    openOrSwitchWindow(Screen.Explorer(root))
                                },
                                onRoot = {
                                    drawerOpen = false
                                    openOrSwitchWindow(Screen.Explorer(File("/")))
                                },
                                onCategory = { cat ->
                                    drawerOpen = false
                                    openOrSwitchWindow(Screen.Category(cat))
                                },
                                onCleaner = {
                                    drawerOpen = false
                                    overlayStack.add(Screen.Cleaner)
                                },
                                onAnalyzer = {
                                    drawerOpen = false
                                    overlayStack.add(Screen.Analyzer)
                                },
                                onApps = {
                                    drawerOpen = false
                                    overlayStack.add(Screen.AppManager)
                                },
                                onSettings = {
                                    drawerOpen = false
                                    overlayStack.add(Screen.Settings)
                                },
                                onOpenFavorite = { fav ->
                                    drawerOpen = false
                                    if (fav.isDirectory) {
                                        openOrSwitchWindow(Screen.Explorer(fav))
                                    } else {
                                        handleOpen(fav)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1001
            )
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val extension = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir com"))
        } catch (_: Exception) {
            // Fallback generic chooser
            try {
                val uri = Uri.fromFile(file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                }
                startActivity(Intent.createChooser(intent, "Abrir com"))
            } catch (_: Exception) {}
        }
    }
}
