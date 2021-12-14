package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import com.jamid.codesquare.processProjects

class VagueProjectAdapter(val currentUser: User, private val projectList: List<String>): RecyclerView.Adapter<ProjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val projectId = projectList[position]
        FireUtility.getProject(projectId) {
            when (it) {
                is Result.Error -> {
                    Log.e(TAG, it.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    val project = processProjects(currentUser, listOf(it.data))
                    holder.bind(project[0])
                }
                null -> {
                    Log.d(TAG, "Document doesn't exist.")
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return projectList.size
    }

    companion object {
        private val TAG = VagueProjectAdapter::class.simpleName
    }

}