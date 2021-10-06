package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.ktx.auth
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
    val commentDao = db.commentDao()

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
        for (chatChannel in chatChannels) {
            if (chatChannel.lastMessage != null) {
                val sender = getUser(chatChannel.lastMessage?.senderId.orEmpty())
                if (sender != null) {
                    val short = UserMinimal(sender.id, sender.name, sender.photo, sender.username)
                    chatChannel.lastMessage?.sender = short
                }
            }
        }
        chatChannelDao.insert(chatChannels)
    }

    suspend fun insertMessages(messages: List<Message>) {
        Log.d(TAG, "Inserting messages")
        for (message in messages) {
            val user = getUser(message.senderId)
            if (user != null) {
                val short = UserMinimal(user.id, user.name, user.photo, user.username)
                message.sender = short
            } else {
                throw NullPointerException("The user doesn't exist for a message with user id - ${message.senderId}")
            }
        }
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

    suspend fun insertProjectRequests(projectRequests: List<ProjectRequest>) {
        projectRequestDao.insert(projectRequests)
    }

    suspend fun getProject(projectId: String): Project? {
        return projectDao.getProject(projectId)
    }

    suspend fun getUser(userId: String): User? {
        return userDao.getUser(userId)
    }

    suspend fun getChatChannel(chatChannel: String): ChatChannel? {
        return chatChannelDao.getChatChannel(chatChannel)
    }

    suspend fun deleteProjectRequest(projectRequest: ProjectRequest) {
        projectRequestDao.deleteProjectRequest(projectRequest.requestId)
    }

    suspend fun getComment(parentId: String): Comment? {
        return commentDao.getCommentById(parentId)
    }

    suspend fun insertComment(comment: Comment) {
        commentDao.insert(comment)
    }

    suspend fun insertComments(comments: List<Comment>) {
        val currentUser = currentUser.value!!
        for (comment in comments) {
            comment.isLiked = currentUser.likedComments.contains(comment.commentId)
        }
        commentDao.insert(comments)
    }

    suspend fun onCommentLiked(comment: Comment) {
        val currentUser = currentUser.value
        if (currentUser != null) {

            val isLiked = currentUser.likedComments.contains(comment.commentId)

            val result = if (isLiked) {
                // dislike
                FireUtility.dislikeComment(currentUser.id, comment)
            } else {
                // like
                FireUtility.likeComment(currentUser.id, comment)
            }

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "Error while liking or disliking a comment -> " + result.exception.localizedMessage!!)
                }
                is Result.Success -> {
                    // insert the new project
                    val existingList = currentUser.likedComments.toMutableList()

                    if (isLiked) {
                        comment.likes = comment.likes - 1
                        comment.isLiked = false
                        existingList.remove(comment.commentId)
                    } else {
                        comment.likes = comment.likes + 1
                        comment.isLiked = true
                        existingList.add(comment.commentId)
                    }

                    Log.d(TAG, "${comment.likes} -- ${comment.isLiked}")

                    commentDao.insert(comment)

                    // insert the new user
                    currentUser.likedComments = existingList
                    userDao.insert(currentUser)
                }
            }

        }
    }

    suspend fun clearComments() {
        commentDao.clearTable()
    }

    suspend fun insertUsers(users: List<User>) {
        for (user in users) {
            user.isCurrentUser = user.id == Firebase.auth.currentUser?.uid.orEmpty()
        }
        userDao.insert(users)
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