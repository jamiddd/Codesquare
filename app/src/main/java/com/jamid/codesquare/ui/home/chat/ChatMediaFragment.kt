package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.FragmentChatMediaBinding

class ChatMediaFragment: Fragment() {

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

        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL) ?: return
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)

        binding.chatMediaPager.adapter = ChatMediaAdapter(chatChannelId, activity)

        TabLayoutMediator(tabLayout, binding.chatMediaPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = "Images"
                1 -> tab.text = "Document"
            }
        }.attach()

    }

    private inner class ChatMediaAdapter(val chatChannelId: String, activity: FragmentActivity): FragmentStateAdapter(activity) {
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
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
    }

}