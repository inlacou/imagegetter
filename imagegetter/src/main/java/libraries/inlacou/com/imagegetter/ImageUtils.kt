package libraries.inlacou.com.imagegetter

import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.view.View
import android.widget.ImageView

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayList

object ImageUtils {
	private const val FORMAT = "jpg"
	val COMPRESS_FORMAT: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG

	private val uniqueImageFilename: String
		get() = "img_" + System.currentTimeMillis() + "." + ImageUtils.FORMAT

	fun fromUri(contentResolver: ContentResolver, uri: Uri): Drawable? {
		return try {
			val inputStream = contentResolver.openInputStream(uri)
			Drawable.createFromStream(inputStream, uri.toString())
		} catch (e: FileNotFoundException) {
			e.printStackTrace()
			null
		} catch (npe: NullPointerException) {
			npe.printStackTrace()
			null
		}

	}

	fun base64StringToByteArray(s: String): ByteArray {
		return Base64.decode(s, Base64.DEFAULT)
	}

	fun byteArrayToBase64String(bytes: ByteArray): String {
		return Base64.encodeToString(bytes, Base64.DEFAULT)
	}

	fun getDawableResource(c: Context, ImageName: String): Drawable {
		return c.resources.getDrawable(c.resources.getIdentifier(ImageName, "drawable", c.packageName))
	}

	fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
		val stream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
		return stream.toByteArray()
	}

	fun generateURI(context: Context): Uri {
		val root = File(Environment.getExternalStorageDirectory().toString() + File.separator + context.getString(R.string.app_name) + File.separator)
		root.mkdirs()
		val fname = ImageUtils.uniqueImageFilename
		val sdImageMainDirectory = File(root, fname)
		return Uri.fromFile(sdImageMainDirectory)
	}

	fun deleteFile(uri: Uri): Boolean {
		val fdelete = File(uri.path)
		return if (fdelete.exists()) {
			fdelete.delete()
		} else false
	}

	fun deleteFile(s: String): Boolean {
		return deleteFile(Uri.parse(s))
	}

	fun openImageIntent(activity: Activity, useCamera: Boolean, useGallery: Boolean, YOUR_SELECT_PICTURE_REQUEST_CODE: Int, outputImageUri: Uri?) {
		val packageManager = activity.packageManager
		var chooserIntent: Intent? = null

		var camera = false
		var gallery = false
		var galleryIntent: Intent? = null
		var cameraIntents: MutableList<Intent>? = null

		// Camera.
		if (useCamera && PermissionUtils.permissionAllowed(activity, PermissionUtils.Permission.camera)) {
			cameraIntents = ArrayList()
			val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			val listCam = packageManager.queryIntentActivities(captureIntent, 0)
			for (res in listCam) {
				val packageName = res.activityInfo.packageName
				val intent = Intent(captureIntent)
				intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
				intent.setPackage(packageName)
				intent.putExtra(MediaStore.EXTRA_OUTPUT, outputImageUri)
				cameraIntents.add(intent)
			}
			camera = true
		}

		// Filesystem.
		if (useGallery && PermissionUtils.permissionAllowed(activity, PermissionUtils.Permission.externalStorage)) {
			galleryIntent = Intent()
			galleryIntent.type = "image/*"
			galleryIntent.action = Intent.ACTION_GET_CONTENT
			gallery = true
		}

		if (camera && gallery) {
			chooserIntent = Intent.createChooser(galleryIntent, activity.getString(R.string.select_source))
			chooserIntent!!.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents!!.toTypedArray<Parcelable>())
		} else if (camera) {
			chooserIntent = Intent.createChooser(cameraIntents!!.removeAt(cameraIntents.size - 1), activity.getString(R.string.select_source))
			chooserIntent!!.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray<Parcelable>())
		} else if (gallery) {
			chooserIntent = Intent.createChooser(galleryIntent, activity.getString(R.string.select_source))
		}

		// Add the camera options.

		if (chooserIntent != null) activity.startActivityForResult(chooserIntent, YOUR_SELECT_PICTURE_REQUEST_CODE)
	}

	fun getCameraPhotoOrientation(context: Context, imageUri: Uri): Int {
		return try {
			context.contentResolver.notifyChange(imageUri, null)
			val imageFile = File(imageUri.path)
			val exif = ExifInterface(
					imageFile.absolutePath)
			val orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL)

			when (orientation) {
				ExifInterface.ORIENTATION_ROTATE_270 -> 270
				ExifInterface.ORIENTATION_ROTATE_180 -> 180
				ExifInterface.ORIENTATION_ROTATE_90 -> 90
				else -> 0
			}
		} catch (e: Exception) {
			e.printStackTrace()
			0
		}
	}

	fun getImage(c: Context, ImageName: String): Drawable {
		return c.resources.getDrawable(c.resources.getIdentifier(ImageName, "drawable", c.packageName))
	}

	fun rotateBitmap(cameraPhotoOrientation: Int, bitmap: Bitmap): Bitmap {
		val matrix = Matrix()

		matrix.postRotate(cameraPhotoOrientation.toFloat())

		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
	}

	private fun scaleBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
		return try {
			Bitmap.createScaledBitmap(bitmap, width, height, true)
		} catch (iae: IllegalArgumentException) {
			Bitmap.createScaledBitmap(bitmap, 50, 50, true)
		}
	}

	fun scaleBitmapKeepAspectRatio(bitmap: Bitmap, scaleSize: Int): Bitmap {
		val originalWidth = bitmap.width
		val originalHeight = bitmap.height
		var newWidth = -1
		var newHeight = -1
		when {
			originalHeight > originalWidth -> {
				newHeight = scaleSize
				val multFactor = originalWidth.toFloat() / originalHeight.toFloat()
				newWidth = (newHeight * multFactor).toInt()
			}
			originalWidth > originalHeight -> {
				newWidth = scaleSize
				val multFactor = originalHeight.toFloat() / originalWidth.toFloat()
				newHeight = (newWidth * multFactor).toInt()
			}
			originalHeight == originalWidth -> {
				newHeight = scaleSize
				newWidth = scaleSize
			}
		}
		return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
	}

	fun scaleBitmapKeepAspectRatio(originalImage: Bitmap, width: Int, height: Int): Bitmap {
		val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val originalWidth = originalImage.width.toFloat()
		val originalHeight = originalImage.height.toFloat()
		val canvas = Canvas(background)
		val scale = width / originalWidth
		val xTranslation = 0.0f
		val yTranslation = (height - originalHeight * scale) / 2.0f
		val transformation = Matrix()
		transformation.postTranslate(xTranslation, yTranslation)
		transformation.preScale(scale, scale)
		val paint = Paint()
		paint.isFilterBitmap = true
		canvas.drawBitmap(originalImage, transformation, paint)
		return background
	}

	fun getHeightScaleToWidth(originalWidth: Int, originalHeight: Int, width: Int): Int {
		var newWidth = -1
		var newHeight = -1
		var multFactor = -1.0f
		if (originalHeight == originalWidth) {
			newHeight = width
		} else {
			newWidth = width
			multFactor = originalHeight.toFloat() / originalWidth.toFloat()
			newHeight = (newWidth * multFactor).toInt()
		}
		return newHeight
	}

	fun base64ToDrawable(context: Context, base64: String): Drawable {
		val bytes = base64StringToByteArray(base64)

		return BitmapDrawable(context.resources, BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
	}

	fun base64toBitmap(context: Context, base64: String): Bitmap {
		val bytes = base64StringToByteArray(base64)

		return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
	}

	fun setImage(context: Context, bitmap: Bitmap, imageView: ImageView) {
		val d = BitmapDrawable(context.resources, bitmap)

		imageView.background = d
	}

	private fun setImageScale(context: Context, bitmap: Bitmap, imageView: ImageView, view: View) {
		var bitmap = bitmap
		bitmap = ImageUtils.scaleBitmap(bitmap, view.width, view.height)

		val d = BitmapDrawable(context.resources, bitmap)

		imageView.background = d
	}

	/*public static Bitmap cropBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final int margin = (int) ApplicationController.getInstance().getResources().getDimension(R.dimen.circled_letter_circle_stroke_size);
        final Rect rect = new Rect(margin, margin, bitmap.getWidth()-margin, bitmap.getHeight()-margin);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }*/


	/*public static Bitmap generateQRCode_general(String data, Activity activity) throws WriterException {
        com.google.zxing.Writer writer = new QRCodeWriter();
        //String finaldata = Uri.encode(data, "utf-8");
        String finaldata = data;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        //int height = size.y; //Not needed at all

        BitMatrix bm = writer.encode(finaldata, BarcodeFormat.QR_CODE, width, width);
        Bitmap ImageBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < width; i++) {//width
            for (int j = 0; j < width; j++) {//height
                ImageBitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK: Color.WHITE);
            }
        }

        if (ImageBitmap != null) {
            return ImageBitmap;
        } else {
            //Toast.makeText(activity.getApplicationContext(), "TO DO", Toast.LENGTH_SHORT).show();
            return null;
        }
    }*/

	fun pixelToDP(c: Context, pixels: Float): Int {
		val density = c.resources.displayMetrics.density
		return (pixels / density).toInt()
	}

	fun dpToPixel(c: Context, dp: Float): Int {
		val density = c.resources.displayMetrics.density
		return (dp * density).toInt()
	}

	/**
	 * Turn drawable resource into byte array.
	 *
	 * @param context parent context
	 * @param id      drawable resource id
	 * @return byte array
	 */
	fun getFileDataFromDrawable(context: Context, id: Int): ByteArray {
		val drawable = ContextCompat.getDrawable(context, id)
		val bitmap = (drawable as BitmapDrawable).bitmap
		val byteArrayOutputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
		return byteArrayOutputStream.toByteArray()
	}

	/**
	 * Turn drawable into byte array.
	 *
	 * @param drawable data
	 * @return byte array
	 */
	fun getFileDataFromDrawable(context: Context, drawable: Drawable): ByteArray {
		val bitmap = (drawable as BitmapDrawable).bitmap
		val byteArrayOutputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
		return byteArrayOutputStream.toByteArray()
	}

	/**
	 * Turn bitmap into byte array.
	 *
	 * @param bitmap data
	 * @return byte array
	 */
	fun getFileDataFromBitmap(context: Context, bitmap: Bitmap): ByteArray {
		val byteArrayOutputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
		return byteArrayOutputStream.toByteArray()
	}

	fun setImageFromMemory(activity: Activity, filename: String, maxSize: Int, imageView: ImageView, adjustViewBounds: Boolean, setScaleType: Boolean) {
		var selectedImage: Bitmap? = null
		try {
			selectedImage = ImageGetter.getBitmapFromPath(activity, filename, maxSize)
		} catch (e: IOException) {
			e.printStackTrace()
		}

		if (selectedImage == null) {
			return
		}
		if (adjustViewBounds) imageView.adjustViewBounds = true
		if (setScaleType) imageView.scaleType = ImageView.ScaleType.CENTER_CROP
		val drawable = BitmapDrawable(activity.resources, selectedImage)
		imageView.setImageDrawable(drawable)
	}
}