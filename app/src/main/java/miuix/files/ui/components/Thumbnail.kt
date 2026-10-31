package miuix.files.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val thumbDispatcher = Dispatchers.IO.limitedParallelism(4)
private val thumbCache = LruCache<String, ImageBitmap>(350)
private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")

fun clearThumbnailCache(): Int {
    val count = thumbCache.size()
    thumbCache.evictAll()
    return count
}

private val knownAppFolders = mapOf(
    "mt2" to "bin.mt.plus",
    "whatsapp" to "com.whatsapp",
    "telegram" to "org.telegram.messenger",
    "nekogram" to "tw.nekomimi.nekogram",
    "shizuku" to "moe.shizuku.privileged.api",
    "termux" to "com.termux",
    "swiftbackup" to "org.swiftapps.swiftbackup"
)

fun isMediaFile(f: File) = f.extension.lowercase().let { it in imageExt || it in videoExt || it == "apk" }

fun loadFolderAppBitmap(context: Context, folder: File): ImageBitmap? {
    val prefs = context.getSharedPreferences(miuix.files.ui.theme.MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(miuix.files.ui.theme.MiuiXFilesPreferences.KEY_SHOW_FOLDER_APP_ICONS, true)) return null
    val nameLower = folder.name.lowercase()
    val key = "folder_app:$nameLower"
    thumbCache.get(key)?.let { return it }

    // 1. Check built-in map or SQLite /sdcard/.estrongs/folder_app.db
    var pkgName: String? = knownAppFolders[nameLower]
    if (pkgName == null) {
        val dbFile = File("/sdcard/.estrongs/folder_app.db")
        if (dbFile.exists() && dbFile.canRead()) {
            runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    db.rawQuery(
                        "SELECT package_name FROM app_folder_map WHERE lower(folder) = ? ORDER BY select_rate DESC LIMIT 1",
                        arrayOf("/$nameLower")
                    ).use { c ->
                        if (c.moveToFirst()) pkgName = c.getString(0)
                    }
                }
            }
        }
    }
    val pkg = pkgName ?: return null

    // 2. Check /sdcard/.estrongs/.app_icon_back/<pkg>.png first (if app is installed or backed up)
    val pm = context.packageManager
    val isInstalled = runCatching {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, 0) != null
    }.getOrDefault(false)

    val cachedIconFile = File("/sdcard/.estrongs/.app_icon_back/$pkg.png")
    if (cachedIconFile.exists() && cachedIconFile.canRead()) {
        runCatching {
            BitmapFactory.decodeFile(cachedIconFile.absolutePath)?.asImageBitmap()?.also {
                thumbCache.put(key, it)
                return it
            }
        }
    }

    if (!isInstalled) return null
    return runCatching {
        val drawable = pm.getApplicationIcon(pkg)
        val w = drawable.intrinsicWidth.coerceIn(96, 192)
        val h = drawable.intrinsicHeight.coerceIn(96, 192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        bmp.asImageBitmap().also { thumbCache.put(key, it) }
    }.getOrNull()
}

fun loadApkIconBitmap(context: Context, apkFile: File): ImageBitmap? {
    val key = "apk_icon:${apkFile.path}"
    thumbCache.get(key)?.let { return it }
    return runCatching {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0) ?: return null
        val appInfo = pkgInfo.applicationInfo ?: return null
        appInfo.sourceDir = apkFile.absolutePath
        appInfo.publicSourceDir = apkFile.absolutePath
        val drawable = appInfo.loadIcon(pm) ?: return null
        val w = drawable.intrinsicWidth.coerceIn(96, 192)
        val h = drawable.intrinsicHeight.coerceIn(96, 192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        bmp.asImageBitmap().also { thumbCache.put(key, it) }
    }.getOrNull()
}

/** Carrega miniatura real de imagem/vídeo ou ícone real do APK (como no ES); senão mostra o ícone de fallback. */
@Composable
fun FileThumbnail(file: File, fallbackRes: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(miuix.files.ui.theme.MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }
    val showThumbnails = prefs.getBoolean(miuix.files.ui.theme.MiuiXFilesPreferences.KEY_SHOW_THUMBNAILS, true)
    val ext = file.extension.lowercase()
    val key = if (ext == "apk") "apk_icon:${file.path}" else file.path
    var bmp by remember(key, showThumbnails) { mutableStateOf(if (showThumbnails) thumbCache.get(key) else null) }
    LaunchedEffect(key, showThumbnails) {
        if (!showThumbnails || bmp != null || !isMediaFile(file)) return@LaunchedEffect
        bmp = withContext(thumbDispatcher) {
            thumbCache.get(key) ?: runCatching {
                if (ext == "apk") {
                    loadApkIconBitmap(context, file)
                } else {
                    val raw: Bitmap? = if (ext in videoExt) {
                        if (Build.VERSION.SDK_INT >= 29) ThumbnailUtils.createVideoThumbnail(file, Size(144, 144), null)
                        else @Suppress("DEPRECATION") ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Images.Thumbnails.MINI_KIND)
                    } else {
                        if (Build.VERSION.SDK_INT >= 29) {
                            ThumbnailUtils.createImageThumbnail(file, Size(144, 144), null)
                        } else {
                            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(file.path, o)
                            var s = 1
                            while (o.outWidth / s > 160 || o.outHeight / s > 160) s *= 2
                            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                                inSampleSize = s
                                inPreferredConfig = Bitmap.Config.RGB_565
                            })
                        }
                    }
                    raw?.asImageBitmap()?.also { thumbCache.put(key, it) }
                }
            }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) {
        Image(
            b,
            null,
            modifier,
            contentScale = if (ext == "apk") ContentScale.Fit else ContentScale.Crop
        )
    } else {
        Image(painterResource(fallbackRes), null, modifier)
    }
}
