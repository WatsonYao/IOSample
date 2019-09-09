/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package watson.io.sample.helper

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.threeten.bp.ZonedDateTime
import watson.io.sample.BuildConfig

/**
 * Implementation of lazy that is not thread safe. Useful when you know what thread you will be
 * executing on and are not worried about synchronization.
 */
fun <T> lazyFast(operation: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
  operation()
}

/** Convenience for callbacks/listeners whose return value indicates an event was consumed. */
inline fun consume(f: () -> Unit): Boolean {
  f()
  return true
}

/**
 * Allows calls like
 *
 * `viewGroup.inflate(R.layout.foo)`
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
  return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}

/**
 * Allows calls like
 *
 * `supportFragmentManager.inTransaction { add(...) }`
 */
inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
  beginTransaction().func().commit()
}

// region ViewModels

/**
 * For Actvities, allows declarations like
 * ```
 * val myViewModel = viewModelProvider(myViewModelFactory)
 * ```
 */
inline fun <reified VM : ViewModel> FragmentActivity.viewModelProvider(provider: ViewModelProvider.Factory) =
  ViewModelProviders.of(this, provider).get(VM::class.java)

inline fun <reified VM : ViewModel> FragmentActivity.viewModelProvider() =
  ViewModelProviders.of(this).get(VM::class.java)


/**
 * For Fragments, allows declarations like
 * ```
 * val myViewModel = viewModelProvider(myViewModelFactory)
 * ```
 */
inline fun <reified VM : ViewModel> Fragment.viewModelProvider(
  provider: ViewModelProvider.Factory
) =
  ViewModelProviders.of(this, provider).get(VM::class.java)

/**
 * Like [Fragment.viewModelProvider] for Fragments that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> Fragment.activityViewModelProvider(
  provider: ViewModelProvider.Factory
) =
  ViewModelProviders.of(requireActivity(), provider).get(VM::class.java)

/**
 * Like [Fragment.viewModelProvider] for Fragments that want a [ViewModel] scoped to the parent
 * Fragment.
 */
inline fun <reified VM : ViewModel> Fragment.parentViewModelProvider(
  provider: ViewModelProvider.Factory
) =
  ViewModelProviders.of(parentFragment!!, provider).get(VM::class.java)

// endregion
// region Parcelables, Bundles

/** Write an enum value to a Parcel */
fun <T : Enum<T>> Parcel.writeEnum(value: T) = writeString(value.name)

/** Read an enum value from a Parcel */
//inline fun <reified T : Enum<T>> Parcel.readEnum(): T = enumValueOf(readString())

/** Write an enum value to a Bundle */
fun <T : Enum<T>> Bundle.putEnum(key: String, value: T) = putString(key, value.name)

/** Read an enum value from a Bundle */
//inline fun <reified T : Enum<T>> Bundle.getEnum(key: String): T = enumValueOf(getString(key))

/** Write a boolean to a Parcel (copied from Parcel, where this is @hidden). */
fun Parcel.writeBoolean(value: Boolean) = writeInt(if (value) 1 else 0)

/** Read a boolean from a Parcel (copied from Parcel, where this is @hidden). */
fun Parcel.readBoolean() = readInt() != 0

// endregion
// region LiveData

/** Uses `Transformations.map` on a LiveData */
fun <X, Y> LiveData<X>.map(body: (X) -> Y): LiveData<Y> {
  return Transformations.map(this, body)
}

/** Uses `Transformations.switchMap` on a LiveData */
fun <X, Y> LiveData<X>.switchMap(body: (X) -> LiveData<Y>): LiveData<Y> {
  return Transformations.switchMap(this, body)
}

fun <T> MutableLiveData<T>.setValueIfNew(newValue: T) {
  if (this.value != newValue) value = newValue
}

fun <T> MutableLiveData<T>.postValueIfNew(newValue: T) {
  if (this.value != newValue) postValue(newValue)
}
// endregion

// region ZonedDateTime
fun ZonedDateTime.toEpochMilli() = this.toInstant().toEpochMilli()
// endregion

