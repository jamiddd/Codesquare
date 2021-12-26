package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.NotificationAdapter
import com.jamid.codesquare.adapter.recyclerview.NotificationViewHolder
import com.jamid.codesquare.data.Notification

@ExperimentalPagingApi
class NotificationFragment: PagerListFragment<Notification, NotificationViewHolder>() {

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
    }

    override fun getAdapter(): PagingDataAdapter<Notification, NotificationViewHolder> {
        return NotificationAdapter()
    }

}