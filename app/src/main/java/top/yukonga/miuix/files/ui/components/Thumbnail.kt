package top.yukonga.miuix.files.ui.components

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

fun isMediaFile(f: File) = f.extension.lowercase().let { it in imageExt || it in videoExt || it == "apk" }

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
    val ext = file.extension.lowercase()
    val key = if (ext == "apk") "apk_icon:${file.path}" else file.path
    var bmp by remember(key) { mutableStateOf(thumbCache.get(key)) }
    LaunchedEffect(key) {
        if (bmp != null || !isMediaFile(file)) return@LaunchedEffect
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
