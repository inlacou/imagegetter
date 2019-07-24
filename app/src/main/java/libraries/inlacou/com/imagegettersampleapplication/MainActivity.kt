package libraries.inlacou.com.imagegettersampleapplication

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import libraries.inlacou.com.imagegetter.ImageGetter
import libraries.inlacou.com.imagegetter.ImageUtils
import timber.log.Timber

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
	
	private var imageView: ImageView? = null
	private var tietMaxImageSize: TextInputEditText? = null
	private var tietMaxFileSize: TextInputEditText? = null
	private var spinnerFormat: AppCompatSpinner? = null
	private var switchCrop: SwitchCompat? = null
	private var switchCircularCropUi: SwitchCompat? = null
	private var switchCropRatio: SwitchCompat? = null
	private var switchAllowGallery: SwitchCompat? = null
	private var switchAllowCamera: SwitchCompat? = null
	
	private var imageGetter: ImageGetter? = null
	private var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
	private var maxImageSize: Int = 512
	private var maxFileSize: Int = 400
	private var crop: Boolean = true
	private var circularCropUi: Boolean = true
	private var fixedRatio: Boolean = true
	private var allowCamera: Boolean = true
	private var allowGallery: Boolean = true
	
	private val imageGetterCallbacks: ImageGetter.Callbacks
		get() = object : ImageGetter.Callbacks {
			override fun setImage(path: String, tag: String?) {
				Timber.d("setImage($path, $tag)")
				imageView?.let { imageView ->
					ImageUtils.setImageFromMemory(
							activity = this@MainActivity,
							filename =  path,
							maxImageSize = 1024,
							imageView = imageView,
							adjustViewBounds = true,
							setScaleType = true)
				}
			}
		}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Timber.d("onCreate()")
		setContentView(R.layout.activity_main)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		
		tietMaxFileSize = findViewById(R.id.tiet_max_file_size)
		tietMaxImageSize = findViewById(R.id.tiet_max_image_size)
		spinnerFormat = findViewById(R.id.spinner_format)
		switchCrop = findViewById(R.id.switch_crop)
		switchCircularCropUi = findViewById(R.id.switch_crop_ui)
		switchCropRatio = findViewById(R.id.switch_crop_ratio)
		switchAllowCamera = findViewById(R.id.switch_allow_camera)
		switchAllowGallery = findViewById(R.id.switch_allow_gallery)
		
		switchCrop?.isChecked = crop
		switchCircularCropUi?.isChecked = circularCropUi
		switchCropRatio?.isChecked = fixedRatio
		switchAllowCamera?.isChecked = allowCamera
		switchAllowGallery?.isChecked = allowGallery
		
		switchCrop?.setOnCheckedChangeListener { buttonView, isChecked -> crop = isChecked }
		switchCircularCropUi?.setOnCheckedChangeListener { buttonView, isChecked -> circularCropUi = isChecked }
		switchCropRatio?.setOnCheckedChangeListener { buttonView, isChecked -> fixedRatio = isChecked }
		switchAllowCamera?.setOnCheckedChangeListener { buttonView, isChecked -> allowCamera = isChecked }
		switchAllowGallery?.setOnCheckedChangeListener { buttonView, isChecked -> allowGallery = isChecked }
		
		spinnerFormat?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("PNG", "JPEG", "WEBP"))
		spinnerFormat?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onNothingSelected(parent: AdapterView<*>?) {}
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				compressFormat = when(position){
					0 -> Bitmap.CompressFormat.PNG
					1 -> Bitmap.CompressFormat.JPEG
					else -> Bitmap.CompressFormat.WEBP
				}
			}
		}
		
		tietMaxFileSize?.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {}
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				maxFileSize = s?.toString()?.toIntOrNull() ?: maxFileSize
			}
		})
		
		tietMaxImageSize?.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {}
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				maxImageSize = s?.toString()?.toIntOrNull() ?: maxImageSize
			}
		})
		
		tietMaxFileSize?.setText(maxFileSize.toString())
		tietMaxImageSize?.setText(maxImageSize.toString())
		
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		val toggle = ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
		drawer.setDrawerListener(toggle)
		toggle.syncState()
		
		val navigationView = findViewById<NavigationView>(R.id.nav_view)
		navigationView.setNavigationItemSelectedListener(this)
		
		imageView = findViewById(R.id.imageView)
		imageView?.setOnClickListener {
			imageGetter?.destroy()
			imageGetter = ImageGetter(
					this@MainActivity,
					true,
					crop,
					circularCropUi,
					fixedRatio,
					allowCamera,
					allowGallery,
					compressFormat,
					-1,
					-1,
					maxImageSize,
					maxFileSize,
					REQUEST_CODE_SELECT_PICTURE,
					REQUEST_CODE_CROP,
					imageGetterCallbacks)
			imageGetter?.start("", true)
		}
	}
	
	override fun onLowMemory() {
		Timber.d("onLowMemory()")
		super.onLowMemory()
	}
	
	override fun onBackPressed() {
		Timber.d("onBackPressed()")
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START)
		} else {
			super.onBackPressed()
		}
	}
	
	override fun onDestroy() {
		Timber.d("onDestroy()")
		imageGetter?.destroy()
		super.onDestroy()
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Timber.d("onCreateOptionsMenu()")
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.none, menu)
		return true
	}
	
	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		Timber.d("onActivityResult()")
		imageGetter?.onActivityResult(requestCode, resultCode, data)
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		Timber.d("onRequestPermissionsResult()")
		imageGetter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
	
	public override fun onSaveInstanceState(outState: Bundle) {
		Timber.d("onSaveInstanceState()")
		super.onSaveInstanceState(outState)
		imageGetter?.onSaveInstanceState(outState)
	}
	
	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		Timber.d("onRestoreInstanceState()")
		imageGetter = ImageGetter.onRestoreInstanceState(savedInstanceState, this, imageGetterCallbacks)
	}
	
	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		Timber.d("onNavigationItemSelected()")
		// Handle navigation view item clicks here.
		val id = item.itemId
		
		if (id == R.id.nav_send) {
			val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/inlacou/imagegetter"))
			startActivity(browserIntent)
		}
		
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		drawer.closeDrawer(GravityCompat.START)
		return true
	}
	
	companion object {
		private const val REQUEST_CODE_SELECT_PICTURE = 0
		private const val REQUEST_CODE_CROP = 1
	}
}
