package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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

    var shouldHideRecyclerView = false
    var shouldShowImage = true
    var shouldShowProgress = true

    val isEmpty = MutableLiveData<Boolean>()

    protected abstract fun getAdapter(): PagingDataAdapter<T, VH>

    open fun getItems(func: suspend () -> Flow<PagingData<T>>) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            func().collectLatest {
                Log.d(TAG, "New data")
                pagingAdapter.submitData(it)
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
        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        if (isViewPagerFragment) {
            binding.noDataImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = binding.pagerRoot.id
                bottomToTop = binding.pagerNoItemsText.id
                startToStart = binding.pagerRoot.id
                endToEnd = binding.pagerRoot.id
                verticalBias = 0.25f
                horizontalBias = 0.5f
                setMargins(smallMargin)
            }
        } else {
            binding.pagerNoItemsText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = binding.pagerRoot.id
                bottomToTop = binding.pagerNoItemsText.id
                startToStart = binding.pagerRoot.id
                endToEnd = binding.pagerRoot.id
                verticalBias = 0.5f
                horizontalBias = 0.5f
                setMargins(smallMargin)
            }
        }
    }

    open fun onViewLaidOut() {
        initLayout()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewLaidOut()
    }

    open fun initLayout() {
        binding.pagerItemsRecycler.apply {
            adapter = pagingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        addLoadListener()

        binding.pagerRefresher.let {
            it.setOnRefreshListener {
                pagingAdapter.refresh()
            }

            it.setColorSchemeColors(requireContext().accentColor())

            if (isNightMode()) {
                it.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.darkest_grey_2))
            } else {
                it.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            val zero = resources.getDimension(R.dimen.zero).toInt()
            val actionBarOffset = resources.getDimension(R.dimen.action_bar_height).toInt()

            it.setProgressViewOffset(false, zero, actionBarOffset)
        }

    }

    open fun addLoadListener() {
        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                if (itemCount != 0) {
                    // hide info and show recyclerview
                    isEmpty.postValue(false)

                    binding.pagerItemsRecycler.show()
                    binding.pagerNoItemsText.hide()
                    binding.noDataImage.hide()
                } else {
                    isEmpty.postValue(true)
                    if (shouldHideRecyclerView) {
                        binding.pagerItemsRecycler.hide()
                    }
                    // hide recyclerview and show info
                    if (shouldShowImage) {
                        binding.noDataImage.show()
                    }
                    binding.pagerNoItemsText.show()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest {
                when (it.refresh) {
                    is LoadState.Loading -> {
                        if (shouldShowProgress) {
                            binding.pagerRefresher.isRefreshing = true
                        }

                        if (shouldHideRecyclerView) {
                            binding.pagerItemsRecycler.hide()
                        }
                        binding.pagerNoItemsText.hide()
                        binding.noDataImage.hide()
                    }
                    is LoadState.Error -> {
                        // when something went wrong while refreshing
                        binding.pagerRefresher.isRefreshing = false

                        if (shouldHideRecyclerView) {
                            binding.pagerItemsRecycler.hide()
                        }
                        binding.pagerNoItemsText.text = getString(R.string.common_error_text)
                        if (shouldShowImage) {
                            binding.noDataImage.show()
                        }
                        binding.noDataImage.show()
                    }
                    is LoadState.NotLoading -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(1500)
                            binding.pagerRefresher.isRefreshing = false
                        }
                    }
                }

                when (it.append) {
                    is LoadState.Loading -> {
                        Log.d(TAG, "Append function - Loading")
                        // when append is loading
                        if (shouldShowProgress) {
                            binding.pagerRefresher.isRefreshing = true
                        }
                        binding.pagerNoItemsText.hide()
                        binding.noDataImage.hide()
                    }
                    is LoadState.Error -> {
                        Log.d(TAG, "Append function - Error")

                        // when append went wrong
                        // when something went wrong while refreshing
                        binding.pagerRefresher.isRefreshing = false
                        if (shouldHideRecyclerView) {
                            binding.pagerItemsRecycler.hide()
                        }

                        binding.pagerNoItemsText.text = getString(R.string.common_error_text)
                        binding.pagerNoItemsText.show()
                        if (shouldShowImage) {
                            binding.noDataImage.show()
                        }
                    }
                    is LoadState.NotLoading -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(1500)
                            binding.pagerRefresher.isRefreshing = false
                        }
                    }
                }

                if (pagingAdapter.itemCount != 0) {
                    // non empty
                    binding.pagerItemsRecycler.show()
                    binding.pagerNoItemsText.hide()
                    binding.noDataImage.hide()
                    isEmpty.postValue(false)
                } else {
                    // empty
                    isEmpty.postValue(true)
                    if (shouldHideRecyclerView) {
                        binding.pagerItemsRecycler.hide()
                    }
                    binding.pagerNoItemsText.show()
                    if (shouldShowImage) {
                        binding.noDataImage.show()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PagerListFragment"
    }

}