package libraries.inlacou.com.imagegetter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import timber.log.Timber

/**
 * Scale mode: Example 512 scaleSize:
 * if image is square, it will be 512*512
 * if image is wider than taller, it will be 512*(less than 512)
 * if image is wider than taller, it will be (less than 512)*512
 */
fun Bitmap.scaleKeepAspectRatio(scaleSize: Int): Bitmap {
    Timber.d("scaleKeepAspectRatio | original width $width and height $height")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        Timber.d("scaleKeepAspectRatio | original allocationByteCount ${allocationByteCount.toDouble()/1000000}mb")
    }
    Timber.d("scaleKeepAspectRatio | scaleSize $scaleSize")
    if(width==scaleSize && height==scaleSize) return this
    var newWidth = -1
    var newHeight = -1
    when {
        height > width -> {
            newHeight = scaleSize
            val multFactor = width.toFloat() / height.toFloat()
            newWidth = (newHeight * multFactor).toInt()
        }
        width > height -> {
            newWidth = scaleSize
            val multFactor = height.toFloat() / width.toFloat()
            newHeight = (newWidth * multFactor).toInt()
        }
        height == width -> {
            newHeight = scaleSize
            newWidth = scaleSize
        }
    }
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, false).apply {
        Timber.d("scaleKeepAspectRatio | resulting width $width and height $height")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Timber.d("scaleKeepAspectRatio | resulting allocationByteCount ${allocationByteCount.toDouble()/1000000}mb")
        }
    }
}

/**
 * Scale to exactly width*height
 */
fun Bitmap.scaleKeepAspectRatio(width: Int, height: Int): Bitmap {
    Timber.d("scaleKeepAspectRatio | original width $width and height $height")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        Timber.d("scaleKeepAspectRatio | original allocationByteCount ${allocationByteCount.toDouble()/1024}mb")
    }
    Timber.d("scaleKeepAspectRatio | scaleSize $width*$height")
    if(width==width && height==height) return this
    val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val originalWidth = width.toFloat()
    val originalHeight = height.toFloat()
    val canvas = Canvas(background)
    val scale = width / originalWidth
    val xTranslation = 0.0f
    val yTranslation = (height - originalHeight * scale) / 2.0f
    val transformation = Matrix()
    transformation.postTranslate(xTranslation, yTranslation)
    transformation.preScale(scale, scale)
    val paint = Paint()
    paint.isFilterBitmap = true
    canvas.drawBitmap(this, transformation, paint)
    return background.also {
        Timber.d("resulting width $width and height $height")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Timber.d("scaleKeepAspectRatio | resulting allocationByteCount ${allocationByteCount.toDouble()/1024}mb")
        }
    }
}