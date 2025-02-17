@file:Suppress("unused")
@file:JvmName("BaseExtensionUtil")
@file:OptIn(ExperimentalContracts::class)

package io.goooler.demoapp.base.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.text.toSpannable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.io.File
import java.io.Serializable
import java.math.BigDecimal
import java.net.URLConnection
import java.util.Collections
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

// ---------------------Types-------------------------------//

typealias BooleanLiveData = LiveData<Boolean>

typealias IntLiveData = LiveData<Int>

typealias LongLiveData = LiveData<Long>

typealias DoubleLiveData = LiveData<Double>

typealias FloatLiveData = LiveData<Float>

typealias StringLiveData = LiveData<String>

typealias ListLiveData<T> = LiveData<List<T>>

typealias MutableBooleanLiveData = MutableLiveData<Boolean>

typealias MutableIntLiveData = MutableLiveData<Int>

typealias MutableLongLiveData = MutableLiveData<Long>

typealias MutableDoubleLiveData = MutableLiveData<Double>

typealias MutableFloatLiveData = MutableLiveData<Float>

typealias MutableStringLiveData = MutableLiveData<String?>

typealias MutableListLiveData<T> = MutableLiveData<List<T>>

typealias ParamMap = HashMap<String, Any>

// ---------------------Any-------------------------------//

inline val randomUUID: String get() = UUID.randomUUID().toString()

inline val currentTimeMillis: Long get() = System.currentTimeMillis()

inline val currentThreadName: String get() = Thread.currentThread().name

inline val isMainThread: Boolean get() = Looper.getMainLooper() == Looper.myLooper()

fun <T : Any> unsafeLazy(initializer: () -> T): Lazy<T> =
  lazy(LazyThreadSafetyMode.NONE, initializer)

fun <T> MutableLiveData<T>.asLiveData(): LiveData<T> = this

// ---------------------CharSequence-------------------------------//

operator fun String.times(@IntRange(from = 0) num: Int): String {
  require(num >= 0) {
    "Param num should >= 0"
  }
  val origin = this
  return buildString {
    for (i in 1..num) append(origin)
  }
}

fun String.fromHtml(): Spanned = parseAsHtml()

fun String.toMimeType(): String = URLConnection.getFileNameMap().getContentTypeFor(this) ?: this

fun String.onlyDigits(): String = replace(Regex("\\D*"), "")

fun String.removeAllSpecialCharacters(): String = replace(Regex("[^a-zA-Z]+"), "")

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isNotNullOrEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullOrEmpty != null)
  }
  return this.isNullOrEmpty().not()
}

fun Spannable.withClickableSpan(clickablePart: String, onClickListener: () -> Unit): Spannable {
  val clickableSpan = object : ClickableSpan() {
    override fun onClick(widget: View) = onClickListener()
  }
  setSpan(
    clickableSpan,
    indexOf(clickablePart),
    indexOf(clickablePart) + clickablePart.length,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
  )
  return this
}

