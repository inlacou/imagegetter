package libraries.inlacou.com.imagegetter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by inlacoubyv on 19/11/15.
 */
public class PermissionUtils {

	public static final String DEBUG_TAG = PermissionUtils.class.getName();

	//TODO handle negatives http://developer.android.com/intl/es/training/permissions/requesting.html

	public static boolean checkPermission(Context c, Permission permission) {
		return ContextCompat.checkSelfPermission(c, permission.permission) == PackageManager.PERMISSION_GRANTED;
	}

	public static void checkGetIfNotPermission(Activity activity, Permission permission, Callbacks callbacks) {
		if(ContextCompat.checkSelfPermission(activity, permission.permission) == PackageManager.PERMISSION_GRANTED){
			callbacks.onPermissionGranted();
		}else{
			ActivityCompat.requestPermissions(activity,
					new String[]{permission.permission},
					permission.requestCode);
		}
	}

	public enum Permission{
		externalStorage(Manifest.permission.WRITE_EXTERNAL_STORAGE, 254),
		camera(Manifest.permission.CAMERA, 253);

		public final int requestCode;
		public final String permission;

		Permission(String permission, int requestCode){
			this.permission = permission;
			this.requestCode = requestCode;
		}

		public int getRequestCode() {
			return requestCode;
		}

		public String getPermission() {
			return permission;
		}
	}

	public interface Callbacks{
		void onPermissionGranted();
	}
}
