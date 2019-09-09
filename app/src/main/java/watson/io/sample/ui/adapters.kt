package watson.io.sample.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import watson.io.sample.R
import watson.io.sample.image.eayLoad
import watson.io.sample.model.MovieModel

class HomeRecyclerViewAdapter(
  private val context: Context,
  data: List<MovieModel>,
  private val listener: (MovieModel, Int) -> Unit
) : RecyclerView.Adapter<MovieViewHolder>() {

  override fun getItemCount(): Int {
    return data.size
  }

  var data: List<MovieModel> = data
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
    val item = data[position]
    val itemContext = holder.itemView.context
    holder.itemView.setOnClickListener {
      listener(item, position)
    }
    with(item) {
      holder.title.text = title
      holder.releaseDate.text = releaseDate
      holder.posterPath.eayLoad(itemContext, posterPath)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
    return MovieViewHolder(
      LayoutInflater.from(context)
        .inflate(R.layout.rv_item_movie, parent, false)
    )
  }
}


class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  var title: TextView = itemView.findViewById(R.id.title)
  var releaseDate: TextView = itemView.findViewById(R.id.releaseDate)
  var posterPath: ImageView = itemView.findViewById(R.id.posterPath)
}