fun CharSequence.withColorSpan(coloredPart: String, @ColorInt color: Int): Spannable {
  return toSpannable().also {
    it.setSpan(
      ForegroundColorSpan(color),
      it.length - coloredPart.length,
      it.length,
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
  }
}

/**
 * subString 防越界处理
 */
fun String.safeSubstring(startIndex: Int, endIndex: Int): String {
  val begin = if (startIndex < 0) 0 else startIndex
  val end = if (endIndex > length) length else endIndex
  return substring(begin, end)
}

fun String?.safeToBoolean(default: Boolean = false): Boolean {
  return if (this == null) default else try {
    toBoolean()
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

fun String?.safeToInt(default: Int = 0): Int {
  return if (this == null) default else try {
    toInt()
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

fun String?.safeToLong(default: Long = 0L): Long {
  return if (this == null) default else try {
    toLong()
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

fun String?.safeToFloat(default: Float = 0f): Float {
  return if (this == null) default else try {
    toFloat()
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

fun String?.safeToDouble(default: Double = 0.0): Double {
  return if (this == null) default else try {
    toDouble()
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

@ColorInt
fun String?.safeToColor(@ColorInt default: Int = 0): Int {
  return try {
    Color.parseColor(this)
  } catch (e: Throwable) {
    e.printStackTrace()
    default
  }
}

fun String?.isNetworkUrl(): Boolean = URLUtil.isNetworkUrl(this)

fun String?.isValidUrl(): Boolean = URLUtil.isValidUrl(this)

/**
 * 验证手机号格式是否正确
 */
fun String.isValidPhoneFormat(): Boolean = startsWith("1") && length == 11

/**
 * 隐藏手机号
 */
fun String.hidePhone(): String {
  return replace(Regex("(\\d{3})\\d{4}(\\d{4})"), "$1****$2")
}

// ---------------------Calculate-------------------------------//

infix fun Double.plus(that: Double): Double {
  return (BigDecimal(this.toString()) + BigDecimal(that.toString())).toDouble()
}

infix fun Double.minus(that: Double): Double {
  return (BigDecimal(this.toString()) - BigDecimal(that.toString())).toDouble()
}

infix fun Double.times(that: Double): Double {
  return (BigDecimal(this.toString()) * BigDecimal(that.toString())).toDouble()
}

infix fun Double.div(that: Double): Double {
  return (BigDecimal(this.toString()) / BigDecimal(that.toString())).toDouble()
}

fun Number.isZero(): Boolean {
  return when (this) {
    is Byte, is Short, is Int, is Long -> this == 0
    is Float, is Double -> BigDecimal(this.toString()) == BigDecimal("0.0")
    else -> false
  }
}

fun Number.isNotZero(): Boolean = isZero().not()

fun Int?.orZero(): Int = this ?: 0

fun Int.isInvalid(invalidValue: Int = -1) = this == invalidValue

fun Int.isValid(invalidValue: Int = -1) = isInvalid(invalidValue).not()

fun Long.isInvalid(invalidValue: Long = -1) = this == invalidValue

fun Long.isValid(invalidValue: Long = -1) = isInvalid(invalidValue).not()

fun Boolean?.orTrue(): Boolean = this ?: true

fun Boolean?.orFalse(): Boolean = this ?: false

// ---------------------Collections-------------------------------//

@OptIn(ExperimentalContracts::class)
fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
  contract {
    returns(true) implies (this@isNotNullOrEmpty != null)
  }
  return this.isNullOrEmpty().not()
}

/**
 * 判断集合内是否仅有一个元素
 */
@OptIn(ExperimentalContracts::class)
fun <T> Collection<T>?.isSingle(): Boolean {
  contract {
    returns(true) implies (this@isSingle != null)
  }
  return this != null && size == 1
}

/**
 * 判断集合内是否有多个元素
 * @param minSize 最小为 2
 */
@OptIn(ExperimentalContracts::class)
fun <T> Collection<T>?.isMultiple(@IntRange(from = 2) minSize: Int = 2): Boolean {
  contract {
    returns(true) implies (this@isMultiple != null)
  }
  val min = if (minSize < 2) 2 else minSize
  return this != null && size >= min
}

fun <T> List<T>.safeSubList(fromIndex: Int, toIndex: Int): List<T> {
  val endIndex = if (toIndex > size) size else toIndex
  return subList(fromIndex, endIndex)
}

/**
 * 取集合内第二个元素
 */
fun <T> List<T>.secondOrNull(): T? {
  return if (size < 2) null else this[1]
}

/**
 * 取集合内第三个元素
 */
fun <T> List<T>.thirdOrNull(): T? {
  return if (size < 3) null else this[2]
}

fun <E> List<E>.toUnmodifiableList(): List<E> = Collections.unmodifiableList(this)

fun <T> Set<T>.toUnmodifiableSet(): Set<T> = Collections.unmodifiableSet(this)

fun <K, V> Map<K, V>.toUnmodifiableMap(): Map<K, V> = Collections.unmodifiableMap(this)

fun paramMapOf(vararg pairs: Pair<String, Any>): HashMap<String, Any> =
  HashMap<String, Any>(pairs.size).apply { putAll(pairs) }

// ---------------------Coroutine-------------------------------//

fun <T> CoroutineScope.defaultAsync(
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> T
): Deferred<T> = async(SupervisorJob(), start, block)

suspend fun <T> withIoContext(block: suspend CoroutineScope.() -> T): T =
  withContext(Dispatchers.IO, block)

suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T =
  withContext(Dispatchers.Default, block)

// ---------------------File-------------------------------//

fun File.notExists(): Boolean = exists().not()

// ---------------------View-------------------------------//

// ---------------------Intent-------------------------------//

fun Intent.getStringExtra(name: String, defaultValue: String): String =
  getStringExtra(name) ?: defaultValue

fun Intent.getCharSequenceExtra(name: String, defaultValue: CharSequence): CharSequence =
  getCharSequenceExtra(name) ?: defaultValue

fun <T : Parcelable> Intent.getParcelableExtra(name: String, defaultValue: T): T =
  getParcelableExtra(name) ?: defaultValue

fun <T : Serializable> Intent.getSerializableExtra(
  name: String,
  defaultValue: Serializable
): Serializable = getSerializableExtra(name) ?: defaultValue

// ---------------------Fragment-------------------------------//

fun <T : Fragment> T.putArguments(bundle: Bundle): T {
  arguments = bundle
  return this
}

fun <T : Fragment> T.putArguments(vararg pairs: Pair<String, Any?>): T =
  putArguments(bundleOf(*pairs))

/**
 * @param containerViewId   容器 id
 * @param fragment          要添加的 fragment
 * @param isAddToBackStack  将要添加的 fragment 是否要添加到返回栈，默认不添加
 * @param tag               fragment 的 tag
 */
@MainThread
fun FragmentManager.addFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = false,
  tag: String? = null
) {
  if (fragment.isAdded) return
  commit {
    if (isAddToBackStack) addToBackStack(tag)
    add(containerViewId, fragment, tag)
  }
}

/**
 * @param containerViewId   容器 id
 * @param fragment          要替换的 fragment
 * @param isAddToBackStack  将要替换的 fragment 是否要添加到返回栈，默认添加
 * @param tag               fragment 的 tag
 */
@MainThread
fun FragmentManager.replaceFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = true,
  tag: String? = null
) {
  if (fragment.isAdded) return
  commit {
    if (isAddToBackStack) addToBackStack(tag)
    replace(containerViewId, fragment, tag)
  }
}

@MainThread
fun Fragment.addFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = false,
  tag: String? = null
) {
  childFragmentManager.addFragment(fragment, containerViewId, isAddToBackStack, tag)
}

@MainThread
fun Fragment.replaceFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = false,
  tag: String? = null
) {
  childFragmentManager.addFragment(fragment, containerViewId, isAddToBackStack, tag)
}

inline val View.attachedFragment: Fragment?
  get() = runCatching { findFragment<Fragment>() }.getOrNull()

inline val View.attachedActivity: Activity?
  get() {
    var baseContext: Context? = context
    while (baseContext is ContextWrapper) {
      if (baseContext is Activity) break
      baseContext = baseContext.baseContext
    }
    return baseContext as? Activity
  }

inline val View.lifecycle: Lifecycle? get() = findViewTreeLifecycleOwner()?.lifecycle

inline val View.lifecycleScope: LifecycleCoroutineScope? get() = lifecycle?.coroutineScope

fun View.hideSoftInput() {
  context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showSoftInput() {
  context.getSystemService<InputMethodManager>()?.showSoftInput(this, 0)
}

// ---------------------Context-------------------------------//

fun Context.addDynamicShortcutCompat(id: String, shortcut: ShortcutInfoCompat) {
  if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
    ShortcutManagerCompat.getDynamicShortcuts(this).any { it.id == id }.not()
  ) {
    try {
      ShortcutManagerCompat.addDynamicShortcuts(this, mutableListOf(shortcut))
    } catch (_: Exception) {
    }
  }
}

// ---------------------Activity-------------------------------//

@MainThread
fun FragmentActivity.addFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = false,
  tag: String? = null
) {
  supportFragmentManager.addFragment(fragment, containerViewId, isAddToBackStack, tag)
}

@MainThread
fun FragmentActivity.replaceFragment(
  fragment: Fragment,
  @IdRes containerViewId: Int = android.R.id.content,
  isAddToBackStack: Boolean = false,
  tag: String? = null
) {
  supportFragmentManager.replaceFragment(fragment, containerViewId, isAddToBackStack, tag)
}

@MainThread
inline fun <reified T : ViewDataBinding> Activity.binding(@LayoutRes resId: Int): Lazy<T> =
  lazy(LazyThreadSafetyMode.NONE) { DataBindingUtil.setContentView(this, resId) }

// ---------------------Other-------------------------------//

/**
 * 条件为真时执行
 */
inline fun Boolean.trueRun(block: () -> Unit) {
  if (this) block()
}

/**
 * 条件为假时执行
 */
inline fun Boolean.falseRun(block: () -> Unit) {
  if (this.not()) block()
}
