package libraries.inlacou.com.imagegetter

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.theartofdev.edmodo.cropper.CropImageView
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Created by inlacou on 12/05/15.
 */
class CropActivity : AppCompatActivity() {
	private var cropImageView: CropImageView? = null
	private var uri: Uri? = null
	private var log: Boolean = false
	private var circular: Boolean = false
	private var fixed: Boolean = false
	private var width: Int = 0
	private var height: Int = 0
	private var progressDialog: ProgressDialog? = null

	private fun log(s: String){
		if(log) Timber.d(s)
	}

	private fun log(tag: String, s: String){
		if(log) Timber.d("$tag | $s")
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		log("onCreate")
		setContentView(R.layout.activity_crop)

		if(intent.hasExtra(INTENT_EXTRA_URI)) uri = intent.getParcelableExtra(INTENT_EXTRA_URI)
		log = intent.getBooleanExtra(INTENT_EXTRA_LOG, false)
		circular = intent.getBooleanExtra(INTENT_EXTRA_CIRCULAR, false)
		fixed = intent.getBooleanExtra(INTENT_EXTRA_FIXED, false)
		width = intent.getIntExtra(INTENT_EXTRA_WIDTH, 1)
		height = intent.getIntExtra(INTENT_EXTRA_HEIGHT, 1)

		Timber.d("CropActivity onCreate | uri: ${uri.toString()} | circular | fixed |")

		initialize()
		populate()

		val toolbar = findViewById<Toolbar>(R.id.activity_circle_info_toolbar)
		if (toolbar != null) {
			try {
				toolbar.setTitleTextColor(Color.WHITE)
				setSupportActionBar(toolbar)
				supportActionBar?.setDisplayHomeAsUpEnabled(true)
			} catch (ise: IllegalStateException) {
				toolbar.visibility = View.GONE
			}

		}
	}

	private fun initialize() {
		log("initialize")
		cropImageView = findViewById(R.id.cropImageView)
		progressDialog = ProgressDialog(this)
		progressDialog?.isIndeterminate = true
		progressDialog?.setTitle(R.string.Please_wait)
		progressDialog?.setCancelable(false)
	}

	private fun populate() {
		log("populate")
		if (circular) {
			cropImageView?.cropShape = CropImageView.CropShape.OVAL
		}
		if (fixed) {
			if (width <= 0) {
				width = 1
			}
			if (height <= 0) {
				height = 1
			}
			cropImageView?.setAspectRatio(width, height)
		}
		cropImageView?.setImageUriAsync(uri)
		cropImageView?.setFixedAspectRatio(fixed)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		log("onCreateOptionsMenu")
		val menuInflater = menuInflater
		menuInflater.inflate(R.menu.menu_circle_crop, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		log("onOptionsItemSelected")
		when(item.itemId) {
			 android.R.id.home -> {
				onBackPressed()
				return true
			}
			R.id.action_complete_crop -> {
				progressDialog?.show()
				cropImageView?.setOnCropImageCompleteListener { view, result ->
					try {
						//Write file
						val filename = "${ImageUtils.getRootUri(this)}${ImageUtils.uniqueImageFilename}"
						val stream = FileOutputStream(filename)

						log("onOptionsItemSelected", "filename: $filename")
						result.bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)

						//Cleanup
						stream.close()
						result.bitmap.recycle()

						//Pop intent
						val intent = Intent()
						intent.putExtra(RESPONSE_EXTRA_BITMAP, filename)
						setResult(Activity.RESULT_OK, intent)
						progressDialog?.dismiss()
						finish()
					} catch (e: Exception) {
						e.printStackTrace()
						progressDialog?.dismiss()
						finish()
					}
				}
				cropImageView?.getCroppedImageAsync()
				return true
			}
			else -> return super.onOptionsItemSelected(item)
		}
	}

	companion object {
		const val INTENT_EXTRA_URI = "intent_extra_uri"
		const val INTENT_EXTRA_LOG = "intent_extra_log"
		const val RESPONSE_EXTRA_BITMAP = "RESPONSE_EXTRA_BITMAP"
		const val INTENT_EXTRA_CIRCULAR = "INTENT_EXTRA_CIRCULAR"
		const val INTENT_EXTRA_WIDTH = "INTENT_EXTRA_WIDTH"
		const val INTENT_EXTRA_HEIGHT = "INTENT_EXTRA_HEIGHT"
		const val INTENT_EXTRA_FIXED = "INTENT_EXTRA_FIXED"

		fun navigateForResult(activity: AppCompatActivity, uri: String, circular: Boolean, fixed: Boolean, width: Int, height: Int, requestCode: Int) {
			val intent = Intent(activity, CropActivity::class.java)

			intent.putExtra(INTENT_EXTRA_URI, uri)
			intent.putExtra(INTENT_EXTRA_CIRCULAR, circular)
			intent.putExtra(INTENT_EXTRA_WIDTH, width)
			intent.putExtra(INTENT_EXTRA_HEIGHT, height)
			intent.putExtra(INTENT_EXTRA_FIXED, fixed)

			activity.startActivityForResult(intent, requestCode)
		}
	}

}
