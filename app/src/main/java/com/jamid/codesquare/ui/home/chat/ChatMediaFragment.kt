package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.CHAT_CHANNEL
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatMediaBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatMediaFragment : BaseFragment<FragmentChatMediaBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatMediaBinding {
        return FragmentChatMediaBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannel = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL) ?: return
        binding.chatMediaPager.adapter = ChatMediaAdapter(chatChannel.chatChannelId, activity)

        OverScrollDecoratorHelper.setUpOverScroll((binding.chatMediaPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        TabLayoutMediator(binding.chatTabs, binding.chatMediaPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = "Photos"
                1 -> tab.text = "Documents"
            }
        }.attach()

    }

    private inner class ChatMediaAdapter(val chatChannelId: String, activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                ChatGalleryFragment.newInstance(chatChannelId)
            } else {
                ChatDocumentsFragment.newInstance(chatChannelId)
            }
        }
    }



    companion object {
        const val TAG = "ChatMediaFragment"

        fun newInstance(bundle: Bundle)
            = ChatMediaFragment().apply {
                arguments = bundle
            }

    }

}