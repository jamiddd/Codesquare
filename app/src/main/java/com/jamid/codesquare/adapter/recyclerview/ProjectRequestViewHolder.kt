package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.ProjectRequestListener
import kotlinx.coroutines.launch

class ProjectRequestViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private lateinit var binding: RequestItemBinding
    private val projectRequestListener = view.context as ProjectRequestListener
    var isMyRequests: Boolean = false

    fun bind(projectRequest: ProjectRequest?) {
        if (projectRequest != null) {
            binding = RequestItemBinding.bind(view)
            binding.requestTime.text = getTextForTime(projectRequest.createdAt)

            binding.root.setOnClickListener {
                projectRequestListener.onProjectRequestClick(projectRequest)
            }

            if (isMyRequests) {
                bindForCurrentUser(projectRequest)
            } else {
                bindForOtherUsers(projectRequest)
            }
        }
    }

    private fun bindForOtherUsers(projectRequest: ProjectRequest) {
        binding.requestProgress.hide()
        binding.requestPrimaryAction.show()
        binding.requestSecondaryAction.show()
        binding.requestProjectName.text = projectRequest.project?.name
        binding.requestImg.setImageURI(projectRequest.project?.images?.firstOrNull())
        binding.requestContent.text = projectRequest.sender?.name + " wants to join your project."

        binding.requestPrimaryAction.setOnClickListener {
            onActionStarted()
            projectRequestListener.onProjectRequestAccept(projectRequest)
        }

        binding.requestSecondaryAction.setOnClickListener {
            onActionStarted()
            projectRequestListener.onProjectRequestCancel(projectRequest)
        }
    }

    private fun onActionStarted() {
        binding.requestProgress.show()
        binding.requestSecondaryAction.disappear()
        binding.requestPrimaryAction.disappear()
    }

    private fun bindForCurrentUser(projectRequest: ProjectRequest) {
        binding.requestProgress.hide()
        binding.requestSecondaryAction.hide()
        binding.requestContent.hide()
        binding.requestImg.setImageURI(projectRequest.project?.images?.first())
        binding.requestProjectName.text = projectRequest.project?.name

        binding.requestPrimaryAction.apply {
            text = "Undo"
            show()

            setOnClickListener {
                projectRequestListener.onProjectRequestUndo(projectRequest)
            }
        }

        val dy = view.context.resources.getDimension(R.dimen.generic_len).toInt()

        val set = ConstraintSet()
        set.clone(binding.requestRoot)

        set.clear(binding.requestContentContainer.id)
        set.clear(binding.requestImg.id)
        set.clear(binding.requestPrimaryAction.id)
        set.clear(binding.requestSecondaryAction.id)

        set.applyTo(binding.requestRoot)

        binding.requestContentContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToTop = binding.requestImg.id
            startToEnd = binding.requestImg.id
            endToStart = binding.requestPrimaryAction.id
            bottomToBottom = binding.requestImg.id

            width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            height = ConstraintLayout.LayoutParams.WRAP_CONTENT

            horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

            setMargins(dy)
        }

        binding.requestImg.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToStart = binding.requestRoot.id
            topToTop = binding.requestContentContainer.id
            endToStart = binding.requestContentContainer.id
            bottomToBottom = binding.requestRoot.id
            setMargins(dy)
        }

        binding.requestPrimaryAction.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToEnd = binding.requestContentContainer.id
            topToTop = binding.requestContentContainer.id
            endToEnd = binding.requestRoot.id
            bottomToBottom = binding.requestContentContainer.id

            setMargins(dy)
        }

        binding.requestRoot.setPadding(dy)

    }

    companion object {

        fun newInstance(parent: ViewGroup, ism: Boolean): ProjectRequestViewHolder {
            return ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)).apply {
                isMyRequests = ism
            }
        }

    }

}