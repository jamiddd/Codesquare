package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SendInviteViewHolder
import com.jamid.codesquare.adapter.recyclerview.SendInvitesAdapter
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.PostInvite
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentSendInviteBinding
import com.jamid.codesquare.listeners.PostMiniItemClickListener
import kotlinx.coroutines.flow.map

class SendInviteFragment: BottomSheetPagingFragment<FragmentSendInviteBinding, Post, SendInviteViewHolder>(), PostMiniItemClickListener {

    private lateinit var receiver: User

    init {
        fullscreen = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = arguments?.getParcelable(USER) ?: return
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val subFieldName = "$CREATOR.$USER_ID"

        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)
            .whereEqualTo(subFieldName, currentUser.id)

        binding.postsList.apply {
            adapter = myPagingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        getItems(viewLifecycleOwner) {
            viewModel.getCurrentUserPosts(query).map {
                it.map { p ->
                    p
                }
            }
        }

       /* viewModel.getCurrentUserPosts().observe(viewLifecycleOwner) {
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
        }*/

    }

    companion object {

        const val TAG = "PostListFragment"

        fun newInstance(user: User) = SendInviteFragment().apply {
            arguments = bundleOf(USER to user)
        }
    }

    override fun onInviteClick(post: Post, receiverId: String, onFailure: () -> Unit) {
        val currentUser = UserManager.currentUser
        val title = post.name
        val content = currentUser.name + " has invited you to collab with them in ${post.name}"
        val notification = Notification.createNotification(
            content,
            receiverId,
            type = 2,
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

    override fun onCreateBinding(inflater: LayoutInflater): FragmentSendInviteBinding {
        return FragmentSendInviteBinding.inflate(inflater)
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post, SendInviteViewHolder> {
        return SendInvitesAdapter(receiver.id, this)
    }

    override fun onPagingDataChanged(itemCount: Int) {
        if (itemCount == 0) {
            binding.noPostsText.text = "No posts to sent invite to"
            binding.noPostsText.show()
        } else {
            binding.noPostsText.hide()
        }
    }

    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {
        //
    }

    override fun onAdapterStateChanged(state: AdapterState, error: Throwable?) {
        setDefaultPagingLayoutBehavior(state, error, null, binding.noPostsText, binding.postsList, null, null)
    }


}