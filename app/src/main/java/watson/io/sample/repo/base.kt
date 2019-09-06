package watson.io.sample.repo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Query
import watson.io.sample.KeyStore


data class NetResult<T>(
  val page: Int,
  val total_result: Int,
  val total_pages: Int,
  val results: List<T>
)

data class Movie(val id: Long, val title: String, val vote_average: String)

abstract class BaseRetrofitClient {

  companion object {
    const val BASE_URL = "https://api.themoviedb.org/"
  }

  fun <S> getService(serviceClass: Class<S>, baseUrl: String = BASE_URL): S {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    val httpClient = OkHttpClient.Builder()
    httpClient.addInterceptor(logging)  // <-- this is the important line!
    return Retrofit.Builder()
      .client(httpClient.build())
      .addConverterFactory(GsonConverterFactory.create())
      .baseUrl(baseUrl)
      .build().create(serviceClass)
  }
}

interface RemoteServer {

  @GET("/3/discover/movie?sort_by=popularity.desc")
  suspend fun discover(@Query("api_key") key: String = KeyStore.getKey()): NetResult<Movie>
}

object RetrofitClient : BaseRetrofitClient() {
  val service by lazy {
    getService(RemoteServer::class.java)
  }
}