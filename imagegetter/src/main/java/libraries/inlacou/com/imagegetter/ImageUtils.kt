package libraries.inlacou.com.imagegetter

import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.util.Base64
import android.view.View
import android.widget.ImageView
import timber.log.Timber
import java.io.*

import java.util.ArrayList

object ImageUtils {
	var COMPRESS_FORMAT: Bitmap.CompressFormat = PNG

	val uniqueImageFilename: String
		get() = "img_" + System.currentTimeMillis() + "." + when(COMPRESS_FORMAT){
			JPEG -> "jpeg"
			PNG -> "png"
			WEBP -> "webp"
		}

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
		bitmap.compress(PNG, 100, stream)
		return stream.toByteArray()
	}

	fun generateURI(context: Context, fname: String = ImageUtils.uniqueImageFilename): Uri {
		Timber.d("generateUri")
		val root = File(getRootUri(context))
		Timber.d("generateUri | root: $root")
		Timber.d("generateUri | root.mkdirs: ${root.mkdirs()}")
		Timber.d("generateUri | fname: $fname")
		val sdImageMainDirectory = File(root, fname)
		Timber.d("generateUri | sdImageMainDirectory: $sdImageMainDirectory")
		Timber.d("generateUri | resutl: ${FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider",sdImageMainDirectory)}")
		return FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider",sdImageMainDirectory)
	}

	fun getRootUri(context: Context) = "${Environment.getExternalStorageDirectory()}${File.separator}${context.getString(R.string.app_name)}${File.separator}"

	fun deleteFile(uri: Uri): Boolean {
		Timber.d("try to deleteFile $uri")
		uri.path.let {
			return if(it!=null) {
				ImageUtils.deleteFile(it)
			}else false
		}
	}

	fun deleteFile(path: String): Boolean {
		Timber.d("try to delete $path")
		return File(path).delete()
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
			/* WIP for Video
			val captureIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
			//time limit for video
			captureIntent.putExtra("android.intent.extra.durationLimit", 15)*/
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
		imageView.background = BitmapDrawable(context.resources, bitmap)
	}

	private fun setImageScale(context: Context, bitmap: Bitmap, imageView: ImageView, view: View) {
		imageView.background = BitmapDrawable(context.resources, ImageUtils.scaleBitmap(bitmap, view.width, view.height))
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
		bitmap.compress(PNG, 0, byteArrayOutputStream)
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
		bitmap.compress(JPEG, 80, byteArrayOutputStream)
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
		bitmap.compress(JPEG, 80, byteArrayOutputStream)
		return byteArrayOutputStream.toByteArray()
	}

	fun setImageFromMemory(activity: Activity, filename: String, maxImageSize: Int, imageView: ImageView, adjustViewBounds: Boolean, setScaleType: Boolean) {
		var selectedImage: Bitmap? = null
		try {
			selectedImage = ImageGetter.getBitmapFromPath(activity, filename, maxImageSize)
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
	
	/**
	 * This method will resize @param bitmap to file size @param imageFileSize and image dimensions @param imageSize and write it on @param absolutePath path as a JPEG file
	 */
	fun fullResizeImage(absolutePath: String, imageFileSize: Int, imageSize: Int, bitmap: Bitmap) {
		//Get byte stream to work with
		val stream = ByteArrayOutputStream()
		//Get file stream to write file
		val auxFileStream = FileOutputStream(absolutePath)
		//Scale and resize step
		var quality = 100
		do {
			stream.reset()
			//Maybe do not repeat this scaleKeepAspectRatio
			if(imageSize>0){
				bitmap.scaleKeepAspectRatio(imageSize)
			}else{
				bitmap
			}.compress(JPEG, quality, stream)
			quality -= 5
			Timber.d("current file size (STEP 1): ${stream.toByteArray().size/1024}vs$imageFileSize (quality ${quality+5})")
		}while(imageFileSize>0 && stream.toByteArray().size/1024>imageFileSize && quality>0)
		//Write to disk
		stream.writeTo(auxFileStream)
		
		//Cleanup
		stream.close()
		auxFileStream.close()
	}
	
	/**
	 * This method will resize @param bitmap to file size @param quality and image dimensions @param imageSize and write it on @param absolutePath path as a JPEG file
	 */
	fun fullResizeImage(absolutePath: String, imageSize: Int, bitmap: Bitmap, quality: Int) {
		//Get byte stream to work with
		val stream = ByteArrayOutputStream()
		//Get file stream to write file
		val auxFileStream = FileOutputStream(absolutePath)
		//Scale and resize step
		stream.reset()
		//Maybe do not repeat this scaleKeepAspectRatio
		if(imageSize>0){
			bitmap.scaleKeepAspectRatio(imageSize)
		}else{
			bitmap
		}.compress(JPEG, quality, stream)
		Timber.d("current file size (STEP 1): ${stream.toByteArray().size/1000} (quality $quality)")
		//Write to disk
		stream.writeTo(auxFileStream)
		
		//Cleanup
		stream.close()
		auxFileStream.close()
	}
}