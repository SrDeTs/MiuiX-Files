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
    const val KEY_SORT_OPTION = "sort_option"
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
