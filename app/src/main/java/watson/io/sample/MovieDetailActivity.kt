package watson.io.sample;

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.Observer
import watson.io.sample.helper.log
import watson.io.sample.helper.toast
import watson.io.sample.helper.viewModelProvider
import watson.io.sample.model.MovieDetailModel
import watson.io.sample.ui.DataStatus
import watson.io.sample.ui.MovieDetailVM

class MovieDetailActivity : BaseActivity<MovieDetailVM>() {

  override fun generateViewModel(): MovieDetailVM = viewModelProvider()

  override fun getLayoutId() = R.layout.activity_movie_detail

  override fun enableLoading(): Boolean = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val id = intent.getStringExtra("id")
    "id -> $id".log()

    initView()
    viewModel.data.observe(this, Observer {
      when (it.status) {
        DataStatus.SUCCESS -> {
          val item = (it.data as MovieDetailModel)
          showData(item)
        }
        DataStatus.ERROR -> {
          it.error?.message.toast(this@MovieDetailActivity)
        }
        else -> {
          "else".log()
        }
      }
    })
    id?.let {
      viewModel.fetchMovieDetail(id)
    }
  }

  private val title: TextView by lazy {
    findViewById<TextView>(R.id.title)
  }

  private val overview: TextView by lazy {
    findViewById<TextView>(R.id.overview)
  }

  private fun initView() {
  }

  @SuppressLint("SetTextI18n")
  private fun showData(item: MovieDetailModel) {
    title.text = "${item.title} - ${item.originalTitle}"
    overview.text = item.overview
  }
}
