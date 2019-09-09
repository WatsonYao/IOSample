package watson.io.sample.executors

import android.annotation.SuppressLint
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import okhttp3.internal.Util
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


data class AppRxSchedulers(
  val db: Scheduler,
  val bg: Scheduler,
  val io: Scheduler,
  val main: Scheduler
)

data class AppCoroutineDispatchers(
  val db: CoroutineDispatcher,
  val bg: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher
)

object Executor {
  val ioExecutor = ThreadPoolExecutor(
    0,
    Int.MAX_VALUE,
    60,
    TimeUnit.SECONDS,
    SynchronousQueue(), Util.threadFactory("wm", false)
  )

  val schedulers = AppRxSchedulers(
    db = Schedulers.single(),
    bg = Schedulers.computation(),
    io = Schedulers.from(ioExecutor),
    main = AndroidSchedulers.mainThread()
  )

  val dispatchers = AppCoroutineDispatchers(
    db = schedulers.db.asCoroutineDispatcher(),
    bg = schedulers.bg.asCoroutineDispatcher(),
    io = schedulers.io.asCoroutineDispatcher(),
    main = schedulers.main.asCoroutineDispatcher()
  )

  @SuppressLint("CheckResult")
  fun <T> runInBg(
    block: () -> T,
    scheduler: Scheduler = schedulers.bg,
    success: (T) -> Unit = {},
    error: () -> Unit = {}
  ) {
    Single.fromCallable { block() }
      .subscribeOn(scheduler)
      .observeOn(schedulers.main)
      .subscribe({ resp ->
        success(resp)
      }, {
        error()
      })
  }

  fun <T> runInBg(block: () -> T) {
    runInBg(block, schedulers.bg)
  }
}