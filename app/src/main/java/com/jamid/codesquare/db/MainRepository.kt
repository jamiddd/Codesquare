package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.*

class MainRepository(db: CodesquareDatabase) {

    val projectDao = db.projectDao()
    val userDao = db.userDao()
    val chatChannelDao = db.chatChannelDao()
    val messageDao = db.messageDao()
    val projectRequestDao = db.projectRequestDao()

    val currentUser: LiveData<User> = userDao.currentUser()

    suspend fun insertProjects(projects: List<Project>) {

        val currentUser = currentUser.value!!

        for (project in projects) {
            project.isMadeByMe = project.creator.userId == currentUser.id
            project.isLiked = currentUser.likedProjects.contains(project.id)
            project.isSaved = currentUser.savedProjects.contains(project.id)

            val set1 = project.requests.toSet()
            val set2 = currentUser.projectRequests.toSet()
            val intersection = set1.intersect(set2)

            project.isRequested = intersection.isNotEmpty()
            project.isCollaboration = currentUser.collaborations.contains(project.id)
        }
        projectDao.insert(projects)
    }

    suspend fun insertCurrentUser(user: User) {
        user.isCurrentUser = true
        userDao.insert(user)
    }

    suspend fun insertUser(localUser: User) {
        userDao.insert(localUser)
    }

    suspend fun clearDatabases() {
        userDao.clearTable()
        projectDao.clearTable()
    }

    suspend fun onLikePressed(project: Project) {
        val currentUser = currentUser.value
        if (currentUser != null) {

            val isLiked = currentUser.likedProjects.contains(project.id)

            val result = if (isLiked) {
                // dislike
                FireUtility.dislikeProject(currentUser.id, project)
            } else {
                // like
                FireUtility.likeProject(currentUser.id, project)
            }

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "Error while liking or disliking a post -> " + result.exception.localizedMessage!!)
                }
                is Result.Success -> {
                    // insert the new project

                    val existingList = currentUser.likedProjects.toMutableList()

                    if (isLiked) {
                        project.likes = project.likes - 1
                        project.isLiked = false
                        existingList.remove(project.id)
                    } else {
                        project.likes = project.likes + 1
                        project.isLiked = true
                        existingList.add(project.id)
                    }

                    Log.d(TAG, "${project.likes} -- ${project.isLiked}")

                    projectDao.insert(project)

                    // insert the new user
                    currentUser.likedProjects = existingList
                    userDao.insert(currentUser)
                }
            }

        }
    }

    suspend fun onSavePressed(project: Project) {
        val currentUser = currentUser.value
        if (currentUser != null) {
            val isSaved = currentUser.savedProjects.contains(project.id)

            val result = if (isSaved) {
                // unSave
                FireUtility.unSaveProject(currentUser.id, project)
            } else {
                // save
                FireUtility.saveProject(currentUser.id, project)
            }

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "Error while saving or unSaving a post -> " + result.exception.localizedMessage!!)
                }
                is Result.Success -> {
                    // insert the new project

                    val existingList = currentUser.savedProjects.toMutableList()

                    if (isSaved) {
                        project.isSaved = false
                        existingList.remove(project.id)
                    } else {
                        project.isSaved = true
                        existingList.add(project.id)
                    }

                    projectDao.insert(project)

                    // insert the new user
                    currentUser.savedProjects = existingList
                    userDao.insert(currentUser)
                }
            }
        }
    }

    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        chatChannelDao.insert(chatChannels)
    }

    suspend fun insertMessages(messages: List<Message>) {
        messageDao.insert(messages)
    }

    suspend fun onJoinProject(project: Project) {
        val currentUser = currentUser.value
        if (currentUser != null) {

            val set1 = currentUser.projectRequests.toSet()
            val set2 = project.requests.toSet()

            val newSet = set1.intersect(set2)

            if (newSet.isNotEmpty()) {
                val requestId = newSet.first()
                FireUtility.undoJoinProject(currentUser.id, project.id, requestId)

                val projectRequestsList = currentUser.projectRequests.toMutableList()
                projectRequestsList.remove(requestId)
                currentUser.projectRequests = projectRequestsList

                val requestsList = project.requests.toMutableList()
                requestsList.remove(requestId)
                project.requests = requestsList

                projectRequestDao.deleteProjectRequest(requestId)

            } else {
                // join project
                when (val sendRequestResult = FireUtility.joinProject(currentUser, project)) {
                    is Result.Error -> {
                        Log.e(TAG, sendRequestResult.exception.localizedMessage.orEmpty())
                    }
                    is Result.Success -> {
                        val projectRequest = sendRequestResult.data

                        val projectRequestsList = currentUser.projectRequests.toMutableList()
                        projectRequestsList.add(projectRequest.requestId)
                        currentUser.projectRequests = projectRequestsList

                        val requestsList = project.requests.toMutableList()
                        requestsList.add(projectRequest.requestId)
                        project.requests = requestsList

                        insertCurrentUser(currentUser)

                        insertProjects(listOf(project))

                        projectRequestDao.insert(sendRequestResult.data)
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG = "MainRepository"
        @Volatile private var instance: MainRepository? = null

        fun getInstance(db: CodesquareDatabase): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(db)
            }
        }

    }

}