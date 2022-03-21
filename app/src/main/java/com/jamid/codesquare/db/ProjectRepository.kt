package com.jamid.codesquare.db

import androidx.lifecycle.LiveData
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Project
import kotlin.random.Random

class ProjectRepository(a: CodesquareDatabase): BaseRepository(a) {

    private val projectDao = database.projectDao()

    suspend fun getProject(projectId: String): Project? {
        return projectDao.getProject(projectId)
    }

    fun getReactiveProject(projectId: String): LiveData<Project> {
        return projectDao.getReactiveProject(projectId)
    }

    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }

    suspend fun updateProjects(projects: List<Project>) {
        // replace by default
        insertProjects(projects, false)
    }

    suspend fun insertProject(project: Project, shouldProcess: Boolean = true) {
        insertProjects(listOf(project), shouldProcess)
    }

    suspend fun insertProjects(projects: List<Project>, shouldProcess: Boolean = true) {
        val currentUser = UserManager.currentUser
        val newProjects = projects.toMutableList()

        if (currentUser.premiumState.toInt() == -1) {
            val numberOfAds = (projects.size / 3)

            val indexes = mutableListOf<Int>()
            for (i in 0 until numberOfAds) {
                indexes.add(Random.nextInt(1, projects.size - 1))
            }

            for (i in indexes) {
                val newProject = Project()
                newProject.isAd = true
                newProject.createdAt = projects[i].createdAt
                newProjects.add(i, newProject)
            }
        }

        if (shouldProcess) {
            val n = processProjects(newProjects)
            projectDao.insert(n)
        } else {
            projectDao.insert(newProjects)
        }
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteAdProjects() {
        projectDao.deleteAdProjects()
    }

    private fun processProjects(projects: List<Project>): List<Project> {
        val currentUser = UserManager.currentUser
        for (project in projects) {
            project.isMadeByMe = project.creator.userId == currentUser.id
            project.isBlocked = project.blockedList.contains(currentUser.id)
            project.isLiked = currentUser.likedProjects.contains(project.id)
            project.isSaved = currentUser.savedProjects.contains(project.id)

            val set1 = project.requests.toSet()
            val set2 = currentUser.projectRequests.toSet()
            val intersection = set1.intersect(set2)

            project.isRequested = intersection.isNotEmpty()
            project.isCollaboration = currentUser.collaborations.contains(project.id)

            project.isArchived = currentUser.archivedProjects.contains(project.id)
        }

        return projects
    }

    fun getCurrentUserProjects(): LiveData<List<Project>> {
        return projectDao.getCurrentUserProjects()
    }

    suspend fun disableLocationBasedProjects() {
        projectDao.disableLocationBasedProjects()
    }

    suspend fun clearProjects() {
        projectDao.clearTable()
    }

}