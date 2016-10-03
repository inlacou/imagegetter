package libraries.inlacou.com.imagegetter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by inlacou on 12/05/15.
 */
public class CropActivity extends AppCompatActivity {

	private static final String DEBUG_TAG = CropActivity.class.getName();
	public static final String INTENT_EXTRA_URI = "intent_extra_uri";
	public static final String RESPONSE_EXTRA_BITMAP = "RESPONSE_EXTRA_BITMAP";
	public static final String INTENT_EXTRA_CIRCULAR = "INTENT_EXTRA_CIRCULAR";
	public static final String INTENT_EXTRA_WIDTH = "INTENT_EXTRA_WIDTH";
	public static final String INTENT_EXTRA_HEIGHT = "INTENT_EXTRA_HEIGHT";
	public static final String INTENT_EXTRA_FIXED = "INTENT_EXTRA_FIXED";
	private CropImageView cropImageView;
	private Uri uri;
	private boolean circular, fixed;
	private int width, height;
	private ProgressDialog progressDialog;

	public static void navigateForResult(AppCompatActivity activity, String uri, boolean circular, boolean fixed, int width, int height, int requestCode) {
		Intent intent = new Intent(activity, CropActivity.class);

		intent.putExtra(INTENT_EXTRA_URI, uri);
		intent.putExtra(INTENT_EXTRA_CIRCULAR, circular);
		intent.putExtra(INTENT_EXTRA_WIDTH, width);
		intent.putExtra(INTENT_EXTRA_HEIGHT, height);
		intent.putExtra(INTENT_EXTRA_FIXED, fixed);

		activity.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(DEBUG_TAG, "onCreate");
		setContentView(R.layout.activity_crop);

		uri = getIntent().getParcelableExtra(INTENT_EXTRA_URI);
		circular = getIntent().getBooleanExtra(INTENT_EXTRA_CIRCULAR, false);
		fixed = getIntent().getBooleanExtra(INTENT_EXTRA_FIXED, false);
		width = getIntent().getIntExtra(INTENT_EXTRA_WIDTH, 1);
		height = getIntent().getIntExtra(INTENT_EXTRA_HEIGHT, 1);

		initialize();
		populate();

		Toolbar toolbar = (Toolbar) findViewById(R.id.activity_circle_info_toolbar);
		if (toolbar != null) {
			try {
				toolbar.setTitleTextColor(Color.WHITE);
				setSupportActionBar(toolbar);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}catch (IllegalStateException ise){
				ise.printStackTrace();
				toolbar.setVisibility(View.GONE);
			}
		}
	}

	private void initialize(){
		cropImageView = (CropImageView) findViewById(R.id.cropImageView);
		progressDialog = new ProgressDialog(this);
		progressDialog.setIndeterminate(true);
		progressDialog.setTitle(R.string.Please_wait);
		progressDialog.setCancelable(false);
	}

	private void populate(){
		if(circular) {
			cropImageView.setCropShape(CropImageView.CropShape.OVAL);
		}
		if(fixed){
			if(width==-1){
				width = 1;
			}
			if(height==-1){
				height = 1;
			}
			cropImageView.setAspectRatio(width, height);
		}
		cropImageView.setImageUriAsync(uri);
		cropImageView.setFixedAspectRatio(fixed);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_circle_crop, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(DEBUG_TAG, "onOptionsItemSelected");
		if(item.getItemId()==android.R.id.home) {
			Log.d(DEBUG_TAG, "home");
			onBackPressed();
			return true;
		}else if(item.getItemId()== R.id.action_complete_crop) {
			Log.d(DEBUG_TAG, "action_complete_crop");
			progressDialog.show();
			Log.d(DEBUG_TAG, "before: getCroppedImageAsync");
			cropImageView.setOnGetCroppedImageCompleteListener(new CropImageView.OnGetCroppedImageCompleteListener() {
				@Override
				public void onGetCroppedImageComplete(CropImageView view, Bitmap bitmap, Exception error) {
					try {
						Log.d(DEBUG_TAG, "uri: " + uri);
						//Write file
						FileOutputStream stream;
						String filename;
						try {
							filename = uri.toString().replace("file:", "");
							stream = new FileOutputStream(filename);
						} catch (FileNotFoundException fnfe) {
							filename = "///storage/emulated/0/"+getString(R.string.app_name)+"/croppedbitmap" + System.currentTimeMillis() + "." + "jpg";
							stream = new FileOutputStream(filename);
							//stream = CropActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
						}
						bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream);
						Log.d(DEBUG_TAG, "filename: " + filename);

						//Cleanup
						stream.close();
						bitmap.recycle();

						//Pop intent
						Intent intent = new Intent();
						intent.putExtra(RESPONSE_EXTRA_BITMAP, filename);
						setResult(RESULT_OK, intent);
						progressDialog.dismiss();
						finish();
					} catch (Exception e) {
						e.printStackTrace();
						progressDialog.dismiss();
						finish();
					}
				}
			});
			cropImageView.getCroppedImageAsync();
			return true;
		}else{
				return super.onOptionsItemSelected(item);
		}
	}

}
