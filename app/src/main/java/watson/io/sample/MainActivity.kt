package watson.io.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import watson.io.sample.helper.checkAllMatched
import watson.io.sample.helper.viewModelProvider
import watson.io.sample.ui.LaunchDestination.MAIN_ACTIVITY
import watson.io.sample.ui.LaunchDestination.ONBOARDING
import watson.io.sample.ui.LaunchViewModel
import watson.io.sample.usecase.EventObserver

class OnboardingActivity : AppCompatActivity() {

}

class MainActivity : AppCompatActivity() {

    lateinit var viewModelFactory: ViewModelProvider.Factory


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel: LaunchViewModel = viewModelProvider(viewModelFactory)
        viewModel.launchDestination.observe(this, EventObserver { destination ->
            when (destination) {
                MAIN_ACTIVITY -> startActivity(Intent(this, MainActivity::class.java))
                ONBOARDING -> startActivity(Intent(this, OnboardingActivity::class.java))
            }.checkAllMatched
            finish()
        })
    }
}
