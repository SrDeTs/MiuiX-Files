package top.yukonga.miuix.files.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme

object MiuiXFilesPreferences {
    const val PREFS_NAME = "miuix_files_prefs"
    /**
     * Theme modes:
     * 0: ES Dark (Clássico - Padrão)
     * 1: MIUIX Escuro (HyperOS Dark)
     * 2: MIUIX Claro (HyperOS Light)
     * 3: Monet Escuro (Cores do Android / Papel de Parede)
     * 4: Monet Claro (Cores do Android / Papel de Parede)
     * 5: Seguir Sistema (Automático Claro / Escuro)
     */
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_SHOW_HIDDEN = "show_hidden_files"
    const val KEY_SORT_OPTION = "sort_option" // 0: Nome (A-Z), 1: Nome (Z-A), 2: Data (Recente), 3: Tamanho (Maior), 4: Tipo
    const val KEY_DEFAULT_VIEW_MODE = "default_view_mode" // 0: Grade (4 colunas), 1: Grade Compacta (5 colunas), 2: Lista Detalhada
    const val KEY_FOLDERS_FIRST = "folders_first"
    const val KEY_PIN_MEDIA_FOLDERS = "pin_media_folders"
    const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    const val KEY_SHOW_FOLDER_APP_ICONS = "show_folder_app_icons"
    const val KEY_SHOW_RECENTS_HOME = "show_recents_home"
    const val KEY_USE_RECYCLE_BIN = "use_recycle_bin"
    const val KEY_CONFIRM_BEFORE_DELETE = "confirm_before_delete"
    const val KEY_BACKUP_DIR = "apk_backup_dir"
    const val KEY_DEFAULT_HOME_DIR = "default_home_dir"
    const val KEY_ROOT_EXPLORER = "root_explorer_enabled"
    const val KEY_FAVORITES = "favorite_paths"

    fun getFavorites(context: Context): List<java.io.File> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()
        return raw.map { java.io.File(it) }.filter { it.exists() }.sortedBy { it.name.lowercase() }
    }

    fun toggleFavorite(context: Context, file: java.io.File): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()
        val path = file.absolutePath
        val added = if (current.contains(path)) {
            current.remove(path)
            false
        } else {
            current.add(path)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return added
    }

    fun isFavorite(context: Context, file: java.io.File): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().contains(file.absolutePath)
    }
}

val LocalAppThemeMode = staticCompositionLocalOf { 0 }

@Composable
fun rememberWallpaperOrSystemSeedColor(context: Context): Color {
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val c = context.resources.getColor(android.R.color.system_accent1_500, context.theme)
                Color(c)
            }.getOrDefault(Color(0xFF0073DD))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                val wm = WallpaperManager.getInstance(context)
                val wc = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val primary = wc?.primaryColor?.toArgb()
                if (primary != null) Color(primary) else Color(0xFF0073DD)
            }.getOrDefault(Color(0xFF0073DD))
        } else {
            Color(0xFF0073DD)
        }
    }
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

    val sysDark = isSystemInDarkTheme()
    val seedColor = rememberWallpaperOrSystemSeedColor(context)

    // 1:1 ES Dark custom Miuix color scheme
    val esDarkColors = remember {
        darkColorScheme(
            primary = Color(0xFF2F7DF6),
            onPrimary = Color.White,
            primaryVariant = Color(0xFF2EC7E0),
            background = Color(0xFF292C2E),
            onBackground = Color.White,
            surface = Color(0xFF292C2E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2D3032),
            surfaceContainer = Color(0xFF2D3032),
            onSurfaceContainer = Color.White,
            surfaceContainerHigh = Color(0xFF31353D),
            surfaceContainerHighest = Color(0xFF383C42),
            secondaryVariant = Color(0xFF383C42),
            onSecondaryVariant = Color.White,
            dividerLine = Color(0xFF383C42),
            outline = Color(0xFF454952)
        )
    }

    // Authentic MIUIX Dark custom surface hierarchy (so cards stand out against background)
    val miuixDarkColors = remember {
        darkColorScheme(
            primary = Color(0xFF277AF7),
            onPrimary = Color.White,
            background = Color(0xFF000000),
            onBackground = Color(0xFFF2F2F2),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFF2F2F2),
            surfaceVariant = Color(0xFF1C1C1E),
            surfaceContainer = Color(0xFF1C1C1E),
            onSurfaceContainer = Color(0xFFF2F2F2),
            surfaceContainerHigh = Color(0xFF242426),
            surfaceContainerHighest = Color(0xFF2C2C2E),
            secondaryVariant = Color(0xFF2C2C2E),
            onSecondaryVariant = Color(0xFFF2F2F2),
            dividerLine = Color(0xFF2C2C2E)
        )
    }

    // Authentic MIUIX Light custom surface hierarchy (soft gray background + crisp white cards)
    val miuixLightColors = remember {
        top.yukonga.miuix.kmp.theme.lightColorScheme(
            primary = Color(0xFF3482FF),
            onPrimary = Color.White,
            background = Color(0xFFF2F3F7),
            onBackground = Color(0xFF141414),
            surface = Color(0xFFF2F3F7),
            onSurface = Color(0xFF141414),
            surfaceVariant = Color.White,
            surfaceContainer = Color.White,
            onSurfaceContainer = Color(0xFF141414),
            surfaceContainerHigh = Color(0xFFE8EAF0),
            surfaceContainerHighest = Color(0xFFDCE0E8),
            secondaryVariant = Color(0xFFE8EAF0),
            onSecondaryVariant = Color(0xFF1F1F1F),
            dividerLine = Color(0xFFE0E2E8)
        )
    }

    val controller = remember(themeMode, sysDark, seedColor) {
        when (themeMode) {
            0 -> ThemeController(
                colorSchemeMode = ColorSchemeMode.Dark,
                darkColors = esDarkColors
            )
            1 -> ThemeController(
                colorSchemeMode = ColorSchemeMode.Dark,
                darkColors = miuixDarkColors
            )
            2 -> ThemeController(
                colorSchemeMode = ColorSchemeMode.Light,
                lightColors = miuixLightColors
            )
            3 -> ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetDark,
                keyColor = seedColor,
                paletteStyle = ThemePaletteStyle.Vibrant
            )
            4 -> ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetLight,
                keyColor = seedColor,
                paletteStyle = ThemePaletteStyle.Vibrant
            )
            else -> ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                lightColors = miuixLightColors,
                darkColors = miuixDarkColors,
                isDark = sysDark
            )
        }
    }

    CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
        MiuixTheme(
            controller = controller,
            content = content
        )
    }
}

