package com.jamid.codesquare

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ViewHolderState {
    STATE_RECYCLED, STATE_ATTACHED, STATE_DETACHED
}

abstract class BottomSheetPagingFragment<VB: ViewBinding, T: Any, VH: RecyclerView.ViewHolder>: BaseBottomFragment<VB>(), PagingFragment<T, VH> {

    private val _pagingVHParentStateEmitter = MutableLiveData<ViewHolderState>()
    override val pagingViewHolderParentStateEmitter: LiveData<ViewHolderState> = _pagingVHParentStateEmitter
    override var myPagingAdapter: PagingDataAdapter<T, VH>? = null
    override var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(viewLifecycleOwner)
    }

}

enum class AdapterState {
    REFRESH_LOADING, REFRESH_ERROR, REFRESH_NOT_LOADING,
    APPEND_LOADING, APPEND_ERROR, APPEND_NOT_LOADING,
    LOAD_FINISHED
}

interface PagingFragment<T: Any, VH: RecyclerView.ViewHolder> {

    val pagingViewHolderParentStateEmitter: LiveData<ViewHolderState>
    var myPagingAdapter: PagingDataAdapter<T, VH>?
    fun getPagingAdapter(): PagingDataAdapter<T, VH>
    var job: Job?

    fun initialize(lifecycleOwner: LifecycleOwner) {
        myPagingAdapter = getPagingAdapter()
        setAdapterStateListeners(lifecycleOwner)
    }

    fun getItems(lifecycleOwner: LifecycleOwner, func: suspend () -> Flow<PagingData<T>>) {
        job?.cancel()
        job = lifecycleOwner.lifecycleScope.launch {
            func().collectLatest {
                myPagingAdapter?.submitData(it)
            }
        }
    }

    fun onPagingDataChanged(itemCount: Int)
    fun onNewDataAdded(positionStart: Int, itemCount: Int)

    fun setDefaultPagingLayoutBehavior(
        state: AdapterState,
        error: Throwable?,
        retryBtn: Button?,
        infoText: TextView?,
        recyclerView: RecyclerView?,
        progress: ProgressBar?,
        swipeRefresh: SwipeRefreshLayout? = null
    ) {

        when (state) {
            AdapterState.REFRESH_LOADING -> {
                recyclerView?.hide()
                progress?.show()
                infoText?.show()
                infoText?.text = "Loading items"
                retryBtn?.hide()
                Log.d(TAG, "setDefaultPagingLayoutBehavior: REFRESH_LOADING")
                if (swipeRefresh?.isRefreshing == false) {
                    swipeRefresh.isRefreshing = true
                }
            }
            AdapterState.REFRESH_ERROR -> {
                recyclerView?.hide()
                progress?.hide()
                infoText?.show()
                infoText?.text = error?.localizedMessage
                retryBtn?.show()
                swipeRefresh?.isRefreshing = false
                Log.d(TAG, "setDefaultPagingLayoutBehavior: REFRESH_ERROR")
            }
            AdapterState.REFRESH_NOT_LOADING -> {
                recyclerView?.show()
                progress?.hide()
                infoText?.hide()
                retryBtn?.hide()
                swipeRefresh?.isRefreshing = false
                Log.d(TAG, "setDefaultPagingLayoutBehavior: REFRESH_NOT_LOADING")
                onPagingDataChanged(myPagingAdapter?.itemCount ?: 0)
            }
            AdapterState.APPEND_LOADING -> {
                recyclerView?.show()
                progress?.hide()
                infoText?.hide()
                retryBtn?.hide()
                swipeRefresh?.isRefreshing = false
                Log.d(TAG, "setDefaultPagingLayoutBehavior: APPEND_LOADING")
            }
            AdapterState.APPEND_ERROR -> {
                recyclerView?.show()
                progress?.hide()
                infoText?.hide()
                retryBtn?.hide()
                swipeRefresh?.isRefreshing = false
                Log.d(TAG, "setDefaultPagingLayoutBehavior: APPEND_ERROR")

            }
            AdapterState.APPEND_NOT_LOADING -> {
                recyclerView?.show()
                progress?.hide()
                infoText?.hide()
                retryBtn?.hide()
                swipeRefresh?.isRefreshing = false
                Log.d(TAG, "setDefaultPagingLayoutBehavior: APPEND_NOT_LOADING")
            }
            AdapterState.LOAD_FINISHED -> {
                recyclerView?.show()
                progress?.hide()
                infoText?.hide()
                retryBtn?.hide()
                swipeRefresh?.isRefreshing = false

                Log.d(TAG, "setDefaultPagingLayoutBehavior: LOAD_FINISHED")
                onPagingDataChanged(myPagingAdapter?.itemCount ?: 0)
            }
        }
    }

    fun onAdapterStateChanged(state: AdapterState, error: Throwable?)

    fun setAdapterStateListeners(lifecycleOwner: LifecycleOwner) {
        myPagingAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                onPagingDataChanged(myPagingAdapter?.itemCount ?: 0)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                Log.d(TAG, "onItemRangeChanged: $positionStart, $itemCount")
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                onNewDataAdded(positionStart, itemCount)
            }

        })

        lifecycleOwner.lifecycleScope.launch {
            myPagingAdapter?.loadStateFlow?.collectLatest {

                when (val s = it.refresh) {
                    is LoadState.Loading -> {

                        onAdapterStateChanged(AdapterState.REFRESH_LOADING, null)
                    }
                    is LoadState.Error -> {
                        onAdapterStateChanged(AdapterState.REFRESH_ERROR, s.error)
                    }
                    is LoadState.NotLoading -> {
                        if (s.endOfPaginationReached) {
                            onAdapterStateChanged(AdapterState.LOAD_FINISHED, null)
                        } else {
                            onAdapterStateChanged(AdapterState.REFRESH_NOT_LOADING, null)
                        }
                    }
                }

                when (val t = it.append) {
                    is LoadState.Loading -> {
                        onAdapterStateChanged(AdapterState.APPEND_LOADING, null)
                    }
                    is LoadState.Error -> {
                        onAdapterStateChanged(AdapterState.APPEND_ERROR, t.error)
                    }
                    is LoadState.NotLoading -> {
                        if (t.endOfPaginationReached) {
                            onAdapterStateChanged(AdapterState.LOAD_FINISHED, null)
                        } else {
                            onAdapterStateChanged(AdapterState.APPEND_NOT_LOADING, null)
                        }
                    }
                }
            }
        }
    }

    fun retryPaging() {
        myPagingAdapter?.retry()
    }

    fun refreshPaging() {
        myPagingAdapter?.refresh()
    }
}

abstract class PagingDataFragment<VB: ViewBinding, T : Any, VH : RecyclerView.ViewHolder> :
    BaseFragment<VB>(), PagingFragment<T, VH> {

    private val _pagingVHParentStateEmitter = MutableLiveData<ViewHolderState>()
    override val pagingViewHolderParentStateEmitter = _pagingVHParentStateEmitter
    override var myPagingAdapter: PagingDataAdapter<T, VH>? = null
    override var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(viewLifecycleOwner)
    }

}
