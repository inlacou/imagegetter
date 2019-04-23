package libraries.inlacou.com.imagegettersampleapplication

import android.app.Application
import timber.log.Timber

class AppCtrl: Application() {

	override fun onCreate() {
		super.onCreate()
		Timber.plant(Timber.DebugTree())
	}

}