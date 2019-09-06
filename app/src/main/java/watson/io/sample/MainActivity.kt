package watson.io.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import watson.io.sample.helper.log
import watson.io.sample.helper.viewModelProvider
import watson.io.sample.repo.Movie
import watson.io.sample.repo.RetrofitClient

class OnboardingActivity : AppCompatActivity() {

}

class MainActivity : AppCompatActivity() {

  lateinit var viewModelFactory: ViewModelProvider.Factory


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

/*    val viewModel: LaunchViewModel = viewModelProvider(viewModelFactory)
    viewModel.launchDestination.observe(this, EventObserver { destination ->
      when (destination) {
        MAIN_ACTIVITY -> startActivity(Intent(this, MainActivity::class.java))
        ONBOARDING -> startActivity(Intent(this, OnboardingActivity::class.java))
      }.checkAllMatched
      finish()
    })*/

    val mViewModel = viewModelProvider<TestViewModel>()
    mViewModel.list.observe(this, Observer {
      "list size = ${it.size}".log()
    })
  }
}


class MainUsecase {
  suspend fun exe() = RetrofitClient.service.discover()
}

class TestViewModel : ViewModel() {

  var list = MutableLiveData<List<Movie>>()

  private val useCase = MainUsecase()

  init {
    viewModelScope.launch {
      val result = useCase.exe()
      Log.i("temp", "result->$result")
      list.postValue(result.results)
    }
  }
}
