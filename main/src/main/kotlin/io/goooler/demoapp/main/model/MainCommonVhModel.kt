package io.goooler.demoapp.main.model

import io.goooler.demoapp.adapter.rv.core.ISpanSize
import io.goooler.demoapp.adapter.rv.diff.IDiffVhModelType
import io.goooler.demoapp.main.R

sealed class MainCommonVhModel : IDiffVhModelType, ISpanSize {

  class Repo(val logoUrl: String?, val content: String?, val fullName: String = "") :
    MainCommonVhModel() {

    override val viewType: Int = R.layout.main_common_rv_item

    override val spanSize: Int = ISpanSize.SPAN_SIZE_SINGLE

    override fun isItemTheSame(that: IDiffVhModelType): Boolean =
      (that as? Repo)?.content == this.content

    override fun isContentTheSame(that: IDiffVhModelType): Boolean = true

    interface OnEventListener {
      fun onContentClick(fullName: String) {}
      fun onItemClick(item: MainCommonVhModel) {}
    }
  }

  class Empty : MainCommonVhModel() {
    override val viewType: Int = io.goooler.demoapp.common.R.layout.common_empty_layout
  }

  class Error : MainCommonVhModel() {
    override val viewType: Int = io.goooler.demoapp.common.R.layout.common_error_layout
  }
}
