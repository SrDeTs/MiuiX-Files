package top.yukonga.miuix.files.core.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
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
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean
)

object AppManager {

    suspend fun getInstalledApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val list = mutableListOf<InstalledAppItem>()

        for (app in packages) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val sourceFile = File(app.sourceDir)
            val name = pm.getApplicationLabel(app).toString()
            val pInfo = try {
                pm.getPackageInfo(app.packageName, 0)
            } catch (_: Exception) {
                null
            }
            val version = pInfo?.versionName ?: "1.0"
            val icon = try {
                pm.getApplicationIcon(app)
            } catch (_: Exception) {
                null
            }

            list.add(
                InstalledAppItem(
                    appName = name,
                    packageName = app.packageName,
                    versionName = version,
                    icon = icon,
                    sourceDir = app.sourceDir,
                    apkSize = sourceFile.length(),
                    isSystemApp = isSystem
                )
            )
        }

        // User apps first, sorted by app name
        list.sortedWith(compareBy<InstalledAppItem> { it.isSystemApp }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    suspend fun backupApp(context: Context, app: InstalledAppItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(Environment.getExternalStorageDirectory(), "AppBackup")
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
