package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.ProjectRequestListener

class ProjectRequestViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private lateinit var binding: RequestItemBinding
    private val projectRequestListener = view.context as ProjectRequestListener

    fun bind(projectRequest: ProjectRequest?) {
        if (projectRequest != null) {
            binding = RequestItemBinding.bind(view)

            binding.requestAcceptProgress.show()
            binding.requestAccept.disappear()
            binding.requestCancel.disappear()

            binding.requestTime.text = getTextForTime(projectRequest.createdAt)

            onProjectRequestUpdated(projectRequest)
        }
    }

    private fun onProjectRequestUpdated(projectRequest: ProjectRequest) {
        binding.requestAcceptProgress.hide()
        binding.requestAccept.show()
        binding.requestCancel.show()

        binding.projectName.text = projectRequest.project?.name
        binding.requestImg.setImageURI(projectRequest.project?.images?.firstOrNull())
        binding.requestContent.text = projectRequest.sender?.name + " wants to join your project."

        binding.requestAccept.setOnClickListener {
            binding.requestAcceptProgress.show()
            binding.requestAccept.disappear()
            projectRequestListener.onProjectRequestAccept(projectRequest)
        }


        binding.requestCancel.setOnClickListener {
            projectRequestListener.onProjectRequestCancel(projectRequest)
        }
    }

    companion object {

        fun newInstance(parent: ViewGroup): ProjectRequestViewHolder {
            return ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false))
        }

    }

}