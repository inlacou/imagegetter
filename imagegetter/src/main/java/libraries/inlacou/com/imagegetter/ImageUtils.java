package libraries.inlacou.com.imagegetter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
	private static final String DEBUG_TAG = ImageUtils.class.getName();
	public static final String FORMAT = "jpg";
	public static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;

	public static String getUniqueImageFilename(){
		return "img_"+ System.currentTimeMillis() + "."+ImageUtils.FORMAT;
	}

	public static Drawable fromUri(ContentResolver contentResolver, Uri uri) {
		try {
			InputStream inputStream = contentResolver.openInputStream(uri);
			return Drawable.createFromStream(inputStream, uri.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			return null;
		}
	}

	public static byte[] base64StringToByteArray(String s){
		return Base64.decode(s, Base64.DEFAULT);
	}

	public static String byteArrayToBase64String(byte[] bytes) {
		String s = Base64.encodeToString(bytes, Base64.DEFAULT);
		Log.d(DEBUG_TAG, s);
		return s;
	}

	public static Drawable getDawableResource(Context c, String ImageName) {
		return c.getResources().getDrawable(c.getResources().getIdentifier(ImageName, "drawable", c.getPackageName()));
	}


	public static byte[] bitmapToByteArray(Bitmap bitmap){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}

	public static Uri generateURI(){
		final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "LEDT" + File.separator);
		Log.d(DEBUG_TAG+".openImageIntent", "root: " + Environment.getExternalStorageDirectory() + File.separator + "LEDT" + File.separator);
		root.mkdirs();
		final String fname = ImageUtils.getUniqueImageFilename();
		Log.d(DEBUG_TAG+".openImageIntent", "fname: " + fname);
		final File sdImageMainDirectory = new File(root, fname);
		Uri outputImageUri = Uri.fromFile(sdImageMainDirectory);
		return outputImageUri;
	}

	public static boolean deleteFile(Uri uri){
		Log.d(DEBUG_TAG, "Trying to delete: " + uri);
		File fdelete = new File(uri.getPath());
		if (fdelete.exists()) {
			return fdelete.delete();
		}
		return false;
	}

	public static boolean deleteFile(String s){
		return deleteFile(Uri.parse(s));
	}

	public static void openImageIntent(Activity activity, int YOUR_SELECT_PICTURE_REQUEST_CODE, Uri outputImageUri) {
		final PackageManager packageManager = activity.getPackageManager();
		Intent chooserIntent = null;

		boolean camera = false, gallery = false;
		Intent galleryIntent = null;
		List<Intent> cameraIntents = null;

		// Camera.
		if(PermissionUtils.checkPermission(activity, PermissionUtils.Permission.camera)) {
			cameraIntents = new ArrayList<>();
			final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
			for (ResolveInfo res : listCam) {
				final String packageName = res.activityInfo.packageName;
				final Intent intent = new Intent(captureIntent);
				intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
				intent.setPackage(packageName);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, outputImageUri);
				cameraIntents.add(intent);
			}
			camera = true;
		}

		// Filesystem.
		if(PermissionUtils.checkPermission(activity, PermissionUtils.Permission.externalStorage)) {
			galleryIntent = new Intent();
			galleryIntent.setType("image/*");
			galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
			gallery = true;
		}

		if (camera && gallery) {
			chooserIntent = Intent.createChooser(galleryIntent, activity.getString(R.string.select_source));
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
		}else if(camera){
			chooserIntent = Intent.createChooser(cameraIntents.remove(cameraIntents.size()-1), activity.getString(R.string.select_source));
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
		}else if(gallery){
			chooserIntent = Intent.createChooser(galleryIntent, activity.getString(R.string.select_source));
		}

		// Add the camera options.

		if(chooserIntent!=null) activity.startActivityForResult(chooserIntent, YOUR_SELECT_PICTURE_REQUEST_CODE);
	}

	public static int getCameraPhotoOrientation(Context context, Uri imageUri){
		try {
			context.getContentResolver().notifyChange(imageUri, null);
			File imageFile = new File(imageUri.getPath());
			ExifInterface exif = new ExifInterface(
					imageFile.getAbsolutePath());
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_270:
					return 270;
				case ExifInterface.ORIENTATION_ROTATE_180:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_90:
					return 90;
				default:
					return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static Drawable getImage(Context c, String ImageName) {
		return c.getResources().getDrawable(c.getResources().getIdentifier(ImageName, "drawable", c.getPackageName()));
	}

	public static Bitmap rotateBitmap(int cameraPhotoOrientation, Bitmap bitmap) {
		Matrix matrix = new Matrix();

		matrix.postRotate(cameraPhotoOrientation);

		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

	public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
		Bitmap res;
		try{
			res = Bitmap.createScaledBitmap(bitmap, width, height, true);
		}catch (IllegalArgumentException iae) {
			res = Bitmap.createScaledBitmap(bitmap, 50, 50, true);
		}

		return res;
	}

	public static Bitmap scaleBitmapKeepAspectRatio(Bitmap bitmap, int scaleSize) {
		Log.d(DEBUG_TAG+".scaleBmKeepAspectRatio", "bitmap.getWidth: " + bitmap.getWidth());
		Log.d(DEBUG_TAG+".scaleBmKeepAspectRatio", "bitmap.getHeight: " + bitmap.getHeight());
		Log.d(DEBUG_TAG+".scaleBmKeepAspectRatio", "scaleSize: " + scaleSize);
		Bitmap resizedBitmap = null;
		int originalWidth = bitmap.getWidth();
		int originalHeight = bitmap.getHeight();
		int newWidth = -1;
		int newHeight = -1;
		float multFactor = -1.0F;
		if(originalHeight > originalWidth) {
			newHeight = scaleSize ;
			multFactor = (float) originalWidth/(float) originalHeight;
			newWidth = (int) (newHeight*multFactor);
		} else if(originalWidth > originalHeight) {
			newWidth = scaleSize ;
			multFactor = (float) originalHeight/ (float)originalWidth;
			newHeight = (int) (newWidth*multFactor);
		} else if(originalHeight == originalWidth) {
			newHeight = scaleSize ;
			newWidth = scaleSize ;
		}
		resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
		Log.d(DEBUG_TAG, "resizedBitmap.getWidth: " + resizedBitmap.getWidth());
		Log.d(DEBUG_TAG, "resizedBitmap.getHeight: " + resizedBitmap.getHeight());
		return resizedBitmap;
	}

	public static Bitmap scaleBitmapKeepAspectRatio(Bitmap originalImage, int width, int height) {
		Bitmap background = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);
		float originalWidth = originalImage.getWidth(), originalHeight = originalImage.getHeight();
		Canvas canvas = new Canvas(background);
		float scale = width/originalWidth;
		float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale)/2.0f;
		Matrix transformation = new Matrix();
		transformation.postTranslate(xTranslation, yTranslation);
		transformation.preScale(scale, scale);
		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		canvas.drawBitmap(originalImage, transformation, paint);
		return background;
	}

	public static int getHeightScaleToWidth(int originalWidth, int originalHeight, int width) {
		int newWidth = -1;
		int newHeight = -1;
		float multFactor = -1.0F;
		if(originalHeight == originalWidth) {
			newHeight = width ;
		}else{
			newWidth = width ;
			multFactor = (float) originalHeight/ (float)originalWidth;
			newHeight = (int) (newWidth*multFactor);
		}
		return newHeight;
	}

	public static Drawable base64ToDrawable(Context context, String base64) {
		byte[] bytes = base64StringToByteArray(base64);

		return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
	}

	public static Bitmap base64toBitmap(Context context, String base64) {
		byte[] bytes = base64StringToByteArray(base64);

		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}

	public static void setImage(Context context, Bitmap bitmap, ImageView imageView){
		Drawable d = new BitmapDrawable(context.getResources(), bitmap);

		imageView.setBackground(d);
	}

	private static void setImageScale(Context context, Bitmap bitmap, ImageView imageView, View view){
		bitmap = ImageUtils.scaleBitmap(bitmap, view.getWidth(), view.getHeight());

		Drawable d = new BitmapDrawable(context.getResources(), bitmap);

		imageView.setBackground(d);
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

	public static int pixelToDP(Context c, float pixels){
		float density = c.getResources().getDisplayMetrics().density;
		return (int) (pixels / density);
	}

	public static int dpToPixel(Context c, float dp){
		float density = c.getResources().getDisplayMetrics().density;
		return (int) (dp * density);
	}

	/**
	 * Turn drawable resource into byte array.
	 *
	 * @param context parent context
	 * @param id      drawable resource id
	 * @return byte array
	 */
	public static byte[] getFileDataFromDrawable(Context context, int id) {
		Drawable drawable = ContextCompat.getDrawable(context, id);
		Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Turn drawable into byte array.
	 *
	 * @param drawable data
	 * @return byte array
	 */
	public static byte[] getFileDataFromDrawable(Context context, Drawable drawable) {
		Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Turn bitmap into byte array.
	 *
	 * @param bitmap data
	 * @return byte array
	 */
	public static byte[] getFileDataFromBitmap(Context context, Bitmap bitmap) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	public static void setImageFromMemory(Activity activity, String filename, ImageView imageView) {
		Log.d(DEBUG_TAG, ".setImage filename: " + filename);
		Bitmap selectedImage = null;
		try {
			selectedImage = ImageGetter.getBitmapFromPath(filename, 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(selectedImage==null) {
			return;
		}
		imageView.setAdjustViewBounds(true);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		Drawable drawable = new BitmapDrawable(activity.getResources(), selectedImage);
		imageView.setImageDrawable(drawable);
	}
}