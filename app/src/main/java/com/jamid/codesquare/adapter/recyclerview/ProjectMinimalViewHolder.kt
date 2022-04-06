package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.ProjectMinimal2
import com.jamid.codesquare.databinding.ProjectMiniItemBinding
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.listeners.ProjectClickListener

class ProjectMinimalViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val projectClickListener = view.context as ProjectClickListener
    private lateinit var binding: ProjectMiniItemBinding

    fun bind(project: ProjectMinimal2) {

        binding = ProjectMiniItemBinding.bind(view)

        binding.miniProjectThumbnail.setImageURI(project.images.first())
        binding.miniProjectUserImg.setImageURI(project.creator.photo)

        binding.miniProjectContent.text = project.content
        binding.miniProjectName.text = project.name

        val infoText = "${project.creator.name} • ${getTextForTime(project.createdAt)}"
        binding.miniProjectInfo.text = infoText

        binding.miniProjectOption.setOnClickListener {
            projectClickListener.onProjectOptionClick(project)
        }

        binding.root.setOnClickListener {
            projectClickListener.onProjectClick(project)
        }
    }
}