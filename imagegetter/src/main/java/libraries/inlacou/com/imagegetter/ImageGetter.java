package libraries.inlacou.com.imagegetter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

//TODO clean old images, here or don't know where. Maybe after sending them to server, don't know.

/**
 * Created by inlacou on 26/04/16.
 */
public class ImageGetter {
	private static final String DEBUG_TAG = ImageGetter.class.getName();
	private final Callbacks callbacks;
	private final boolean crop, circular, fixed;
	private int request_code_select_picture,request_code_crop;
	private final int width, height;
	private Context context;

	private Activity activity;
	private Uri uri;
	private String tag;
	private Source source;

	public ImageGetter(Context context, boolean crop, boolean circular, boolean fixed, int width, int height,
	                   int request_code_select_picture, int request_code_crop, Callbacks callbacks) {
		this.callbacks = callbacks;
		this.crop = crop;
		this.circular = circular;
		this.width = width;
		this.height = height;
		this.fixed = fixed;
		this.context = context;
		this.request_code_select_picture = request_code_select_picture;
		this.request_code_crop = request_code_crop;
		activity = callbacks.getActivity();
	}

	private ImageGetter(boolean crop, boolean circular, boolean fixed, int width, int height,
	                    int request_code_select_picture, int request_code_crop, Callbacks callbacks) {
		this.callbacks = callbacks;
		this.crop = crop;
		this.circular = circular;
		this.width = width;
		this.height = height;
		this.fixed = fixed;
		this.request_code_select_picture = request_code_select_picture;
		this.request_code_crop = request_code_crop;
		activity = callbacks.getActivity();
	}

	public void start(String tag){
		destroy();
		this.uri = ImageUtils.generateURI(context);
		this.tag = tag;
		checkExternalStoragePermission();
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		boolean cameraPermission, externalStoragePermission;
		for (int i=0; i<permissions.length; i++){
			Log.d(DEBUG_TAG, i + " onActivityResult(" + requestCode + ", " + permissions[i] + ", " + grantResults[i] + ")");
			if(permissions[i].equalsIgnoreCase(PermissionUtils.Permission.externalStorage.permission)){
				externalStoragePermission = true;
				checkCameraPermission(uri);
			}else if(permissions[i].equalsIgnoreCase(PermissionUtils.Permission.camera.permission)){
				cameraPermission = true;
				ImageUtils.openImageIntent(activity, request_code_select_picture, uri);
			}
		}
	}

	private void checkExternalStoragePermission() {
		final Uri aux = this.uri;
		PermissionUtils.checkGetIfNotPermission(context, new PermissionUtils.Callbacks() {
			@Override
			public void onPermissionGranted() {
				///ImageUtils.openImageIntent(activity, REQUEST_CODE_SELECT_PICTURE, aux);
				checkCameraPermission(aux);
			}

			@Override
			public Activity getActivity() {
				return activity;
			}
		}, PermissionUtils.Permission.externalStorage);
	}

