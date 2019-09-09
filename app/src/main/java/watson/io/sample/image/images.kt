package watson.io.sample.image

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

object ImageStore {
  const val URL_HOST = "https://image.tmdb.org/t/p/w500"
}

fun ImageView?.load(url: String?, context: Context? = null, placeholder: Int = 0, error: Int = 0) {
  if (url != null && this != null) {
    val imageView = this
    val realContext = context ?: imageView.context
    val realPlaceholder = if (placeholder == 0) imageView.drawable else realContext.resources.getDrawable(placeholder)
    val realError = if (error == 0) imageView.drawable else realContext.resources.getDrawable(error)
    Glide.with(realContext)
      .load(ImageStore.URL_HOST + url)
      .apply(
        RequestOptions()
          .placeholder(realPlaceholder)
          .error(realError)
          .fitCenter()
      )
      .into(imageView)
  }
}

fun ImageView?.eayLoad(context: Context, url: String) {
  this?.let {
    Glide.with(context)
      .load(ImageStore.URL_HOST + url)
      .apply(RequestOptions().centerCrop())
      .into(this)
  }
}
