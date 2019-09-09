package watson.io.sample.repo

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import watson.io.sample.KeyStore
import watson.io.sample.exception.ServerException
import watson.io.sample.exception.withDetailContext
import watson.io.sample.executors.Executor
import watson.io.sample.helper.GsonHelper
import watson.io.sample.helper.JsonUtil
import watson.io.sample.model.*
import watson.io.sample.model.ModelMapper.from
import java.io.IOException
import java.io.Serializable
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit


class BaseResponse<T> : Serializable {
  var page: Int = 0
  var total_result: Int = 0
  var total_pages: Int = 0
  var status_code: Int = 0
  var results: T? = null
  var status_message: String? = null


  val isOK: Boolean
    get() = status_code == 0

  val emptyData: Boolean
    get() = results == null
}


object BaseURL {
  const val root = "https://api.themoviedb.org/"
}


interface IMovieRepo {
  suspend fun discover(): List<MovieModel>
  suspend fun movieDetail(id: String): MovieDetailModel
}

object MovieRepo : IMovieRepo {
  override suspend fun discover(): List<MovieModel> {
    try {
      return RetrofitClient.movieApi.discover().asSequence().map(ModelMapper::from).toList()
    } catch (error: Throwable) {
      error.printStackTrace()
      throw error.withDetailContext()
    }
  }

  override suspend fun movieDetail(id: String): MovieDetailModel {
    try {
      return from(RetrofitClient.movieApi.movieDetail(id))
    } catch (error: Throwable) {
      error.printStackTrace()
      throw error.withDetailContext()
    }
  }
}

interface RemoteMovieApi {

  @GET("/3/discover/movie?sort_by=popularity.desc&language=zh-CN")
  suspend fun discover(
    @Query("api_key") key: String = KeyStore.getKey(),
    @Query("language") language: String = KeyStore.language
  ): List<MovieDTO>

  @NoWrapJson
  @GET("/3/movie/{movieId}")
  suspend fun movieDetail(
    @Path("movieId") id: String,
    @Query("api_key") key: String = KeyStore.getKey(),
    @Query("language") language: String = KeyStore.language
  ): MovieDetailDTO
}

class HttpLogger : HttpLoggingInterceptor.Logger {
  companion object {
    val ignoreHost = mutableListOf<String>().apply {
      add("https://www.google.com/")
    }
  }

  private val sb = StringBuffer()

  @Synchronized
  override fun log(message: String) {
    var msg = message
    if (message.startsWith("--> POST") || message.startsWith("--> GET")) {
      sb.setLength(0)
    }
    if (message.startsWith("{") && message.endsWith("}")
      || message.startsWith("[") && message.endsWith("]")
    ) {
      msg = JsonUtil.formatJson(JsonUtil.decodeUnicode(message))
    }
    if (notIgnore(sb)) {
      sb.append(msg + "\n")
      if (message.startsWith("<-- END HTTP") || message.startsWith("<-- HTTP FAILED")) {
        // d华为的有些机型打印不错，需要e
        Log.d("temp", sb.toString())
      }
    }
  }

  private fun notIgnore(sb: StringBuffer): Boolean {
    for (s in ignoreHost) {
      if (sb.contains(s)) {
        return false
      }
    }
    return true
  }
}

object OkHttpHelper {
  val okHttpClient: OkHttpClient by lazy {
    val interceptor = HttpLoggingInterceptor(HttpLogger())
    interceptor.level = HttpLoggingInterceptor.Level.BODY
    val builder = OkHttpClient.Builder()
      .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
      .dispatcher(Dispatcher(Executor.ioExecutor))
    builder.addNetworkInterceptor(interceptor)
    builder.build()
  }
}

object RetrofitClient {

  val movieApi by lazy {
    RetrofitFactory.create(GsonHelper.gson, OkHttpHelper.okHttpClient, BaseURL.root)
      .create(RemoteMovieApi::class.java)
  }
}

object RetrofitFactory {
  fun create(gson: Gson, okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
    return Retrofit.Builder()
      .addConverterFactory(WrapperConverterFactory(gson))
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .build()
  }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoWrapJson

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WrapJson


class WrapperConverterFactory(val gson: Gson) : Converter.Factory() {

  private val gsonConverterFactory = GsonConverterFactory.create(gson)

  override fun responseBodyConverter(
    type: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit
  ): Converter<ResponseBody, *>? {
    annotations.forEach {
      if (it.annotationClass == NoWrapJson::class) {
        return GsonResponseBodyConverter(gson, gson.getAdapter(TypeToken.get(type)))
      }
      if (it.annotationClass == WrapJson::class) {
        return getWrapperResponseBodyConverter(type, annotations, retrofit)
      }
    }
    return getWrapperResponseBodyConverter(type, annotations, retrofit)
  }

  @Suppress("UNCHECKED_CAST")
  private fun getWrapperResponseBodyConverter(
    type: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit
  ): WrapperResponseBodyConverter<*> {
    val wrappedType = object : ParameterizedType {
      override fun getActualTypeArguments(): Array<Type> = arrayOf(type)
      override fun getOwnerType(): Type? = null
      override fun getRawType(): Type = BaseResponse::class.java
    }
    val gsonConverter: Converter<ResponseBody, *>? = gsonConverterFactory.responseBodyConverter(wrappedType, annotations, retrofit)
    return WrapperResponseBodyConverter(gsonConverter as Converter<ResponseBody, BaseResponse<Any>>)
  }

  override fun requestBodyConverter(
    type: Type?, parameterAnnotations: Array<Annotation>,
    methodAnnotations: Array<Annotation>, retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    return gsonConverterFactory.requestBodyConverter(type!!, parameterAnnotations, methodAnnotations, retrofit)
  }
}

class WrapperResponseBodyConverter<T>(
  private val converter: Converter<ResponseBody, BaseResponse<T>>
) : Converter<ResponseBody, T> {

  @Throws(IOException::class)
  override fun convert(responseBody: ResponseBody): T {
    val response = converter.convert(responseBody)
    return if (response?.isOK == true) response.results!!
    else throw ServerException(response!!.status_code, response.status_message ?: "")
  }
}

class GsonResponseBodyConverter<T>(
  private val gson: Gson,
  private val adapter: TypeAdapter<T>
) : Converter<ResponseBody, T> {

  @Throws(IOException::class)
  override fun convert(value: ResponseBody): T {
    val jsonReader = gson.newJsonReader(value.charStream())
    value.use {
      val result = adapter.read(jsonReader)
      if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw JsonIOException("JSON document was not fully consumed.")
      }
      return result
    }
  }
}