package watson.io.sample.helper

import android.app.Activity
import android.content.Intent
import watson.io.sample.MovieDetailActivity

fun Activity.toMovieDetail(id: String) {
  this.startActivity(
    Intent(this, MovieDetailActivity::class.java)
      .apply {
        putExtra("id", id)
      })
}