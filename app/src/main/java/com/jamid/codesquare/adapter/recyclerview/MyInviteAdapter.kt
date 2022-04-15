package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.ProjectInviteComparator
import com.jamid.codesquare.data.ProjectInvite
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.ProjectListItemBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener
import com.jamid.codesquare.show

class MyInviteAdapter(private val inviteClickListener: ProjectMiniItemClickListener): ListAdapter<ProjectInvite, MyInviteAdapter.MyInviteViewHolder>(ProjectInviteComparator()){

    inner class MyInviteViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: ProjectListItemBinding

        fun bind(invite: ProjectInvite) {
            binding = ProjectListItemBinding.bind(view)

            binding.projectMiniImage.setImageURI(invite.project.image)
            binding.projectMiniName.text = invite.project.name

            FireUtility.getUser(invite.receiverId) {
                val result = it ?: return@getUser

                when (result) {
                    is Result.Error -> Log.e(TAG, "bind: ${result.exception.localizedMessage}")
                    is Result.Success -> {
                        binding.projectMiniInviteHelperText.show()
                        val receiver = result.data
                        val t = "Invite sent to ${receiver.name}"
                        binding.projectMiniInviteHelperText.text = t
                    }
                }

            }

            binding.projectMiniInviteBtn.text = "Revoke"
            binding.projectMiniInviteBtn.setOnClickListener {
                binding.inviteBtnProgress.show()
                binding.projectMiniInviteBtn.hide()

                inviteClickListener.onRevokeInviteClick(invite) {
                    binding.inviteBtnProgress.hide()
                    binding.projectMiniInviteBtn.show()
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyInviteViewHolder {
        return MyInviteViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.project_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyInviteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private const val TAG = "MyInviteAdapter"
    }

}