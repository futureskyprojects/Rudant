package vistark.vn.rudant

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream


class Screenshot {
    fun getScreenshotBitmap(path: String): Bitmap {
        return BitmapFactory.decodeFile(path)
    }

    fun take(act: AppCompatActivity): String {
        val millis: Long = System.currentTimeMillis()
        val mPath = "${act.externalCacheDir}/$millis.jpeg"
        val bitmap: Bitmap

        val v1 = act.window.decorView.rootView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap = Bitmap.createBitmap(v1.width, v1.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            v1.getLocationInWindow(locationOfViewInWindow)
            PixelCopy.request(
                act.window,
                Rect(
                    locationOfViewInWindow[0],
                    locationOfViewInWindow[1],
                    locationOfViewInWindow[0] + v1.width,
                    locationOfViewInWindow[1] + v1.height
                ), bitmap, { _ ->
                    //                    if (copyResult == PixelCopy.SUCCESS) {
//                    }
                    // possible to handle other result codes ...
                },
                Handler()
            )
        } else {
            v1.isDrawingCacheEnabled = true
            bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
        }
        val imageFile = File(mPath)

        val outputStream = FileOutputStream(imageFile)
        val quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.flush()
        outputStream.close()

        return imageFile.path
    }
}