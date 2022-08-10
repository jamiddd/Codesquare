package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.NotificationAdapter
import com.jamid.codesquare.adapter.recyclerview.NotificationViewHolder
import com.jamid.codesquare.data.Notification

class NotificationFragment: DefaultPagingFragment<Notification, NotificationViewHolder>() {

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
                    viewModel.deleteNotification(notification)
                    FireUtility.deleteNotification(notification) {
                        if (!it.isSuccessful) {
                            viewModel.insertNotifications(notification)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getItems(viewLifecycleOwner) {
            viewModel.getNotifications()
        }

        binding.noDataImage.setAnimation(R.raw.empty_notification)

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.pagerItemsRecycler)

        val padding = resources.getDimension(R.dimen.large_padding).toInt()
        binding.pagerItemsRecycler.setPadding(0, 0, 0, padding)

    }


    override fun getDefaultInfoText(): String {
        return getString(R.string.empty_notifications_greet)
    }

    override fun getPagingAdapter(): PagingDataAdapter<Notification, NotificationViewHolder> {
        return NotificationAdapter()
    }

}