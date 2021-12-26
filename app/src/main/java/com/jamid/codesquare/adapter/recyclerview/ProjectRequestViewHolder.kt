package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.Result
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
        /*if (projectRequest.sender != null && projectRequest.project != null) {


            // update the new project request to local database
            projectRequestListener.updateProjectRequest(projectRequest)
        } else {
            FireUtility.getProject(projectRequest.projectId) {
                when (it) {
                    is Result.Error -> Log.e(TAG, it.exception.localizedMessage.orEmpty())
                    is Result.Success -> {
                        FireUtility.getUser(projectRequest.senderId) { it1 ->
                            when (it1) {
                                is Result.Error -> Log.e(TAG, it1.exception.localizedMessage.orEmpty())
                                is Result.Success -> {
                                    val project = processProjects(arrayOf(it.data)).first()
                                    projectRequest.project = project
                                    binding.projectName.text = project.name
                                    val sender = it1.data
                                    projectRequest.sender = sender
                                    binding.requestImg.setImageURI(sender.photo)
                                    val contentText = "${sender.name} has requested to join your project."
                                    binding.requestContent.text = contentText
                                    onProjectRequestUpdated(projectRequest)
                                }
                                null -> projectRequestListener.onProjectRequestProjectDeleted(projectRequest)
                            }
                        }
                    }
                    null -> projectRequestListener.onProjectRequestProjectDeleted(projectRequest)
                }
            }
        }*/
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

        private const val TAG = "RequestViewHolder"

        fun newInstance(parent: ViewGroup): ProjectRequestViewHolder {
            return ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false))
        }

    }

}