package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VagueProjectAdapter(private val projectList: List<String>, private val scope: CoroutineScope, private val onLoad: suspend (id: String) -> Project?): RecyclerView.Adapter<ProjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        return ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.project_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val projectId = projectList[position]
        scope.launch {
            val project = onLoad(projectId)
            holder.bind(project)
        }
    }

    override fun getItemCount(): Int {
        return projectList.size
    }

    companion object {
    }

}