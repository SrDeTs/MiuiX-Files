package top.yukonga.miuix.files.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

object MiuiXFilesPreferences {
    const val PREFS_NAME = "miuix_files_prefs"
    const val KEY_THEME_MODE = "theme_mode" // 0: Auto/System, 1: Light, 2: Dark, 3: Monet
    const val KEY_SHOW_HIDDEN = "show_hidden_files"
    const val KEY_SORT_OPTION = "sort_option" // 0: Nome (A-Z), 1: Nome (Z-A), 2: Data (Recente), 3: Tamanho (Maior), 4: Tipo
    const val KEY_DEFAULT_VIEW_MODE = "default_view_mode" // 0: Grade (4 colunas), 1: Grade Compacta (5 colunas), 2: Lista Detalhada
    const val KEY_FOLDERS_FIRST = "folders_first"
    const val KEY_PIN_MEDIA_FOLDERS = "pin_media_folders" // Fixar DCIM e Download no topo
    const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    const val KEY_SHOW_FOLDER_APP_ICONS = "show_folder_app_icons"
    const val KEY_SHOW_RECENTS_HOME = "show_recents_home"
    const val KEY_USE_RECYCLE_BIN = "use_recycle_bin"
    const val KEY_CONFIRM_BEFORE_DELETE = "confirm_before_delete"
    const val KEY_BACKUP_DIR = "apk_backup_dir"
    const val KEY_DEFAULT_HOME_DIR = "default_home_dir"
    const val KEY_ROOT_EXPLORER = "root_explorer_enabled"
}

@Composable
fun MiuiXFilesTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }
    var themeMode by remember(prefs) { mutableIntStateOf(prefs.getInt(MiuiXFilesPreferences.KEY_THEME_MODE, 0)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MiuiXFilesPreferences.KEY_THEME_MODE) {
                themeMode = prefs.getInt(MiuiXFilesPreferences.KEY_THEME_MODE, 0)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val mode = when (themeMode) {
        1 -> ColorSchemeMode.Light
        2 -> ColorSchemeMode.Dark
        3 -> if (Build.VERSION.SDK_INT >= 31) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        else -> ColorSchemeMode.Dark
    }

    val controller = remember(mode) { ThemeController(mode) }

    MiuixTheme(
        controller = controller,
        content = content
    )
}
