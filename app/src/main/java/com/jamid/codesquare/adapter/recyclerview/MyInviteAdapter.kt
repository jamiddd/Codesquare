package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.PostInviteComparator
import com.jamid.codesquare.data.PostInvite
import com.jamid.codesquare.databinding.PostListItemBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.PostMiniItemClickListener
import com.jamid.codesquare.show

class MyInviteAdapter(private val inviteClickListener: PostMiniItemClickListener): ListAdapter<PostInvite, MyInviteAdapter.MyInviteViewHolder>(PostInviteComparator()){
    init {
        Log.d("Something", "Simple: ")
    }
    inner class MyInviteViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: PostListItemBinding

        fun bind(invite: PostInvite) {
            binding = PostListItemBinding.bind(view)

            binding.postMiniImage.setImageURI(invite.post.image)
            binding.postMiniName.text = invite.post.name

            FireUtility.getUser(invite.receiverId) { user ->
                if (user != null) {
                    binding.postMiniInviteHelperText.show()
                    val t = "Invite sent to ${user.name}"
                    binding.postMiniInviteHelperText.text = t
                }
            }

            binding.postMiniInviteBtn.text = "Revoke"
            binding.postMiniInviteBtn.setOnClickListener {
                binding.inviteBtnProgress.show()
                binding.postMiniInviteBtn.hide()

                inviteClickListener.onRevokeInviteClick(invite) {
                    binding.inviteBtnProgress.hide()
                    binding.postMiniInviteBtn.show()
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyInviteViewHolder {
        return MyInviteViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.post_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyInviteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private const val TAG = "MyInviteAdapter"
    }

}