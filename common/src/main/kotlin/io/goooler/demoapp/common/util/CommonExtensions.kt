@file:Suppress("unused")
@file:JvmName("CommonExtensionUtil")

package io.goooler.demoapp.common.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.annotation.AnyThread
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.PluralsRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.AdaptScreenUtils
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.TimeUtils
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import io.goooler.demoapp.base.util.ToastUtil
import io.goooler.demoapp.common.BuildConfig
import io.goooler.demoapp.common.CommonApplication
import io.goooler.demoapp.common.base.theme.BaseThemeViewModel
import io.goooler.demoapp.common.base.theme.ITheme
import io.goooler.demoapp.common.type.SpKeys
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

typealias DimensionUtil = SizeUtils
typealias SpHelper = SPUtils

val isDebug: Boolean = BuildConfig.DEBUG

inline var isFirstRun: Boolean
  get() = SpHelper.getInstance().getBoolean(SpKeys.FirstRun.key, true)
  set(value) = SpHelper.getInstance().put(SpKeys.FirstRun.key, value)

inline fun debugRun(debug: () -> Unit) {
  if (isDebug) debug()
}

fun Throwable.toast() {
  toString().showToast()
}

@AnyThread
fun @receiver:StringRes Int.showToast() {
  ToastUtil.show(CommonApplication.app, this)
}

@AnyThread
fun String.showToast() {
  ToastUtil.show(CommonApplication.app, this)
}

@MainThread
fun SmartRefreshLayout.finishRefreshAndLoadMore() {
  finishRefresh()
  finishLoadMore()
}

@MainThread
fun SmartRefreshLayout.enableRefreshAndLoadMore(enable: Boolean = true) {
  setEnableRefresh(enable)
  setEnableLoadMore(enable)
}

@MainThread
fun SmartRefreshLayout.disableRefreshAndLoadMore() {
  enableRefreshAndLoadMore(false)
}

// ---------------------Convert-------------------------------//

/**
 * 拼上图片前缀
 */
fun String.toLoadUrl(): String {
  return if (URLUtil.isNetworkUrl(this)) this else BuildConfig.CDN_PREFIX + this
}

fun Long.toDateString(pattern: String): String = TimeUtils.millis2String(this, pattern)

fun Long.easyTime(): String {
  val now = System.currentTimeMillis()
  val t = now - this
  if (t < 0) {
    // 未来
    return toDateString("yyyy-MM-dd HH:mm")
  }
  val oneMinute = 1000 * 60
  val oneHour = oneMinute * 60
  val oneDay = oneHour * 24
  val c1 = Calendar.getInstance()
  val c2 = Calendar.getInstance()
  c1.time = Date(this)
  c2.time = Date(now)
  val day1 = c1.get(Calendar.DAY_OF_WEEK)
  val day2 = c2.get(Calendar.DAY_OF_WEEK)
  val isYesterday = t < oneDay * 2 && (day2 - day1 == 1 || day2 - day1 == -6)

  val year1 = c1.get(Calendar.YEAR)
  val year2 = c2.get(Calendar.YEAR)

  val isSameYear = year1 == year2

  return when {
    isSameYear.not() -> toDateString("yyyy-MM-dd HH:mm")
    isYesterday -> toDateString("昨天 HH:mm")
    t < oneMinute -> "刚刚"
    t < oneHour -> (t / oneMinute).toString() + "分钟前"
    t < oneDay -> (t / oneHour).toString() + "小时前"
    isSameYear -> toDateString("MM-dd HH:mm")
    else -> toDateString("yyyy-MM-dd HH:mm")
  }
}

/**
 * @param isYuan 默认以分为单位，传入元为单位传 true
 * @param trans2W 是否需要在超过一万时转换为 1.2w 的形式，不需要的话传 false
 *
 * 分是 Long 类型、元是 Double 类型
 */
fun Number.formatMoney(isYuan: Boolean = false, trans2W: Boolean = false, scale: Int = 2): String {
  val moneyF = if (isYuan) {
    toDouble()
  } else {
    // 分转为元
    toDouble() / 100
  }
  return try {
    when {
      trans2W && moneyF / 10000 > 0 -> {
        BigDecimal.valueOf(moneyF / 10000)
          .setScale(1, BigDecimal.ROUND_DOWN)
          .stripTrailingZeros().toPlainString() + "W"
      }

      else ->
        BigDecimal.valueOf(moneyF)
          .setScale(scale, BigDecimal.ROUND_DOWN)
          .stripTrailingZeros().toPlainString()
          .let {
            if (it.toDouble().absoluteValue < 0.000001) {
              "0"
            } else {
              it
            }
          }
    }
  } catch (e: Exception) {
    e.printStackTrace()
    moneyF.toString()
  }
}

// ---------------------Rx-------------------------------//

fun <T : Any> Single<T>.subscribeOnIoThread(): Single<T> = subscribeOn(Schedulers.io())

