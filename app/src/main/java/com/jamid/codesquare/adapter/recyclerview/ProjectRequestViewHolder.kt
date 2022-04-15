package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.databinding.RequestItemAltBinding
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.ProjectRequestListener

class ProjectRequestViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private lateinit var binding: ViewBinding
    private val projectRequestListener = view.context as ProjectRequestListener
    var isMyRequests: Boolean = false

    fun bind(projectRequest: ProjectRequest?) {
        if (projectRequest != null) {
            if (isMyRequests) {
                binding = RequestItemAltBinding.bind(view)
                bindForCurrentUser(binding as RequestItemAltBinding, projectRequest)
            } else {
                binding = RequestItemBinding.bind(view)
                bindForOtherUsers(binding as RequestItemBinding, projectRequest)
            }
        }
    }

    private fun bindForOtherUsers(binding: RequestItemBinding, projectRequest: ProjectRequest) {
        binding.requestProgress.hide()
        binding.requestPrimaryAction.show()
        binding.requestSecondaryAction.show()
        binding.requestProjectName.text = projectRequest.project.name
        binding.requestImg.setImageURI(projectRequest.project.image)
        val content = projectRequest.sender.name + " wants to join your project."
        binding.requestContent.text = content

        binding.requestPrimaryAction.setOnClickListener {
            onActionStarted(binding)
            projectRequestListener.onProjectRequestAccept(projectRequest) {
                onActionEnded(binding)
            }
        }

        binding.requestSecondaryAction.setOnClickListener {
            onActionStarted(binding)
            projectRequestListener.onProjectRequestCancel(projectRequest)
        }

        binding.requestTime.text = getTextForTime(projectRequest.createdAt)

        binding.root.setOnClickListener {
            projectRequestListener.onProjectRequestClick(projectRequest)
        }
    }

    private fun onActionEnded(binding: RequestItemBinding) {
        binding.requestProgress.hide()
        binding.requestSecondaryAction.show()
        binding.requestPrimaryAction.show()
    }

    private fun onActionStarted(binding: RequestItemBinding) {
        binding.requestProgress.show()
        binding.requestSecondaryAction.disappear()
        binding.requestPrimaryAction.disappear()
    }

    private fun bindForCurrentUser(binding: RequestItemAltBinding, projectRequest: ProjectRequest) {
        binding.requestProgress.hide()
        binding.requestContent.hide()
        binding.requestImg.setImageURI(projectRequest.project.image)
        binding.requestProjectName.text = projectRequest.project.name

        binding.requestPrimaryAction.apply {
            text = view.context.getText(R.string.undo)
            show()

            setOnClickListener {
                projectRequestListener.onProjectRequestUndo(projectRequest)
            }
        }

        binding.requestTime.text = getTextForTime(projectRequest.createdAt)

        binding.root.setOnClickListener {
            projectRequestListener.onProjectRequestClick(projectRequest)
        }

    }

    companion object {

        fun newInstance(parent: ViewGroup, ism: Boolean): ProjectRequestViewHolder {
            return if (ism) {
                ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item_alt, parent, false)).apply {
                    isMyRequests = ism
                }
            } else {
                ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)).apply {
                    isMyRequests = ism
                }
            }
        }

    }

}