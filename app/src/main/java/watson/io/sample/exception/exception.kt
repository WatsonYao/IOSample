package watson.io.sample.exception

import android.database.sqlite.SQLiteException
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.ParseException
import javax.net.ssl.SSLHandshakeException

data class ServerException(
  val code: Int,
  override val message: String
) : RuntimeException(message)

data class AccessThrowable(
  val code: Int,
  override val message: String
) : Throwable(message)


fun Throwable.withDetailContext() = when (this) {
  is HttpException -> AccessThrowable(
    ERROR.ERROR_HTTP, when (this.code()) {
      ExceptionTransformer.UNAUTHORIZED, ExceptionTransformer.FORBIDDEN,
      ExceptionTransformer.NOT_FOUND, ExceptionTransformer.REQUEST_TIMEOUT,
      ExceptionTransformer.GATEWAY_TIMEOUT, ExceptionTransformer.INTERNAL_SERVER_ERROR,
      ExceptionTransformer.BAD_GATEWAY, ExceptionTransformer.SERVICE_UNAVAILABLE -> "网络异常,请检查网络设置"
      else -> "网络错误"
    }
  )
  is ServerException -> AccessThrowable(this.code, this.message)

  is JsonParseException,
  is JSONException,
  is ParseException,
  is JsonIOException -> AccessThrowable(ERROR.ERROR_PARSE, "解析错误")

  is ConnectException,
  is SocketTimeoutException,
  is UnknownHostException -> AccessThrowable(ERROR.ERROR_NETWORK, "网络不给力")

  is SSLHandshakeException -> AccessThrowable(ERROR.ERROR_SSL, "证书验证失败")

  is SQLiteException -> AccessThrowable(ERROR.ERROR_SQL, "数据库出错")
  else -> {
    AccessThrowable(ERROR.UNKNOWN, "未知错误")
  }
}

object ExceptionTransformer {
  const val UNAUTHORIZED = 401
  const val FORBIDDEN = 403
  const val NOT_FOUND = 404
  const val REQUEST_TIMEOUT = 408
  const val INTERNAL_SERVER_ERROR = 500
  const val BAD_GATEWAY = 502
  const val SERVICE_UNAVAILABLE = 503
  const val GATEWAY_TIMEOUT = 504
}

/**
 * 约定异常
 */
object ERROR {
  /**
   * 未知错误
   */
  const val UNKNOWN = 1000
  /**
   * 解析错误
   */
  const val ERROR_PARSE = 1001
  /**
   * 网络错误
   */
  const val ERROR_NETWORK = 1002
  /**
   * 协议出错
   */
  const val ERROR_HTTP = 1003
  /**
   * 证书出错
   */
  const val ERROR_SSL = 1005
  /**
   * 数据库出错
   */
  const val ERROR_SQL = 1006

  /**
   * token过期
   */
  const val ERROR_TOKEN_EXPIRED = 11

  /**
   * app需升级
   */
  const val ERROR_NEED_UPDATE = 12
}