package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostListAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.PostListLayoutBinding
import com.jamid.codesquare.listeners.PostMiniItemClickListener

@ExperimentalPagingApi
class PostListFragment: RoundedBottomSheetDialogFragment(), PostMiniItemClickListener {

    private lateinit var binding: PostListLayoutBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PostListLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val receiver = arguments?.getParcelable<User>(USER) ?: return

        val postListAdapter = PostListAdapter(receiver.id, this)

        binding.postsList.apply {
            adapter = postListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.getCurrentUserPosts().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                val currentUser = UserManager.currentUser

                // if all the posts are not downloaded for some reason, download it now
                if (currentUser.postsCount.toInt() != it.size) {
                    FireUtility.downloadAllUserPosts { it1 ->
                        val allPostsResult = it1 ?: return@downloadAllUserPosts
                        when (allPostsResult) {
                            is Result.Error -> viewModel.setCurrentError(allPostsResult.exception)
                            is Result.Success -> {
                                viewModel.insertPosts(allPostsResult.data)
                            }
                        }
                    }
                }
                postListAdapter.submitList(it)
            }
        }

    }

    companion object {

        const val TAG = "PostListFragment"

        fun newInstance(user: User) = PostListFragment().apply {
            arguments = bundleOf(USER to user)
        }
    }

    override fun onInviteClick(post: Post, receiverId: String, onFailure: () -> Unit) {
        val currentUser = UserManager.currentUser
        if (post.contributors.size < 5 || currentUser.premiumState.toInt() == 1) {
            val title = post.name
            val content = currentUser.name + " has invited you to join their post: ${post.name}"
            val notification = Notification.createNotification(
                content,
                receiverId,
                type = -1,
                postId = post.id,
                title = title
            )

            FireUtility.inviteUserToPost(post, receiverId, notification.id) {
                if (it.isSuccessful) {
                    if (notification.senderId != notification.receiverId) {
                        FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                            if (error != null) {
                                viewModel.setCurrentError(error)
                            } else {
                                if (!exists) {
                                    FireUtility.sendNotification(notification) { it1 ->
                                        if (it1.isSuccessful) {
                                            viewModel.insertNotifications(notification)
                                        } else {
                                            viewModel.setCurrentError(it1.exception)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {

            onFailure()

            val upgradeMsg = getString(R.string.upgrade_plan_imsg)
            val frag = MessageDialogFragment.builder(upgradeMsg)
                .setPositiveButton(getString(R.string.upgrade)) { _, _ ->
                    (activity as MainActivity?)?.showSubscriptionFragment()
                }
                .setNegativeButton(getString(R.string.cancel)){ a, _ ->
                    a.dismiss()
                }.build()

            frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
        }
    }

    override fun onRevokeInviteClick(invite: PostInvite, onFailure: () -> Unit) {
        FireUtility.revokeInvite(invite) {
            if (!it.isSuccessful) {
                Snackbar.make(binding.root, "Something went wrong while trying to revoke post invite", Snackbar.LENGTH_LONG).show()
                viewModel.setCurrentError(it.exception)
                onFailure()
            }
        }
    }


}