package vistark.vn.rudant

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

class ShareUtils {
    fun share(context: Context) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND;
        intent.type = "text/plain";
        intent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(Intent.createChooser(intent, "Share"));
    }

    fun share(context: Context, path: String, highScore: Int, usedTime: String) {
        val file = File(path)
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "I got $highScore score in ${usedTime}s. Do you want to play ${context.resources.getString(
                R.string.app_name
            )} and defeat me? Download here: ${PlayStoreUtils().getAppAddress(
                context
            )}"
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
