package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.User
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.listeners.ProjectRequestListener

class ProjectRequestViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val projectTitle = view.findViewById<TextView>(R.id.project_title)
    private val requestContent = view.findViewById<TextView>(R.id.request_content)
    private val requestAcceptBtn = view.findViewById<Button>(R.id.request_accept)
    private val requestCancelBtn = view.findViewById<Button>(R.id.request_cancel)
    private val requestTime = view.findViewById<TextView>(R.id.request_time)
    private val projectImage = view.findViewById<SimpleDraweeView>(R.id.project_img)

    private val projectRequestListener = view.context as ProjectRequestListener

    fun bind(projectRequest: ProjectRequest?) {
        if (projectRequest != null) {

            Firebase.firestore.collection("projects").document(projectRequest.projectId)
                .get()
                .addOnSuccessListener {
                    if (it != null && it.exists()) {
                        val project = it.toObject(Project::class.java)!!

                        projectRequest.project = project

                        projectTitle.text = project.title
                        projectImage.setImageURI(project.images.first())

                        Firebase.firestore.collection("users").document(projectRequest.senderId)
                            .get()
                            .addOnSuccessListener { it1 ->
                                if (it1 != null && it1.exists()) {
                                    val sender = it1.toObject(User::class.java)!!

                                    projectRequest.sender = sender

                                    val contentText = "${sender.name} has requested to join your project."
                                    requestContent.text = contentText

                                    requestTime.text = getTextForTime(projectRequest.createdAt)

                                    requestAcceptBtn.setOnClickListener {
                                        projectRequestListener.onProjectRequestAccept(projectRequest)
                                    }

                                    requestCancelBtn.setOnClickListener {
                                        projectRequestListener.onProjectRequestCancel(projectRequest)
                                    }

                                }
                            }.addOnFailureListener { it1 ->
                                Log.e(TAG, it1.localizedMessage.orEmpty())
                            }

                    }
                }.addOnFailureListener {
                    Log.e(TAG, it.localizedMessage.orEmpty())
                }

        }
    }

    companion object {

        private const val TAG = "RequestViewHolder"

        fun newInstance(parent: ViewGroup): ProjectRequestViewHolder {
            return ProjectRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false))
        }

    }

}