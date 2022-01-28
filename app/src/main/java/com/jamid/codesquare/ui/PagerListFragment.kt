package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentPagerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalPagingApi
abstract class PagerListFragment<T: Any, VH: RecyclerView.ViewHolder> : Fragment() {

    open var job: Job? = null
    lateinit var pagingAdapter: PagingDataAdapter<T, VH>
    protected val viewModel: MainViewModel by activityViewModels()
    lateinit var binding: FragmentPagerBinding
    protected var recyclerView: RecyclerView? = null
    protected var noItemsText: TextView? = null
    private var swipeRefresher: SwipeRefreshLayout? = null
    var shouldHideRecyclerView = false
    var shouldShowImage = true
    var shouldScrollToBottomOnNewData = false

    val isEmpty = MutableLiveData<Boolean>()

    protected abstract fun getAdapter(): PagingDataAdapter<T, VH>

    open fun getItems(func: suspend () -> Flow<PagingData<T>>) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            func().collectLatest {
                pagingAdapter.submitData(it)
                if (!shouldScrollToBottomOnNewData) {
                    delay(500)
                    binding.pagerItemsRecycler.scrollToPosition(0)
                }
            }
        }
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

    fun setIsViewPagerFragment(isViewPagerFragment: Boolean) {
        if (isViewPagerFragment) {
            val eightDp = convertDpToPx(8)
            val params = binding.noDataImage.layoutParams as ConstraintLayout.LayoutParams
            params.topToTop = binding.pagerRoot.id
            params.bottomToTop = binding.pagerNoItemsText.id
            params.startToStart = binding.pagerRoot.id
            params.endToEnd = binding.pagerRoot.id
            params.verticalBias = 0.25f
            params.horizontalBias = 0.5f
            params.setMargins(eightDp)
            binding.noDataImage.layoutParams = params
        } else {
            val eightDp = convertDpToPx(8)
            val params = binding.pagerNoItemsText.layoutParams as ConstraintLayout.LayoutParams
            params.topToTop = binding.pagerRoot.id
            params.bottomToTop = binding.pagerNoItemsText.id
            params.startToStart = binding.pagerRoot.id
            params.endToEnd = binding.pagerRoot.id
            params.verticalBias = 0.5f
            params.horizontalBias = 0.5f
            params.setMargins(eightDp)
            binding.pagerNoItemsText.layoutParams = params
        }
    }

    open fun onViewLaidOut() {
        val progressBar = activity?.findViewById<ProgressBar>(R.id.main_progress_bar)
        recyclerView = binding.pagerItemsRecycler
        noItemsText = binding.pagerNoItemsText
        swipeRefresher = binding.pagerRefresher
        initLayout(binding.pagerItemsRecycler, binding.pagerActionBtn, binding.pagerNoItemsText, progressBar = progressBar, image = binding.noDataImage, refresher = binding.pagerRefresher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewLaidOut()
    }

    open fun initLayout(recyclerView: RecyclerView, actionBtn: MaterialButton? = null, infoText: TextView? = null, progressBar: ProgressBar? = null, image: ImageView? = null, refresher: SwipeRefreshLayout? = null) {
        recyclerView.apply {
            adapter = pagingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        addLoadListener(recyclerView, actionBtn, infoText, progressBar, image, refresher = refresher)

        refresher?.let {
            it.setOnRefreshListener {
                pagingAdapter.refresh()
            }

            it.setColorSchemeColors(requireContext().accentColor())

            if (isNightMode()) {
                it.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.darkest_grey_2))
            } else {
                it.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            it.setProgressViewOffset(false, convertDpToPx(0), convertDpToPx(56))
        }

    }

    open fun addLoadListener(recyclerView: RecyclerView, actionBtn: MaterialButton? = null, infoText: TextView? = null, progressBar: ProgressBar? = null, image: ImageView? = null, refresher: SwipeRefreshLayout? = null) {
        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                if (itemCount != 0) {
                    // hide info and show recyclerview
                    isEmpty.postValue(false)

                    recyclerView.show()
                    infoText?.hide()
                    image?.hide()
                } else {
                    isEmpty.postValue(true)
                    if (shouldHideRecyclerView) {
                        recyclerView.hide()
                    }
                    // hide recyclerview and show info
                    if (shouldShowImage) {
                        image?.show()
                    }
                    infoText?.show()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest {
                when (it.refresh) {
                    is LoadState.Loading -> {
                        Log.d(TAG, "Refresh function - Loading")

                        refresher?.isRefreshing = true
                        // when refresh has just started
//                        progressBar?.show()
                        if (shouldHideRecyclerView) {
                            recyclerView.hide()
                        }
                        infoText?.hide()
                        image?.hide()
                    }
                    is LoadState.Error -> {
                        Log.d(TAG, "Refresh function - Error")

                        // when something went wrong while refreshing
                        refresher?.isRefreshing = false
//                        progressBar?.hide()
                        if (shouldHideRecyclerView) {
                            recyclerView.hide()
                        }
                        infoText?.text = getString(R.string.common_error_text)
                        if (shouldShowImage) {
                            image?.show()
                        }
                        image?.show()
                    }
                    is LoadState.NotLoading -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(1500)
//                            progressBar?.hide()
                            refresher?.isRefreshing = false
                        }
                    }
                }

                when (it.append) {
                    is LoadState.Loading -> {
                        Log.d(TAG, "Append function - Loading")
                        // when append is loading
                        refresher?.isRefreshing = true
//                        progressBar?.show()
                        infoText?.hide()
                        image?.hide()
                    }
                    is LoadState.Error -> {
                        Log.d(TAG, "Append function - Error")

                        // when append went wrong
                        // when something went wrong while refreshing
                        refresher?.isRefreshing = false
//                        progressBar?.hide()
                        if (shouldHideRecyclerView) {
                            recyclerView.hide()
                        }

                        infoText?.text = getString(R.string.common_error_text)
                        infoText?.show()
                        if (shouldShowImage) {
                            image?.show()
                        }
                    }
                    is LoadState.NotLoading -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(1500)
//                            progressBar?.hide()
                            refresher?.isRefreshing = false
                        }
                    }
                }

                if (pagingAdapter.itemCount != 0) {
                    // non empty
                    recyclerView.show()
                    infoText?.hide()
                    image?.hide()
                    isEmpty.postValue(false)
                } else {
                    // empty
                    isEmpty.postValue(true)
                    if (shouldHideRecyclerView) {
                        recyclerView.hide()
                    }
                    infoText?.show()
                    if (shouldShowImage) {
                        image?.show()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PagerListFragment"
    }

}