package com.jamid.codesquare.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jamid.codesquare.data.Project

class UpdateProjectViewModel: ViewModel() {

    private val _currentProject = MutableLiveData<Project>().apply { value = null }
    val currentProject: LiveData<Project> = _currentProject

    fun setCurrentProject(project: Project) {
        _currentProject.postValue(project)
    }

    fun setCurrentProjectImages(images: List<String>) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.images = images
            _currentProject.postValue(currentProject)
        }
    }

    fun addImagesToExistingProject(newImages: List<String>) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            val finalImagesContainer = mutableListOf<String>()
            finalImagesContainer.addAll(currentProject.images)
            finalImagesContainer.addAll(newImages)
            currentProject.images = finalImagesContainer
            _currentProject.postValue(currentProject)
        }
    }

    fun removeAllImagesFromProject() {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.images = emptyList()
            _currentProject.postValue(currentProject)
        }
    }

    fun removeImageAtPosition(pos: Int) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            val newImages = currentProject.images.toMutableList()
            newImages.removeAt(pos)
            currentProject.images = newImages
            _currentProject.postValue(currentProject)
        }
    }

    fun setProjectName(name: String) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.name = name
            _currentProject.postValue(currentProject)
        }
    }

    fun setProjectContent(content: String) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.content = content
            _currentProject.postValue(currentProject)
        }
    }

    fun setProjectTags(tags: List<String>) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.tags = tags
            _currentProject.postValue(currentProject)
        }
    }

    fun setProjectLinks(links: List<String>) {
        val currentProject = currentProject.value
        if (currentProject != null) {
            currentProject.sources = links
            _currentProject.postValue(currentProject)
        }
    }

    fun clearCurrentProject() {
        _currentProject.postValue(null)
    }

}