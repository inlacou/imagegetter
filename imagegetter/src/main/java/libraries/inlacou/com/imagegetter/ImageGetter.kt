package libraries.inlacou.com.imagegetter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import timber.log.Timber

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by inlacou on 26/04/16.
 */
class ImageGetter(private val activity: Activity,
				  private val log: Boolean = false,
				  private val crop: Boolean = true,
				  private val circular: Boolean = true,
				  private val fixed: Boolean = true,
				  private val useCamera: Boolean = true,
				  private val useGallery: Boolean = true,
				  private val format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
				  private val width: Int = 1,
				  private val height: Int = 1,
				  private val request_code_select_picture: Int,
				  private val request_code_crop: Int,
				  private val callbacks: Callbacks) {

	var uri: Uri? = null
	private var tag: String? = null

	private fun log(s: String){
		if(log) Timber.d(s)
	}

	private fun log(tag: String, s: String){
		if(log) Timber.d("$tag | $s")
	}

	@JvmOverloads
	fun start(tag: String, destroyPrevious: Boolean = false) { //It's false until below TODO is addressed
		log("start with tag: $tag")
		if(destroyPrevious) destroy()	//TODO Hmmm I destroy here, but if I dont finish the process... it's still deleted (confirmed)
		this.uri = ImageUtils.generateURI(activity)
		log("start with uri: ${uri.toString()}")
		this.tag = tag
		ImageUtils.COMPRESS_FORMAT = format
		checkExternalStoragePermission()
	}

	fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		log("onRequestPermissionsResult")
		for (i in permissions.indices) {
			if (permissions[i].equals(PermissionUtils.Permission.externalStorage.permission, ignoreCase = true)) {
				if(useCamera) {
					checkCameraPermission(uri)
				}else{
					ImageUtils.openImageIntent(activity, useCamera, useGallery, request_code_select_picture, uri)
				}
			} else if (permissions[i].equals(PermissionUtils.Permission.camera.permission, ignoreCase = true)) {
				ImageUtils.openImageIntent(activity, useCamera, useGallery, request_code_select_picture, uri)
			}
		}
	}

	private fun checkExternalStoragePermission() {
		val aux = uri
		log("checkExternalStoragePermission with uri: ${uri.toString()}")
		PermissionUtils.checkGetIfNotPermission(activity, PermissionUtils.Permission.externalStorage) {
			if (useCamera) {
				checkCameraPermission(aux)
			} else {
				ImageUtils.openImageIntent(activity, false, useGallery, request_code_select_picture, aux)
			}
		}
	}

	private fun checkCameraPermission(aux: Uri?) {
		log("checkCameraPermission with uri: ${aux.toString()}")
		PermissionUtils.checkGetIfNotPermission(activity,
				PermissionUtils.Permission.camera
		) { ImageUtils.openImageIntent(activity, useCamera, useGallery, request_code_select_picture, aux) }
	}

	fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		log("onActivityResult")
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == request_code_select_picture) {
				var isCamera: Boolean = if (data == null) {
					true
				} else {
					MediaStore.ACTION_IMAGE_CAPTURE == data.action
				}

				data?.data.let { data -> //This is an additional check, because some cameras do not say they are a camera at least in the previous way
					if(data!=null && data.lastPathSegment==uri?.lastPathSegment){
						isCamera = true
					}
				}

				val selectedImageUri: Uri? =  if (isCamera) {
					uri
				} else {
					data?.data ?: uri
					//Example content://com.android.providers.media.documents/document/image%3A51353
					//It's generated by the system, not me
				}
				uri = selectedImageUri

				/*Bitmap bitmap = ((BitmapDrawable) yourDrawable).getBitmap();

				Rotate the bitmap
				bitmap = ImageUtils.rotateBitmap(ImageUtils.getCameraPhotoOrientation(this, selectedImageUri), bitmap);

				selectedImage = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, 250, 250);*/

				if (!crop) {
					callbacks.setImage(selectedImageUri.toString(), tag)
				} else {
					launchCropActivity(selectedImageUri)
				}
			} else if (requestCode == request_code_crop) {
				val filename = data!!.getStringExtra(CropActivity.RESPONSE_EXTRA_BITMAP)
				callbacks.setImage(filename, tag)
				uri = Uri.parse(filename)
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			uri = null
			return
		}
	}

	private fun launchCropActivity(selectedImageUri: Uri?) {
		log("launchCropActivity")
		val intent = Intent(activity, CropActivity::class.java)
		selectedImageUri?.let { intent.putExtra(CropActivity.INTENT_EXTRA_URI, selectedImageUri) }
		intent.putExtra(CropActivity.INTENT_EXTRA_LOG, log)
		intent.putExtra(CropActivity.INTENT_EXTRA_CIRCULAR, circular)
		intent.putExtra(CropActivity.INTENT_EXTRA_WIDTH, width)
		intent.putExtra(CropActivity.INTENT_EXTRA_HEIGHT, height)
		intent.putExtra(CropActivity.INTENT_EXTRA_FIXED, fixed)
		activity.startActivityForResult(intent, request_code_crop)
	}

	fun onSaveInstanceState(outState: Bundle) {
		log("onSaveInstanceState")
		uri?.let { outState.putString("uri", it.toString()) }
		outState.putBoolean("crop", crop)
		outState.putBoolean("circular", circular)
		outState.putBoolean("fixed", fixed)
		outState.putInt("width", width)
		outState.putInt("height", height)
		outState.putInt("request_code_select_picture", request_code_select_picture)
		outState.putInt("request_code_crop", request_code_crop)
		outState.putBoolean("use_camera", useCamera)
	}

	fun destroy() {
		log("destroy", "deleteing uri?.path... ${uri?.path}")
		try {
			uri?.path?.let {
				destroy(it)
				log("destroy", "deleted path: $it")
			}
		} catch (npe: NullPointerException) {
			log("destroy", "nothing!")
		}

	}

	fun destroy(path: String) {
		log("destroy", "deleteing $path... ")
		try {
			File(path).delete()
			log("destroy", "deleted path: $path")
		} catch (npe: NullPointerException) {
			log("destroy", "nothing!")
		}
	}

	interface Callbacks {
		fun setImage(path: String, tag: String?)
	}

	companion object {
		@Throws(IOException::class)
		fun getBitmapFromPath(context: Context, filename: String, size: Int): Bitmap {
			return try {
				val inputStream = FileInputStream(filename)
				var bitmap = BitmapFactory.decodeStream(inputStream)
				bitmap = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, size)
				inputStream.close()
				bitmap
			} catch (fnfe: FileNotFoundException) {
				ImageUtils.scaleBitmapKeepAspectRatio(MediaStore.Images.Media.getBitmap(
						context.contentResolver, Uri.parse(filename)), size)
			}

		}

		@Throws(IOException::class)
		fun getBitmapFromPath(filename: String): Bitmap {
			val inputStream = FileInputStream(filename)
			val bitmap = BitmapFactory.decodeStream(inputStream)
			inputStream.close()
			return bitmap
		}

		fun onRestoreInstanceState(savedInstanceState: Bundle, activity: Activity, callbacks: Callbacks): ImageGetter? {
			return if (savedInstanceState.containsKey("crop") && savedInstanceState.containsKey("circular") && savedInstanceState.containsKey("uri")) {
				val imageGetter = ImageGetter(activity,
						crop = savedInstanceState.getBoolean("crop"),
						circular = savedInstanceState.getBoolean("circular"),
						fixed = savedInstanceState.getBoolean("fixed"),
						useCamera = savedInstanceState.getBoolean("use_camera"),
						width = savedInstanceState.getInt("width"),
						height = savedInstanceState.getInt("height"),
						request_code_select_picture = savedInstanceState.getInt("request_code_select_picture"),
						request_code_crop = savedInstanceState.getInt("request_code_crop"),
						callbacks = callbacks)
				imageGetter.uri = Uri.parse(savedInstanceState.getString("uri"))
				imageGetter
			}else{
				Timber.d("onRestoreInstanceState | nothing found on savedInstanceState")
				null
			}
		}
	}
}
