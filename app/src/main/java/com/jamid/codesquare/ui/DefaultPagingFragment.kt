package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.databinding.FragmentPagerBinding

abstract class DefaultPagingFragment<T: Any, VH: RecyclerView.ViewHolder> : PagingDataFragment<FragmentPagerBinding, T, VH>() {

    private var hasPausedOnce = false

    abstract fun getDefaultInfoText(): String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPagerBinding.bind(view)

        binding.pagerItemsRecycler.apply {
            adapter = myPagingAdapter
            itemAnimator = null
            /*addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(requireContext(), R.drawable.custom_gray_divider)?.let { setDrawable(it) }
            })*/
            layoutManager = LinearLayoutManager(requireContext())
        }

        setSwipeRefresher()

    }

    open fun setAppBarRecyclerBehavior() {
        // this is only for profile fragment
        binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            var hasGoneUp = false
            var totalScroll = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalScroll += dy
                if (totalScroll > 100) {
                    recyclerView.isNestedScrollingEnabled = false
                    hasGoneUp = true
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && totalScroll == 0 && hasGoneUp) {
                    recyclerView.isNestedScrollingEnabled = true
                    hasGoneUp = false
                }
            }
        })
    }

    open fun setSwipeRefresher(onRefresh: (() -> Unit)? = null) {
        binding.pagerRefresher.setDefaultSwipeRefreshLayoutUi()

        binding.pagerRefresher.setOnRefreshListener {
            if (!binding.pagerRefresher.canChildScrollUp()) {
                if (onRefresh != null) {
                    onRefresh()
                } else {
                    myPagingAdapter?.refresh()
                }
            }
            binding.pagerRefresher.isRefreshing = false
        }
    }

    override fun onPause() {
        super.onPause()
        hasPausedOnce = true
    }

    override fun onAdapterStateChanged(state: AdapterState, error: Throwable?) {
        if (hasPausedOnce) {
            setDefaultPagingLayoutBehavior(state, error, binding.pagerActionBtn, binding.pagerNoItemsText, binding.pagerItemsRecycler, null)
        } else {
            setDefaultPagingLayoutBehavior(state, error, binding.pagerActionBtn, binding.pagerNoItemsText, binding.pagerItemsRecycler, null, binding.pagerRefresher)
        }
    }

    override fun onPagingDataChanged(itemCount: Int) {
        if (itemCount == 0) {
            binding.pagerNoItemsText.fadeIn()
            binding.pagerNoItemsText.text = getDefaultInfoText()
        } else {
            binding.pagerNoItemsText.fadeOut().doOnEnd {
                binding.pagerNoItemsText.hide()
            }
        }
    }

    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {

    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPagerBinding {
        return FragmentPagerBinding.inflate(inflater)
    }

}