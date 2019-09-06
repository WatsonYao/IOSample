package watson.io.sample.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import watson.io.sample.helper.map
import watson.io.sample.usecase.Event
import watson.io.sample.usecase.OnboardingCompletedUseCase
import watson.io.sample.usecase.Result

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

enum class LaunchDestination {
    ONBOARDING,
    MAIN_ACTIVITY
}