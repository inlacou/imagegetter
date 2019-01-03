package libraries.inlacou.com.imagegettersampleapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.view.View
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView

import libraries.inlacou.com.imagegetter.ImageGetter
import libraries.inlacou.com.imagegetter.ImageUtils

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

	private var imageView: ImageView? = null
	private var imageGetter: ImageGetter? = null

	private val imageGetterCallbacks: ImageGetter.Callbacks
		get() = object : ImageGetter.Callbacks {
			override fun setImage(path: String, tag: String?) {
				imageView?.let { imageView ->
					ImageUtils.setImageFromMemory(
							activity = this@MainActivity,
							filename =  path,
							maxSize = 1024,
							imageView = imageView,
							adjustViewBounds = true,
							setScaleType = true)
				}
			}
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		val toggle = ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
		drawer.setDrawerListener(toggle)
		toggle.syncState()

		val navigationView = findViewById<NavigationView>(R.id.nav_view)
		navigationView.setNavigationItemSelectedListener(this)

		imageView = findViewById(R.id.imageView)
		imageView?.setOnClickListener {
			if (imageGetter == null) {
				imageGetter = ImageGetter(
						this@MainActivity,
						true,
						false,
						true,
						true,
						true,
						-1,
						-1,
						REQUEST_CODE_SELECT_PICTURE,
						REQUEST_CODE_CROP,
						imageGetterCallbacks)
			}
			imageGetter?.start("", true)
		}
	}

	override fun onBackPressed() {
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START)
		} else {
			super.onBackPressed()
		}
	}

	override fun onDestroy() {
		imageGetter?.destroy()
		super.onDestroy()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.none, menu)
		return true
	}

	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		imageGetter?.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		imageGetter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	public override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		imageGetter?.onSaveInstanceState(outState)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		imageGetter = ImageGetter.onRestoreInstanceState(savedInstanceState, this, imageGetterCallbacks)
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
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
