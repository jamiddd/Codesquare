package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.CHAT_CHANNEL
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatMediaBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class ChatMediaFragment : BaseFragment<FragmentChatMediaBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()

    override fun getViewBinding(): FragmentChatMediaBinding {
        return FragmentChatMediaBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        activity.binding.mainToolbar.menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        val chatChannel = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL) ?: return
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)

        binding.chatMediaPager.adapter = ChatMediaAdapter(chatChannel.chatChannelId, activity)

        OverScrollDecoratorHelper.setUpOverScroll(binding.chatMediaPager.getChildAt(0) as RecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

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