package top.yukonga.miuix.files.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val thumbCache = LruCache<String, Bitmap>(120)
private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")

fun isMediaFile(f: File) = f.extension.lowercase().let { it in imageExt || it in videoExt }

/** Carrega miniatura real de imagem/vídeo (como no ES); senão mostra o ícone de fallback. */
@Composable
fun FileThumbnail(file: File, fallbackRes: Int, modifier: Modifier = Modifier) {
    val key = file.absolutePath
    var bmp by remember(key) { mutableStateOf(thumbCache.get(key)) }
    LaunchedEffect(key) {
        if (bmp != null || !isMediaFile(file)) return@LaunchedEffect
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val ext = file.extension.lowercase()
                val b = if (ext in videoExt) {
                    if (android.os.Build.VERSION.SDK_INT >= 29) ThumbnailUtils.createVideoThumbnail(file, Size(256, 256), null)
                    else @Suppress("DEPRECATION") ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Images.Thumbnails.MINI_KIND)
                } else {
                    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, o)
                    var s = 1
                    while (o.outWidth / s > 320 || o.outHeight / s > 320) s *= 2
                    BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = s })
                }
                b?.also { thumbCache.put(key, it) }
            }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else Image(painterResource(fallbackRes), null, modifier)
}
