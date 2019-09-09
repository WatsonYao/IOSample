package watson.io.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import watson.io.sample.helper.*
import watson.io.sample.model.MovieModel
import watson.io.sample.ui.BaseViewModel
import watson.io.sample.ui.DataStatus
import watson.io.sample.ui.DiscoverVM
import watson.io.sample.ui.HomeRecyclerViewAdapter

class OnboardingActivity : AppCompatActivity()

abstract class BaseActivity<VM : BaseViewModel> : AppCompatActivity() {

  abstract fun enableLoading(): Boolean
  abstract fun generateViewModel(): VM
  abstract fun getLayoutId(): Int

  protected lateinit var viewModel: VM
  protected var loading: View? = null

  fun addLoading() {
    loading = findViewById(R.id.loading)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(getLayoutId())

    viewModel = generateViewModel()
    if (enableLoading()) {
      addLoading()
      viewModel.loading.observe(this, Observer {
        when (it) {
          true -> loading.show()
          false -> loading.hide()
        }
      })
    }
  }
}

class MainActivity : BaseActivity<DiscoverVM>() {

  override fun getLayoutId() = R.layout.activity_main

  override fun generateViewModel(): DiscoverVM = viewModelProvider()

  override fun enableLoading() = true


  private val data = mutableListOf<MovieModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    addLoading()

    initRecyclerView()
    viewModel.movies.observe(this, Observer {
      when (it.status) {
        DataStatus.SUCCESS -> {
          "size ${it.data?.size} ".log()
          (recyclerView.adapter as HomeRecyclerViewAdapter).data = it.data!!
        }
        DataStatus.ERROR -> {
          it.error?.message.toast(this@MainActivity)
        }
        else -> {
        }
      }
    })
    viewModel.fetchDiscover()
  }

  private fun initRecyclerView() {
    recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    recyclerView.adapter = HomeRecyclerViewAdapter(this, data) { item, position ->
      "click $position ".log()
      toMovieDetail(item.id.toString())
    }
  }

  private val recyclerView: RecyclerView by lazy {
    findViewById<RecyclerView>(R.id.recyclerView)
  }

}