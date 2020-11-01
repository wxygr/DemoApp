package io.goooler.demoapp.common.util

import com.blankj.utilcode.util.SPUtils
import io.goooler.demoapp.base.util.isNotNullOrEmpty

@Suppress("MemberVisibilityCanBePrivate", "unused")
object AppUserInfoManager {

    private const val SP_USER_INFO_KEY = "spUserInfoKey"
    private val spUtil = SPUtils.getInstance("spUserInfoName")
    private var info: UserInfoBean? = null

    var userId: String? = null
        get() = info?.userId
        private set
    var token: String? = null
        get() = info?.token
        private set
    var userName: String? = null
        get() = info?.userName
        private set
    var nickName: String? = null
        get() = info?.nickName
        private set

    init {
        info = spUtil.getString(SP_USER_INFO_KEY).fromJson<UserInfoBean>()
    }

    val haveLogin get() = token.isNotNullOrEmpty() && userId.isNotNullOrEmpty()

    @Synchronized
    fun saveUserInfo(bean: UserInfoBean) {
        info = bean
        spUtil.put(SP_USER_INFO_KEY, bean.toJson())
    }

    fun resetUserInfo() {
        info = null
        spUtil.put(SP_USER_INFO_KEY, "")
    }

    class UserInfoBean(
        val userId: String,
        val token: String,
        val userName: String,
        val nickName: String
    )
}