/**
 * Helper to force a when statement to assert all options are matched in a when statement.
 *
 * By default, Kotlin doesn't care if all branches are handled in a when statement. However, if you
 * use the when statement as an expression (with a value) it will force all cases to be handled.
 *
 * This helper is to make a lightweight way to say you meant to match all of them.
 *
 * Usage:
 *
 * ```
 * when(sealedObject) {
 *     is OneType -> //
 *     is AnotherType -> //
 * }.checkAllMatched
 */
val <T> T.checkAllMatched: T
  get() = this

// region UI utils

/**
 * Retrieves a color from the theme by attributes. If the attribute is not defined, a fall back
 * color will be returned.
 */
@ColorInt
fun Context.getThemeColor(
  @AttrRes attrResId: Int,
  @ColorRes fallbackColorResId: Int
): Int {
  val tv = TypedValue()
  return if (theme.resolveAttribute(attrResId, tv, true)) {
    tv.data
  } else {
    ContextCompat.getColor(this, fallbackColorResId)
  }
}

// endregion

/**
 * Helper to throw exceptions only in Debug builds, logging a warning otherwise.
 */
fun exceptionInDebug(t: Throwable) {
  if (BuildConfig.DEBUG) {
    throw t
  } else {
    //Timber.e(t)
  }
}

fun String.log() = Log.i("temp", this)
fun String.loge() = Log.e("temp", this)

object GsonHelper {
  val gson: Gson = GsonBuilder().create()
}

object JsonUtil {

  fun formatJson(jsonStr: String?): String {
    if (null == jsonStr || "" == jsonStr) return ""
    val sb = StringBuilder()
    var last: Char
    var current = '\u0000'
    var indent = 0
    for (i in 0 until jsonStr.length) {
      last = current
      current = jsonStr[i]
      // 换行，且下一行缩进
      when (current) {
        '{', '[' -> {
          sb.append(current)
          sb.append('\n')
          indent++
          addIndentBlank(sb, indent)
        }
        // 换行，当前行缩进
        '}', ']' -> {
          sb.append('\n')
          indent--
          addIndentBlank(sb, indent)
          sb.append(current)
        }
        // 换行
        ',' -> {
          sb.append(current)
          if (last != '\\') {
            sb.append('\n')
            addIndentBlank(sb, indent)
          }
        }
        else -> sb.append(current)
      }
    }
    return sb.toString()
  }

  private fun addIndentBlank(sb: StringBuilder, indent: Int) {
    for (i in 0 until indent) {
      sb.append('\t')
    }
  }

  fun decodeUnicode(theString: String): String {
    var aChar: Char
    val len = theString.length
    val outBuffer = StringBuffer(len)
    var x = 0
    while (x < len) {
      aChar = theString[x++]
      if (aChar == '\\') {
        aChar = theString[x++]
        if (aChar == 'u') {
          var value = 0
          for (i in 0..3) {
            aChar = theString[x++]
            when (aChar) {
              '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> value = (value shl 4) + aChar.toInt() - '0'.toInt()
              'a', 'b', 'c', 'd', 'e', 'f' -> value = (value shl 4) + 10 + aChar.toInt() - 'a'.toInt()
              'A', 'B', 'C', 'D', 'E', 'F' -> value = (value shl 4) + 10 + aChar.toInt() - 'A'.toInt()
              else -> throw IllegalArgumentException(
                "Malformed   \\uxxxx   encoding."
              )
            }

          }
          outBuffer.append(value.toChar())
        } else {
          when (aChar) {
            't' -> aChar = '\t'
            'r' -> aChar = '\r'
            'n' -> aChar = '\n'
            'f' -> aChar = '\u000C'
          }
          outBuffer.append(aChar)
        }
      } else
        outBuffer.append(aChar)
    }
    return outBuffer.toString()
  }
}

fun View?.show() {
  this?.let {
    if (this.visibility != View.VISIBLE) {
      this.visibility = View.VISIBLE
    }
  }
}

fun View?.hide() {
  this?.let {
    if (this.visibility != View.INVISIBLE) {
      this.visibility = View.INVISIBLE
    }
  }
}

fun String?.toast(context: Context) {
  this?.let {
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
  }
}