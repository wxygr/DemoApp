package io.goooler.demoapp.main.vm

import android.app.Application
import io.goooler.demoapp.R
import io.goooler.demoapp.api.RetrofitHelper
import io.goooler.demoapp.base.BaseViewModel
import io.goooler.demoapp.base.MutableStringLiveData
import io.goooler.demoapp.main.api.MainApi
import io.goooler.demoapp.main.repository.MainRepository
import io.reactivex.schedulers.Schedulers

class MainViewModel(application: Application) : BaseViewModel(application) {
    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        MainRepository(RetrofitHelper.createApiService(MainApi::class.java))
    }

    val title = MutableStringLiveData()

    fun initData() {
        repository.getDemoAppInfo()
                .subscribeOn(Schedulers.io())
                .filter {
                    it.status
                }
                .map {
                    return@map it.entry!!.name.orEmpty()
                }
                .subscribe({
                    title.postValue(it)
                }, {
                    title.postValue(getString(R.string.main_fragment))
                    silentThrowable(it)
                })
                .let {
                    addDisposable(it)
                }
    }
}