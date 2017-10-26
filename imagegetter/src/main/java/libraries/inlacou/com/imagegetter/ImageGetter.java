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

/**
 * Created by inlacou on 26/04/16.
 */
public class ImageGetter {
	private static final String DEBUG_TAG = ImageGetter.class.getName();
	private final Callbacks callbacks;
	private final boolean crop, circular, fixed;
	private int request_code_select_picture, request_code_crop;
	private final int width, height;

	private Activity activity;
	private Uri uri;
	private String tag;
	private boolean useCamera;

	public ImageGetter(Activity activity, boolean crop, boolean circular, boolean fixed, int width, int height,
	                   int request_code_select_picture, int request_code_crop, boolean useCamera, Callbacks callbacks) {
		this.activity = activity;
		this.crop = crop;
		this.circular = circular;
		this.width = width;
		this.height = height;
		this.fixed = fixed;
		this.request_code_select_picture = request_code_select_picture;
		this.request_code_crop = request_code_crop;
		this.useCamera = useCamera;
		this.callbacks = callbacks;
	}

	public void start(String tag){
		destroy();
		this.uri = ImageUtils.generateURI(activity);
		this.tag = tag;
		checkExternalStoragePermission();
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		for (int i=0; i<permissions.length; i++){
			Log.d(DEBUG_TAG, i + " onActivityResult(" + requestCode + ", " + permissions[i] + ", " + grantResults[i] + ")");
			if(permissions[i].equalsIgnoreCase(PermissionUtils.Permission.externalStorage.permission)){
				checkCameraPermission(uri);
			}else if(permissions[i].equalsIgnoreCase(PermissionUtils.Permission.camera.permission)){
				ImageUtils.openImageIntent(activity, useCamera, request_code_select_picture, uri);
			}
		}
	}

	private void checkExternalStoragePermission() {
		final Uri aux = this.uri;
		PermissionUtils.checkGetIfNotPermission(activity, new PermissionUtils.Callbacks() {
			@Override
			public void onPermissionGranted() {
				if(useCamera) {
					checkCameraPermission(aux);
				}else{
					ImageUtils.openImageIntent(activity, false, request_code_select_picture, aux);
				}
			}

			@Override
			public Activity getActivity() {
				return activity;
			}
		}, PermissionUtils.Permission.externalStorage);
	}

	private void checkCameraPermission(final Uri aux){
		PermissionUtils.checkGetIfNotPermission(activity, new PermissionUtils.Callbacks() {
			@Override
			public void onPermissionGranted() {
				ImageUtils.openImageIntent(activity, useCamera, request_code_select_picture, aux);
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
		outState.putBoolean("use_camera", useCamera);
	}

	public static ImageGetter onRestoreInstanceState(Bundle savedInstanceState, Activity activity, Callbacks callbacks) {
		if(savedInstanceState.containsKey("crop") && savedInstanceState.containsKey("circular") && savedInstanceState.containsKey("uri")) {
			ImageGetter imageGetter = new ImageGetter(activity,
					savedInstanceState.getBoolean("crop"),
					savedInstanceState.getBoolean("circular"),
					savedInstanceState.getBoolean("fixed"),
					savedInstanceState.getInt("width"),
					savedInstanceState.getInt("height"),
					savedInstanceState.getInt("request_code_select_picture"),
					savedInstanceState.getInt("request_code_crop"),
					savedInstanceState.getBoolean("use_camera"),
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
			Log.d(DEBUG_TAG+".destroy", "uri.getPath: " + uri.getPath());
			destroy(uri.getPath());
		}catch (NullPointerException npe){
			Log.d(DEBUG_TAG+".destroy", "nothing!");
		}
	}

	public void destroy(String path){
		Log.d(DEBUG_TAG+".destroy", "deleteing... ");
		try {
			Log.d(DEBUG_TAG+".destroy", "received path: " + path);
			new File(uri.getPath()).delete();
		}catch (NullPointerException npe){
			Log.d(DEBUG_TAG+".destroy", "nothing!");
		}
	}

	public interface Callbacks {
		void setImage(String path, String tag);
	}

	public enum Source {
		CAMERA, GALLERY
	}

}
