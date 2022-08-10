package com.jamid.codesquare.adapter.viewpager

import android.annotation.SuppressLint
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.ui.MediaFragment

class FullscreenMediaViewPagerAdapter(
    activity: FragmentActivity,
    private val message: Message? = null
): FragmentStateAdapter(activity) {
    init {
        Log.d("Something", "Simple: ")
    }
    private val mediaItems = mutableListOf<MediaItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: List<MediaItem>) {
        mediaItems.apply {
            clear()
            addAll(list)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mediaItems.size
    }

    override fun createFragment(position: Int): Fragment {
        return MediaFragment.newInstance(mediaItems[position], message)
    }

}