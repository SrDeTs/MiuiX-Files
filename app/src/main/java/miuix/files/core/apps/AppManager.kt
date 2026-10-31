package miuix.files.core.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val icon: Drawable?,
    val iconBitmap: ImageBitmap? = null,
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean
)

object AppManager {

    private var cachedApps: List<InstalledAppItem>? = null

    fun getCachedApps(): List<InstalledAppItem>? = cachedApps

    private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        return runCatching {
            val w = drawable.intrinsicWidth.coerceIn(1, 96)
            val h = drawable.intrinsicHeight.coerceIn(1, 96)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }.getOrNull()
    }

    suspend fun getInstalledApps(context: Context, forceRefresh: Boolean = false): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cachedApps?.let { return@withContext it }
        }
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val list = ArrayList<InstalledAppItem>(packages.size)

        for (pInfo in packages) {
            val app = pInfo.applicationInfo ?: continue
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val sourceFile = File(app.sourceDir)
            val name = pm.getApplicationLabel(app).toString()
            val version = pInfo.versionName ?: "1.0"
            val icon = runCatching { pm.getApplicationIcon(app) }.getOrNull()

            list.add(
                InstalledAppItem(
                    appName = name,
                    packageName = app.packageName,
                    versionName = version,
                    icon = icon,
                    iconBitmap = drawableToImageBitmap(icon),
                    sourceDir = app.sourceDir,
                    apkSize = sourceFile.length(),
                    isSystemApp = isSystem
                )
            )
        }

        // User apps first, sorted by app name
        list.sortedWith(compareBy<InstalledAppItem> { it.isSystemApp }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            .also { cachedApps = it }
    }

    suspend fun backupApp(context: Context, app: InstalledAppItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(miuix.files.ui.theme.MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            val customRelPath = prefs.getString(miuix.files.ui.theme.MiuiXFilesPreferences.KEY_BACKUP_DIR, "backups/apps") ?: "backups/apps"
            val backupDir = if (customRelPath.startsWith("/")) File(customRelPath) else File(Environment.getExternalStorageDirectory(), customRelPath)
            if (!backupDir.exists()) backupDir.mkdirs()

            val sanitizedName = app.appName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val targetFile = File(backupDir, "${sanitizedName}_${app.versionName}.apk")

            FileInputStream(File(app.sourceDir)).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
