package libraries.inlacou.com.imagegetter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by inlacoubyv on 19/11/15.
 */
public class PermissionUtils {

	//Should handle negatives http://developer.android.com/intl/es/training/permissions/requesting.html

	public static boolean permissionAllowed(Context c, Permission permission) {
		return ContextCompat.checkSelfPermission(c, permission.permission) == PackageManager.PERMISSION_GRANTED;
	}
	
	public static boolean permissionNotAllowed(Context c, Permission permission) {
		return ContextCompat.checkSelfPermission(c, permission.permission) != PackageManager.PERMISSION_GRANTED;
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
