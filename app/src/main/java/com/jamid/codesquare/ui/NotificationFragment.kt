package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.NotificationAdapter
import com.jamid.codesquare.adapter.recyclerview.NotificationViewHolder
import com.jamid.codesquare.data.Notification

@ExperimentalPagingApi
class NotificationFragment: PagerListFragment<Notification, NotificationViewHolder>() {

    private val simpleCallback = object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val notificationViewHolder = viewHolder as NotificationViewHolder?
            if (notificationViewHolder != null) {
                val notification = notificationViewHolder.notificationCopy
                if (notification != null) {
                    FireUtility.deleteNotification(notification) {
                        if (it.isSuccessful) {
                            viewModel.deleteNotification(notification)
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                }
            }
        }
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        getItems {
            viewModel.getNotifications()
        }

        binding.pagerNoItemsText.text = getString(R.string.empty_notifications_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)

        isEmpty.observe(viewLifecycleOwner) {
            val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
            if (toolbar != null) {
                if (toolbar.menu.size() > 0) {
                    toolbar.menu.getItem(0).isVisible = !it
                }
            }
        }

        binding.pagerItemsRecycler.itemAnimator = null

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.pagerItemsRecycler)

    }



    override fun getAdapter(): PagingDataAdapter<Notification, NotificationViewHolder> {
        return NotificationAdapter()
    }

}