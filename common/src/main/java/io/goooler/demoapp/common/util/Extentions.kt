@file:Suppress("unused")

package io.goooler.demoapp.common.util

import android.webkit.URLUtil
import androidx.annotation.Px
import com.blankj.utilcode.util.AdaptScreenUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.TimeUtils
import io.goooler.demoapp.common.BuildConfig
import io.goooler.demoapp.common.type.SpKeys
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.*

val isFirstRun: Boolean = SPUtils.getInstance().getBoolean(SpKeys.SP_FIRST_RUN.key, true)

/**
 * 拼上图片前缀
 */
fun String.toLoadUrl(): String {
    return if (URLUtil.isNetworkUrl(this)) this else BuildConfig.CDN_PREFIX + this
}

/**
 * 获取图片宽高比例，如：/assets/img/2019/07/18/n_1563460410803_3849___size550x769.jpg
 */
fun String.getSizeByLoadUrl(defaultWidth: Int, defaultHeight: Int): List<Int> {
    val sizeList = arrayListOf(
        defaultWidth, defaultHeight
    )
    val flag = "size"
    if (!contains(BuildConfig.CDN_PREFIX) || !contains(flag)) {
        return sizeList
    }
    Regex("$flag(\\d+x\\d+)")
        .findAll(this)
        .forEach {
            // size550x769
            val sizeXXXxXXX = it.value
            // 550x769
            val mXXXxXXX = sizeXXXxXXX.replace(flag, "")
            val list = mXXXxXXX.split("x")
            if (list.size < 2) {
                return sizeList
            }
            // 550
            val width = list[0].toInt()
            // 769
            val height = list[1].toInt()
            sizeList.clear()
            sizeList.add(width)
            sizeList.add(height)
            return sizeList
        }
    return sizeList
}

fun Long.toDateString(pattern: String): String {
    return TimeUtils.millis2String(this, pattern)
}

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
        !isSameYear -> toDateString("yyyy-MM-dd HH:mm")
        isYesterday -> toDateString("昨天 HH:mm")
        t < oneMinute -> "刚刚"
        t < oneHour -> (t / oneMinute).toString() + "分钟前"
        t < oneDay -> (t / oneHour).toString() + "小时前"
        isSameYear -> toDateString("MM-dd HH:mm")
        else -> toDateString("yyyy-MM-dd HH:mm")
    }
}

typealias DimensionUtil = SizeUtils

@Px
fun Float.sp2px(): Int = SizeUtils.sp2px(this)

@Px
fun Float.dp2px(): Int = SizeUtils.dp2px(this)

@Px
fun Float.pt2px(): Int = AdaptScreenUtils.pt2Px(this)

fun Int.px2sp(): Int = SizeUtils.px2sp(this.toFloat())

fun Int.px2dp(): Int = SizeUtils.px2dp(this.toFloat())

fun Int.px2pt(): Int = AdaptScreenUtils.px2Pt(this.toFloat())

//---------------------Rx-------------------------------//

fun <T> Single<T>.observeOnMainThread(): Single<T> {
    return observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.observeOnMainThread(): Observable<T> {
    return observeOn(AndroidSchedulers.mainThread())
}