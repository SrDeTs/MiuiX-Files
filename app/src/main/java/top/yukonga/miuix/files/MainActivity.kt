package top.yukonga.miuix.files

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.ui.screens.*
import top.yukonga.miuix.files.ui.theme.MiuiXFilesTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

sealed class Screen {
    data object Home : Screen()
    data class Explorer(val directory: File) : Screen()
    data class Category(val category: FileCategory) : Screen()
    data object Cleaner : Screen()
    data object AppManager : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkStoragePermissions()

        setContent {
            MiuiXFilesTheme {
                val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
                val currentScreen = backStack.lastOrNull() ?: Screen.Home

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF292C2E),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    onNavigateDir = { dir -> backStack.add(Screen.Explorer(dir)) },
                                    onNavigateCategory = { cat -> backStack.add(Screen.Category(cat)) },
                                    onNavigateCleaner = { backStack.add(Screen.Cleaner) },
                                    onNavigateAppManager = { backStack.add(Screen.AppManager) },
                                    onNavigateSettings = { backStack.add(Screen.Settings) },
                                    onOpenFile = { file -> openFile(file) }
                                )
                            }
                            is Screen.Explorer -> {
                                ExplorerScreen(
                                    initialDir = screen.directory,
                                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    onOpenFile = { file -> openFile(file) }
                                )
                            }
                            is Screen.Category -> {
                                CategoryScreen(
                                    category = screen.category,
                                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    onOpenFile = { file -> openFile(file) }
                                )
                            }
                            is Screen.Cleaner -> {
                                CleanerScreen(
                                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                                )
                            }
                            is Screen.AppManager -> {
                                AppManagerScreen(
                                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    onNavigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                                )
                            }
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
