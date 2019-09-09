package watson.io.sample.ui

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.rx2.rxSingle
import watson.io.sample.exception.AccessThrowable
import watson.io.sample.exception.ERROR
import watson.io.sample.executors.Executor.dispatchers
import watson.io.sample.helper.loge

open class BaseViewModel : ViewModel() {

  protected val _loading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
  val loading: LiveData<Boolean>
    get() = _loading

  protected val _toastError: MutableLiveData<String> by lazy { MutableLiveData<String>() }
  val toastError: LiveData<String>
    get() = _toastError

  protected val _toastOK: MutableLiveData<String> by lazy { MutableLiveData<String>() }
  val toastOK: LiveData<String>
    get() = _toastOK

  private val _reLogin: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
  val reLogin: LiveData<Boolean>
    get() = _reLogin

  private val _networkError: MutableLiveData<AccessThrowable> by lazy { MutableLiveData<AccessThrowable>() }
  val networkError: LiveData<AccessThrowable>
    get() = _networkError

  private val viewModelJob = SupervisorJob()
  private var disposable: Disposable? = null

  protected val uiScope = CoroutineScope(
    dispatchers.main
        + viewModelJob
        + CoroutineExceptionHandler { _, exception ->
      "caught original $exception".loge()
    })

  override fun onCleared() {
    uiScope.coroutineContext.cancelChildren()
    disposable?.dispose()
    super.onCleared()
  }

  fun <T> load(loader: suspend () -> T): Deferred<T> {
    return uiScope.async(dispatchers.io) {
      loader()
    }
  }

  fun <T : Any> loadAsync(
    showLoading: Boolean = true,
    block: suspend CoroutineScope.() -> T
  ): Single<T> {
    if (showLoading) {
      _loading.value = true
    }
    return rxSingle(block = block)
  }

  fun <T : Any, R : Any> Single<T>.then(block: suspend CoroutineScope.(T) -> R): Single<R> {
    return flatMap {
      rxSingle { block(it) }
    }
  }

  fun <T> Deferred<T>.result(): Observable<T> {
    return Observable.create<T> { emitter ->
      result({
        emitter.onNext(it)
      }, {
        emitter.onError(it)
      })
    }
  }

  infix fun <T> Deferred<T>.result(success: (T) -> Unit) {
    return result(success, { _toastError.value = it.message })
  }

  fun <T> Deferred<T>.result(
    success: (T) -> Unit,
    error: (AccessThrowable) -> Unit = { _toastError.value = it.message },
    complete: () -> Unit = {},
    showLoading: Boolean = true,
    escapeHideSpinner: Boolean = false
  ) {
    if (isActive) uiScope.launch {
      try {
        if (showLoading) {
          _loading.value = true
        }
        success(this@result.await())
      } catch (err: AccessThrowable) {
        when (err.code) {
          ERROR.ERROR_NETWORK -> {
            _networkError.postValue(err)
          }
          ERROR.ERROR_TOKEN_EXPIRED -> {
            _reLogin.postValue(true)
          }
        }
        error(err)
      } catch (err: Exception) {
        error(AccessThrowable(ERROR.UNKNOWN, err.message ?: ""))
      } finally {
        if (showLoading && !escapeHideSpinner) {
          _loading.value = false
        }
        complete.invoke()
      }
    }
  }
}

enum class DataStatus {
  CREATED,
  SUCCESS,
  ERROR,
  LOADING,
  COMPLETE
}

class DataState<T> {

  @NonNull
  @get:NonNull
  var status: DataStatus? = null
    private set

  @Nullable
  @get:Nullable
  var data: T? = null
    private set

  @Nullable
  @get:Nullable
  var error: Throwable? = null
    private set

  init {
    this.status = DataStatus.CREATED
    this.data = null
    this.error = null
  }

  fun loading(): DataState<T> {
    this.status = DataStatus.LOADING
    this.data = null
    this.error = null
    return this
  }

  fun success(@NonNull data: T): DataState<T> {
    this.status = DataStatus.SUCCESS
    this.data = data
    this.error = null
    return this
  }

  fun error(@NonNull error: Throwable): DataState<T> {
    this.status = DataStatus.ERROR
    this.data = null
    this.error = error
    return this
  }

  fun complete(): DataState<T> {
    this.status = DataStatus.COMPLETE
    return this
  }
}

open class StateLiveData<T> : LiveData<DataState<T>>() {

  /**
   * Use this to put the Data on a LOADING Status
   */
  fun postLoading() {
    postValue(DataState<T>().loading())
  }

  /**
   * Use this to put the Data on a ERROR DataStatus
   * @param throwable the error to be handled
   */
  fun postError(throwable: Throwable) {
    postValue(DataState<T>().error(throwable))
  }

  /**
   * Use this to put the Data on a SUCCESS DataStatus
   * @param data
   */
  fun postSuccess(data: T) {
    postValue(DataState<T>().success(data))
  }

  /**
   * Use this to put the Data on a COMPLETE DataStatus
   */
  fun postComplete() {
    postValue(DataState<T>().complete())
  }
}