package watson.io.sample

import android.app.Application

object KeyStore {
  private var appKey: String? = null

  fun initKey(key: String) {
    appKey = key
  }

  fun getKey() = appKey!!
}

class App : Application() {

  lateinit var appKey: String
  override fun onCreate() {
    super.onCreate()
    KeyStore.initKey(getString(R.string.service_key))
  }
}