	private void checkCameraPermission(final Uri aux){
		PermissionUtils.checkGetIfNotPermission(context, new PermissionUtils.Callbacks() {
			@Override
			public void onPermissionGranted() {
				ImageUtils.openImageIntent(activity, request_code_select_picture, aux);
			}

			@Override
			public Activity getActivity() {
				return activity;
			}
		}, PermissionUtils.Permission.camera);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == request_code_select_picture) {
				final boolean isCamera;
				if (data == null) {
					isCamera = true;
				} else {
					isCamera = MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
				}
				if(isCamera){
					source = Source.CAMERA;
				}else{
					source = Source.GALLERY;
				}
				Log.d(DEBUG_TAG+".onActivityResult", "isCamera: " + isCamera);

				Uri selectedImageUri;
				if (isCamera) {
					selectedImageUri = uri;
				} else {
					selectedImageUri = data == null ? null : data.getData();
					//Example content://com.android.providers.media.documents/document/image%3A51353
					//It's generated by the system, not me
					//So old uri is useless
					//Maybe this works:
					uri = selectedImageUri;
					//I cant make it break when getting from gallery
				}
				Log.d(DEBUG_TAG+".onActivityResult", "selectedImageUri: " + selectedImageUri);

				/*Bitmap bitmap = ((BitmapDrawable) yourDrawable).getBitmap();

				Rotate the bitmap
				bitmap = ImageUtils.rotateBitmap(ImageUtils.getCameraPhotoOrientation(this, selectedImageUri), bitmap);

				selectedImage = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, 250, 250);*/

				if(!crop){
					callbacks.setImage(selectedImageUri.toString(), tag);
				}else {
					Log.d(DEBUG_TAG+".onActivityResult", "launching CircularCropActivity intent");
					launchCropActivity(selectedImageUri);
				}
			}else if (requestCode == request_code_crop) {
				String filename = data.getStringExtra(CropActivity.RESPONSE_EXTRA_BITMAP);
				Log.d(DEBUG_TAG+".onActivityResult", "3 - filename: " + filename);
				callbacks.setImage(filename, tag);
				if(callbacks.shouldDestroyFile(source)){
					destroy();
				}
				uri = Uri.parse(filename);
			}
		}else if(resultCode==Activity.RESULT_CANCELED){
			uri = null;
			return;
		}
	}

	public static Bitmap getBitmapFromPath(Context context, String filename, int size) throws IOException {
		Log.d(DEBUG_TAG+".getBitmapFromPath", "filename: " + filename);
		Log.d(DEBUG_TAG+".getBitmapFromPath", "size: " + size);
		try {
			FileInputStream is = new FileInputStream(filename);
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			bitmap = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, size);
			is.close();
			return bitmap;
		}catch (FileNotFoundException fnfe){
			Log.d(DEBUG_TAG+".getBitmapFromPath", "FileNotFoundException! Trying other approach...");
			return ImageUtils.scaleBitmapKeepAspectRatio(MediaStore.Images.Media.getBitmap(
					context.getContentResolver(), Uri.parse(filename)), size);
		}
	}

	public static Bitmap getBitmapFromPath(String filename) throws IOException {
		FileInputStream is = new FileInputStream(filename);
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		is.close();
		return bitmap;
	}

	private void launchCropActivity(Uri selectedImageUri) {
		Intent intent = new Intent(activity, CropActivity.class);
		intent.putExtra(CropActivity.INTENT_EXTRA_URI, selectedImageUri);
		intent.putExtra(CropActivity.INTENT_EXTRA_CIRCULAR, circular);
		intent.putExtra(CropActivity.INTENT_EXTRA_WIDTH, width);
		intent.putExtra(CropActivity.INTENT_EXTRA_HEIGHT, height);
		intent.putExtra(CropActivity.INTENT_EXTRA_FIXED, fixed);
		activity.startActivityForResult(intent, request_code_crop);
	}

	public void onSaveInstanceState(Bundle outState) {
		if(uri !=null) outState.putString("uri", uri.toString());
		outState.putBoolean("crop", crop);
		outState.putBoolean("circular", circular);
		outState.putBoolean("fixed", fixed);
		outState.putInt("width", width);
		outState.putInt("height", height);
		outState.putInt("request_code_select_picture", request_code_select_picture);
		outState.putInt("request_code_crop", request_code_crop);
	}

	public static ImageGetter onRestoreInstanceState(Bundle savedInstanceState, Callbacks callbacks) {
		if(savedInstanceState.containsKey("crop") && savedInstanceState.containsKey("circular") && savedInstanceState.containsKey("uri")) {
			ImageGetter imageGetter = new ImageGetter(savedInstanceState.getBoolean("crop"),
					savedInstanceState.getBoolean("circular"),
					savedInstanceState.getBoolean("fixed"),
					savedInstanceState.getInt("width"),
					savedInstanceState.getInt("height"),
					savedInstanceState.getInt("request_code_select_picture"),
					savedInstanceState.getInt("request_code_crop"),
					callbacks);
			imageGetter.setUri(Uri.parse(savedInstanceState.getString("uri")));
			return imageGetter;
		}
		return null;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public Uri getUri() {
		return uri;
	}

	public void destroy(){
		Log.d(DEBUG_TAG+".destroy", "deleteing... ");
		try {
			Log.d(DEBUG_TAG+".destroy", "deleteing... " + uri.getPath());
			destroy(uri.getPath());
		}catch (NullPointerException npe){
			Log.d(DEBUG_TAG+".destroy", "nothing!");
		}
	}

	public void destroy(String path){
		Log.d(DEBUG_TAG+".destroy", "deleteing... ");
		try {
			Log.d(DEBUG_TAG+".destroy", "deleteing... " + path);
			new File(uri.getPath()).delete();
		}catch (NullPointerException npe){
			Log.d(DEBUG_TAG+".destroy", "nothing!");
		}
	}

	public interface Callbacks {
		Activity getActivity();
		void setImage(String path, String tag);
		boolean shouldDestroyFile(Source source);
	}

	public enum Source {
		CAMERA, GALLERY
	}

}
