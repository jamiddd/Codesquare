package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.comparators.PostComparator
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.PostListItemBinding
import com.jamid.codesquare.listeners.PostMiniItemClickListener

class SendInvitesAdapter(
    private val receiverId: String,
    private val postMiniItemClickListener: PostMiniItemClickListener
) : PagingDataAdapter<Post, SendInviteViewHolder>(PostComparator()) {
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SendInviteViewHolder {
        return SendInviteViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.post_list_item, parent, false),
            receiverId,
            postMiniItemClickListener
        )
    }

    override fun onBindViewHolder(holder: SendInviteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private const val TAG = "ProjectListAdapter"
    }

}


class SendInviteViewHolder(
    val view: View,
    private val receiverId: String,
    private val postMiniItemClickListener: PostMiniItemClickListener
) : RecyclerView.ViewHolder(view) {

    fun bind(post: Post?) {

        if (post == null)
            return

        val binding = PostListItemBinding.bind(view)
        binding.postMiniImage.setImageURI(post.mediaList.firstOrNull())
        binding.postMiniName.text = post.name

        binding.inviteBtnProgress.show()
        binding.postMiniInviteBtn.disappear()

        // checking if the user is already a contributor
        if (post.contributors.contains(receiverId)) {
            // hide invite button
            binding.inviteBtnProgress.hide()
            binding.postMiniInviteBtn.hide()
        } else {
            // if the user is not already a contributor, check if the user has sent request to the current user
            FireUtility.getPostRequest(post.id, receiverId) {
                when (it) {
                    is Result.Error -> Log.e(TAG, it.exception.localizedMessage.orEmpty())
                    is Result.Success -> {
                        // since there is already a request by this user, somehow need to show the user that there is a request,
                        // or make accept reject button right here, maybe later
                        binding.inviteBtnProgress.hide()
                        binding.postMiniInviteBtn.hide()
                    }
                    null -> {
                        // there is no existing post request by this user, check if we have already sent invite to this user
                        FireUtility.getExistingInvite(
                            post.id,
                            receiverId,
                            UserManager.currentUserId
                        ) { it1 ->

                            binding.inviteBtnProgress.hide()
                            binding.postMiniInviteBtn.show()

                            when (it1) {
                                is Result.Error -> Log.e(
                                    TAG,
                                    it1.exception.localizedMessage.orEmpty()
                                )
                                is Result.Success -> {
                                    // yes, we have already sent invite to this user
                                    val invite = it1.data
                                    binding.postMiniInviteBtn.text =
                                        view.context.getString(R.string.revoke)
                                    binding.postMiniInviteBtn.icon = null

                                    // setting the button to revoke this invite on click
                                    binding.postMiniInviteBtn.setOnClickListener {
                                        postMiniItemClickListener.onRevokeInviteClick(
                                            invite
                                        ) {
                                            binding.inviteBtnProgress.hide()
                                            binding.postMiniInviteBtn.show()
                                        }
                                        bind(post)
                                    }
                                }
                                null -> {
                                    // no existing invite found, this is the only condition when we can send invite
                                    binding.postMiniInviteBtn.text =
                                        view.context.getString(R.string.invite)
                                    binding.postMiniInviteBtn.icon =
                                        ContextCompat.getDrawable(
                                            view.context,
                                            R.drawable.ic_round_how_to_reg_24
                                        )

                                    // setting the button to invite this user on click
                                    binding.postMiniInviteBtn.setOnClickListener {
                                        postMiniItemClickListener.onInviteClick(
                                            post,
                                            receiverId
                                        ) {
                                            binding.inviteBtnProgress.hide()
                                            binding.postMiniInviteBtn.show()
                                        }
                                        bind(post)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}