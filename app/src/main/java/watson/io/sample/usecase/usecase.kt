package watson.io.sample.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import watson.io.sample.model.PreferenceStorage
import watson.io.sample.repo.MovieRepo

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Result<out R> {

  data class Success<out T>(val data: T) : Result<T>()
  data class Error(val exception: Exception) : Result<Nothing>()
  object Loading : Result<Nothing>()

  override fun toString(): String {
    return when (this) {
      is Success<*> -> "Success[data=$data]"
      is Error -> "Error[exception=$exception]"
      Loading -> "Loading"
    }
  }
}

/**
 * `true` if [Result] is of type [Success] & holds non-null [Success.data].
 */
val Result<*>.succeeded
  get() = this is Result.Success && data != null

/**
 * Executes business logic synchronously or asynchronously using a [Scheduler].
 */
abstract class UseCase<in P, R> {

  private val taskScheduler = DefaultScheduler

  /** Executes the use case asynchronously and places the [Result] in a MutableLiveData
   *
   * @param parameters the input parameters to run the use case with
   * @param result the MutableLiveData where the result is posted to
   *
   */
  operator fun invoke(parameters: P, result: MutableLiveData<Result<R>>) {
    // result.value = Result.Loading TODO: add data to Loading to avoid glitches
    try {
      taskScheduler.execute {
        try {
          execute(parameters).let { useCaseResult ->
            result.postValue(Result.Success(useCaseResult))
          }
        } catch (e: Exception) {
          result.postValue(Result.Error(e))
        }
      }
    } catch (e: Exception) {
      result.postValue(Result.Error(e))
    }
  }

  /** Executes the use case asynchronously and returns a [Result] in a new LiveData object.
   *
   * @return an observable [LiveData] with a [Result].
   *
   * @param parameters the input parameters to run the use case with
   */
  operator fun invoke(parameters: P): LiveData<Result<R>> {
    val liveCallback: MutableLiveData<Result<R>> = MutableLiveData()
    this(parameters, liveCallback)
    return liveCallback
  }

  /** Executes the use case synchronously  */
  fun executeNow(parameters: P): Result<R> {
    return try {
      Result.Success(execute(parameters))
    } catch (e: Exception) {
      Result.Error(e)
    }
  }

  /**
   * Override this to set the code to be executed.
   */
  @Throws(RuntimeException::class)
  protected abstract fun execute(parameters: P): R
}

operator fun <R> UseCase<Unit, R>.invoke(): LiveData<Result<R>> = this(Unit)
operator fun <R> UseCase<Unit, R>.invoke(result: MutableLiveData<Result<R>>) = this(Unit, result)

open class Event<out T>(private val content: T) {

  var hasBeenHandled = false
    private set // Allow external read but not write

  /**
   * Returns the content and prevents its use again.
   */
  fun getContentIfNotHandled(): T? {
    return if (hasBeenHandled) {
      null
    } else {
      hasBeenHandled = true
      content
    }
  }

  /**
   * Returns the content, even if it's already been handled.
   */
  fun peekContent(): T = content
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
  override fun onChanged(event: Event<T>?) {
    event?.getContentIfNotHandled()?.let { value ->
      onEventUnhandledContent(value)
    }
  }
}

open class OnboardingCompletedUseCase(private val preferenceStorage: PreferenceStorage) : UseCase<Unit, Boolean>() {
  override fun execute(parameters: Unit): Boolean = preferenceStorage.onboardingCompleted
}

class MovieUsecase {
  suspend fun exe() = MovieRepo.discover()
}

class MovieDetailUsecase {
  suspend fun exe(id: String) = MovieRepo.movieDetail(id)
}