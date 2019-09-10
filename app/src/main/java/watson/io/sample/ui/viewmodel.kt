package watson.io.sample.ui

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import watson.io.sample.helper.log
import watson.io.sample.helper.map
import watson.io.sample.model.MovieDetailModel
import watson.io.sample.model.MovieModel
import watson.io.sample.usecase.*

/**
 * Logic for determining which screen to send users to on app launch.
 */
class LaunchViewModel(onboardingCompletedUseCase: OnboardingCompletedUseCase) : ViewModel() {

  private val onboardingCompletedResult = MutableLiveData<Result<Boolean>>()
  val launchDestination: LiveData<Event<LaunchDestination>>

  init {
    // Check if onboarding has already been completed and then navigate the user accordingly
    onboardingCompletedUseCase(Unit, onboardingCompletedResult)
    launchDestination = onboardingCompletedResult.map {
      // If this check fails, prefer to launch main activity than show onboarding too often
      if ((it as? Result.Success)?.data == false) {
        Event(LaunchDestination.ONBOARDING)
      } else {
        Event(LaunchDestination.MAIN_ACTIVITY)
      }
    }
  }
}

open class BaseVM : BaseViewModel() {
  protected var init: Boolean = false

  inline fun doWithForceState(force: Boolean, exe: () -> Unit) {
    if (init) {
      if (!force) {
        return
      }
    }
    init = true
    exe()
  }
}

class DiscoverVM : BaseVM() {

  private val _movies = StateLiveData<List<MovieModel>>()
  val movies: StateLiveData<List<MovieModel>>
    get() = _movies

  fun fetchDiscover(force: Boolean = false) {
    doWithForceState(force) {
      "discover".log()
      load {
        MovieUsecase().exe()
      }.result({
        _movies.postSuccess(it)
      }, {
        it.printStackTrace()
        _movies.postError(it)
      })
    }
  }
}

class MovieDetailVM : BaseVM() {

  private val _data = StateLiveData<MovieDetailModel>()
  val data: StateLiveData<MovieDetailModel>
    get() = _data

  fun fetchMovieDetail(id: String, force: Boolean = false) {
    if (TextUtils.isEmpty(id)) return
    doWithForceState(force) {
      load {
        MovieDetailUsecase().exe(id)
      }.result({
        _data.postSuccess(it)
      }, {
        it.printStackTrace()
        _data.postError(it)
      })
    }
  }
}

enum class LaunchDestination {
  ONBOARDING,
  MAIN_ACTIVITY
}