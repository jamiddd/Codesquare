package com.jamid.codesquare.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.databinding.FragmentPagerBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import com.jamid.codesquare.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class PagerListBottomFragment<T: Any, VH: RecyclerView.ViewHolder> : DialogFragment() {

    open var job: Job? = null
    lateinit var pagingAdapter: PagingDataAdapter<T, VH>
    protected val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentPagerBinding
    protected var recyclerView: RecyclerView? = null
    protected var noItemsText: TextView? = null
    protected var swipeRefresher: SwipeRefreshLayout? = null

    protected abstract fun getAdapter(): PagingDataAdapter<T, VH>

    open fun getItems(func: suspend () -> Flow<PagingData<T>>) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            func().collectLatest {
                pagingAdapter.submitData(it)
            }
        }
    }

    /** The system calls this only when creating the layout in a dialog. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPagerBinding.inflate(inflater)
        pagingAdapter = getAdapter()
        return binding.root
    }

    open fun onViewLaidOut() {
        recyclerView = binding.pagerItemsRecycler
        noItemsText = binding.pagerNoItemsText
        swipeRefresher = binding.pagerRefresher
        initLayout(binding.pagerItemsRecycler, binding.pagerNoItemsText, refresher = binding.pagerRefresher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewLaidOut()
    }

    open fun initLayout(recyclerView: RecyclerView, infoText: TextView? = null, progressBar: ProgressBar? = null, refresher: SwipeRefreshLayout? = null) {
        recyclerView.apply {
            adapter = pagingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.root.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT)

        addLoadListener(recyclerView, infoText, progressBar, refresher)

        refresher?.let {
            it.setOnRefreshListener {
                pagingAdapter.refresh()
            }
        }

    }

    open fun onItemsLoaded(positionStart: Int, itemCount: Int) {

    }

    open fun addLoadListener(recyclerView: RecyclerView, infoText: TextView? = null, progressBar: ProgressBar? = null, refresher: SwipeRefreshLayout? = null) {
        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                onItemsLoaded(positionStart, itemCount)
                if (itemCount != 0) {
                    // hide recyclerview and show info
                    recyclerView.show()
                    infoText?.hide()
                } else {
                    // hide info and show recyclerview
                    recyclerView.hide()
                    infoText?.show()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest {
                when (it.refresh) {
                    is LoadState.Loading -> {
                        Log.d(TAG, "Refresh function - Loading")

                        // when refresh has just started
                        progressBar?.show()
                        recyclerView.hide()
                        infoText?.hide()

                    }
                    is LoadState.Error -> {
                        Log.d(TAG, "Refresh function - Error")

                        // when something went wrong while refreshing
                        progressBar?.hide()
                        recyclerView.hide()
                        infoText?.text = "Something went wrong :("
                        infoText?.show()
                    }
                    is LoadState.NotLoading -> {
                        progressBar?.hide()
                        refresher?.isRefreshing = false
                    }
                }

                when (it.append) {
                    is LoadState.Loading -> {
                        Log.d(TAG, "Append function - Loading")
                        // when append is loading
                        progressBar?.show()
                        infoText?.hide()
                    }
                    is LoadState.Error -> {
                        Log.d(TAG, "Append function - Error")

                        // when append went wrong
                        // when something went wrong while refreshing
                        progressBar?.hide()
                        recyclerView.hide()

                        infoText?.text = "Something went wrong :("
                        infoText?.show()
                    }
                    is LoadState.NotLoading -> {
                        progressBar?.hide()
                        refresher?.isRefreshing = false
                    }
                }

                if (pagingAdapter.itemCount != 0) {
                    // non empty
                    recyclerView.show()
                    infoText?.hide()
                } else {
                    // empty
                    recyclerView.hide()
                    infoText?.show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "PagerListFragment"
    }

}