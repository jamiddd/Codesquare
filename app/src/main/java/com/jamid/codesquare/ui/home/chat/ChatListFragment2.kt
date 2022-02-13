package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatList2Binding
import com.jamid.codesquare.listeners.ChatChannelClickListener
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.SubscriptionFragment

@ExperimentalPagingApi
class ChatListFragment2: Fragment() {

    private lateinit var binding: FragmentChatList2Binding
    private lateinit var chatChannelAdapter2: ChatChannelAdapter2
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatList2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser
        chatChannelAdapter2 = ChatChannelAdapter2(currentUser.id, requireActivity() as ChatChannelClickListener)
        binding.chatListRecycler.apply {
            adapter = chatChannelAdapter2
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        binding.noChatChannelsText.text = getString(R.string.empty_chat_list_greet)

        viewModel.chatChannels.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                onChatChannelExists()
                chatChannelAdapter2.submitList(it)
            } else {
                onChatChannelNotFound()
            }
        }

        binding.exploreProjectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_preSearchFragment)
        }

        binding.getStartedBtn.setOnClickListener {
            if (currentUser.premiumState.toInt() == 1 || currentUser.projects.size < 2) {
                findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null, slideRightNavOptions())
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Collab")
                    .setMessage("You have already created 2 projects. To create more, upgrade your subscription plan!")
                    .setPositiveButton("Upgrade") { _, _ ->
                        val act = activity as MainActivity
                        act.subscriptionFragment = SubscriptionFragment()
                        act.subscriptionFragment?.show(act.supportFragmentManager, "SubscriptionFragment")
                    }.setNegativeButton("Cancel") { a, _ ->
                        a.dismiss()
                    }
                    .show()
            }
//            findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment)
        }


        if (currentUser.premiumState.toInt() == -1) {
            binding.chatListAdContainer.show()
            setAdView()
        } else {
            binding.chatListAdContainer.hide()
        }


        binding.chatChannelsRefresher.setOnRefreshListener {
            Firebase.firestore.collection(CHAT_CHANNELS)
                .whereArrayContains(CONTRIBUTORS, currentUser.id)
                .get()
                .addOnCompleteListener {
                    binding.chatChannelsRefresher.isRefreshing = false
                    Log.d(TAG, "Got some results")
                    if (it.isSuccessful) {
                        if (!it.result.isEmpty) {
                            val chatChannels = it.result.toObjects(ChatChannel::class.java)
                            viewModel.insertChatChannels(chatChannels)
                        }
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }

        }


    }

    private fun setAdView() {
        binding.adView.loadAd(AdRequest.Builder().build())
//        val adView = requireActivity().findViewById<AdView>(R.id.adView)
        /*if (rand == 2) {

            *//*
            val adLoader = AdLoader.Builder(requireContext(), "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd {
                    val styles = NativeTemplateStyle.Builder().build()
                    val template = requireActivity().findViewById<TemplateView>(R.id.my_template)
                    template.setStyles(styles)
                    template.setNativeAd(it)
                }.withAdListener(object: AdListener() {
                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        Log.e(TAG, p0.message)
                    }
                }).build()

            adLoader.loadAd()*//*
        } else {
            adView?.hide()
        }*/
//        adView.loadAd(AdRequest.Builder().build())
    }

    private fun onChatChannelExists() {
        binding.noChatChannelsText.hide()
        binding.noChannelsImage.hide()
        binding.getStartedBtn.hide()
        binding.exploreProjectBtn.hide()
    }

    private fun onChatChannelNotFound() {
        binding.noChatChannelsText.show()
        binding.noChannelsImage.show()
        binding.getStartedBtn.show()
        binding.exploreProjectBtn.show()
    }

}