fun <T : Any> Single<T>.observeOnMainThread(): Single<T> = observeOn(AndroidSchedulers.mainThread())

fun <T : Any> Single<T>.subscribeAndObserve(): Single<T> =
  subscribeOnIoThread().observeOnMainThread()

fun <T : Any> Observable<T>.subscribeOnIoThread(): Observable<T> = subscribeOn(Schedulers.io())

fun <T : Any> Observable<T>.observeOnMainThread(): Observable<T> =
  observeOn(AndroidSchedulers.mainThread())

fun <T : Any> Observable<T>.subscribeAndObserve(): Observable<T> =
  subscribeOnIoThread().observeOnMainThread()

// ---------------------Res-------------------------------//

fun @receiver:DrawableRes Int.getDrawable(): Drawable? = try {
  ResourceUtils.getDrawable(this)
} catch (e: Exception) {
  e.printStackTrace()
  null
}

@ColorInt
fun @receiver:ColorRes Int.getColor(): Int = try {
  ColorUtils.getColor(this)
} catch (e: Exception) {
  e.printStackTrace()
  -1
}

fun @receiver:StringRes Int.getString(): String? = try {
  StringUtils.getString(this)
} catch (e: Exception) {
  e.printStackTrace()
  null
}

fun @receiver:PluralsRes Int.getQuantityString(num: Int): String? = try {
  CommonApplication.app.resources.getQuantityString(this, num, num)
} catch (e: Exception) {
  e.printStackTrace()
  null
}

fun @receiver:StringRes Int.formatString(vararg args: Any): String =
  String.format(getString().orEmpty(), args)

@Px
fun @receiver:Dimension(unit = Dimension.SP) Float.sp2px(): Int = SizeUtils.sp2px(this)

@Px
fun @receiver:Dimension(unit = Dimension.DP) Float.dp2px(): Int = SizeUtils.dp2px(this)

@Px
fun Float.pt2px(): Int = AdaptScreenUtils.pt2Px(this)

@Dimension(unit = Dimension.SP)
fun @receiver:Dimension Int.px2sp(): Int = SizeUtils.px2sp(this.toFloat())

@Dimension(unit = Dimension.DP)
fun @receiver:Dimension Int.px2dp(): Int = SizeUtils.px2dp(this.toFloat())

fun @receiver:Dimension Int.px2pt(): Int = AdaptScreenUtils.px2Pt(this.toFloat())

fun Bitmap.toDrawable(): Drawable = ImageUtils.bitmap2Drawable(this)

fun Drawable.toBitmap(): Bitmap = ImageUtils.drawable2Bitmap(this)

// ---------------------View-------------------------------//

inline fun <reified T> DiffUtil.ItemCallback<T>.asConfig(): AsyncDifferConfig<T> {
  return AsyncDifferConfig.Builder(this)
    .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
    .build()
}

// ---------------------Fragment-------------------------------//

@MainThread
inline fun <reified V, reified VM> V.getViewModel(): Lazy<VM>
  where V : LifecycleOwner, V : ViewModelStoreOwner,
        VM : ViewModel, VM : DefaultLifecycleObserver = lazy(LazyThreadSafetyMode.NONE) {
  ViewModelProvider(this)[VM::class.java].apply(lifecycle::addObserver)
}

@MainThread
inline fun <reified V, reified VM : BaseThemeViewModel> V.getThemeViewModel(): Lazy<VM>
  where V : LifecycleOwner, V : ViewModelStoreOwner, V : ITheme = lazy(LazyThreadSafetyMode.NONE) {
  ViewModelProvider(this)[VM::class.java].also {
    lifecycle.addObserver(it)
    it.loading.observe(this) { show ->
      if (show) showLoading() else hideLoading()
    }
  }
}

inline fun <reified T : ViewDataBinding> Fragment.inflate(crossinline transform: (T) -> Unit = {}): Lazy<T> =
  lazy(LazyThreadSafetyMode.NONE) {
    inflateBinding<T>(layoutInflater).also {
      it.lifecycleOwner = viewLifecycleOwner
      transform(it)
    }
  }

// ---------------------Activity-------------------------------//

inline fun <reified T : ViewDataBinding> ComponentActivity.inflate(crossinline transform: (T) -> Unit = {}): Lazy<T> =
  lazy(LazyThreadSafetyMode.NONE) {
    inflateBinding<T>(layoutInflater).also {
      setContentView(it.root)
      it.lifecycleOwner = this
      transform(it)
    }
  }

@Suppress("UNCHECKED_CAST")
fun <T : ViewBinding> LifecycleOwner.inflateBinding(inflater: LayoutInflater): T {
  return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
    .filterIsInstance<Class<T>>()
    .first()
    .getDeclaredMethod("inflate", LayoutInflater::class.java)
    .invoke(null, inflater) as T
}
