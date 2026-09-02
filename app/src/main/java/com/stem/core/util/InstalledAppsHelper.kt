package com.stem.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build



data class LaunchableAppInfo(
    val packageName: String,
    val label: String
)

/**
 * Enumerates launchable apps for the per-app exclusion list in Settings. Relies on the
 * `<queries>` block in AndroidManifest.xml — required on Android 11+ package-visibility rules
 * for [android.content.pm.PackageManager.queryIntentActivities] to see apps outside Stem's own
 * package.
 */
object InstalledAppsHelper {

    fun getLaunchableApps(context: Context): List<LaunchableAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager).toString()
                LaunchableAppInfo(packageName = packageName, label = label)
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
