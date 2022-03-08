package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.CHAT_CHANNEL
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatMediaBinding

@ExperimentalPagingApi
class ChatMediaFragment : Fragment() {

    private lateinit var binding: FragmentChatMediaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatMediaBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        val chatChannel = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL) ?: return
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)

        binding.chatMediaPager.adapter = ChatMediaAdapter(chatChannel.chatChannelId, activity)

        (binding.chatMediaPager.getChildAt(0) as RecyclerView).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER

        TabLayoutMediator(tabLayout, binding.chatMediaPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = "Images"
                1 -> tab.text = "Document"
            }
        }.attach()

    }

    @ExperimentalPagingApi
    private inner class ChatMediaAdapter(val chatChannelId: String, activity: FragmentActivity) :
        FragmentStateAdapter(activity) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                ChatImagesFragment.newInstance(chatChannelId)
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