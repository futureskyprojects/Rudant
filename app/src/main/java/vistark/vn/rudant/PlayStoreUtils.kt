package vistark.vn.rudant

import android.content.Context
import android.content.Intent
import android.net.Uri

class PlayStoreUtils {
    fun openPlayStoreForApp(context: Context) {
        val appPackageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        }

    }

    fun getAppAddress(context: Context): String {
        return "https://play.google.com/store/apps/details?id=${context.packageName}"
    }
}