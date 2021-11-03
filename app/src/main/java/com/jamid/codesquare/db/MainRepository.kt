package com.jamid.codesquare.db

import android.media.Image
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.*
import com.jamid.codesquare.document
import com.jamid.codesquare.image
import java.io.File

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
        Log.d(TAG, "Inserting current user")
        user.isCurrentUser = true
        userDao.insert(user)
    }

    suspend fun insertUser(localUser: User) {
        Log.d(TAG, "Inserting 1 user")
        if (localUser.id == currentUser.value?.id) {
            insertCurrentUser(localUser)
        } else {
            userDao.insert(localUser)
        }
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

    suspend fun getAllLocalChatChannels(): List<ChatChannel> {
        return chatChannelDao.allChannels().orEmpty()
    }

    suspend fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) {
        chatChannelDao.insert(channels)
    }


    // check if the message already has the user, in which case no need to get the user
    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        for (chatChannel in chatChannels) {

            val lastMessage = chatChannel.lastMessage

            if (lastMessage != null && lastMessage.sender.isEmpty()) {
                val sender = getUser(lastMessage.senderId)
                if (sender != null) {
                    lastMessage.sender = sender
                } else {

                    val ref = Firebase.firestore.collection("users")
                        .document(lastMessage.senderId)

                    when (val result = FireUtility.getDocument(ref))  {
                        is Result.Error -> {
                            Log.e(TAG, "Something went wrong while getting user data for chatChannel")
                        }
                        is Result.Success -> {
                            lastMessage.sender = result.data.toObject(User::class.java)!!
                        }
                    }
                }
            }
        }
        chatChannelDao.insert(chatChannels)
    }

    suspend fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        for (message in messages) {
            val user = getUser(message.senderId)
            if (user != null) {
                message.sender = user
            } else {
                throw NullPointerException("The user doesn't exist for a message with user id - ${message.senderId}")
            }

            if (message.type == image) {
                val name = message.content + message.metadata!!.ext
                val f = File(imagesDir, name)
                message.isDownloaded = f.exists()
            }

            if (message.type == document) {
                val name = message.content + message.metadata!!.ext
                val f = File(documentsDir, name)
                message.isDownloaded = f.exists()
            }
        }
        return messages
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

    suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
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
        Log.d(TAG, "Inserting multiple users")
        userDao.insert(users)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    suspend fun updateMessages(messages: List<Message>) {
        messageDao.update(messages)
    }

    suspend fun getProjectByChatChannel(channelId: String): Project? {
        return projectDao.getProjectByChatChannel(channelId)
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return userDao.getChannelContributors(chatChannel) ?: emptyList()
    }

    suspend fun updateLocalProject(project: Project) {
        projectDao.update(project)
    }

    fun getLiveProjectByChatChannel(chatChannel: String): LiveData<Project> {
        return projectDao.getLiveProjectByChatChannel(chatChannel)
    }

    suspend fun getLimitedMediaMessages(channelId:String, limit: Int): List<Message> {
        return messageDao.getLimitedMediaMessages(channelId, limit).orEmpty()
    }

    suspend fun insertMessages(imagesDir: File, documentsDir: File, messages: List<Message>, preProcessed: Boolean = false) {
        Log.d(TAG, "Inserting messages")
        if (!preProcessed) {
            messageDao.insertMessages(processMessages(imagesDir, documentsDir, messages))
        } else {
            messageDao.insertMessages(messages)
        }
    }

    suspend fun getMessagesBefore(chatChannel: String, time: Long, limit: Int): List<Message> {
        return messageDao.getMessagesBefore(chatChannel, time, limit)
    }

    suspend fun getMessagesOnRefresh(chatChannelId: String, pageSize: Int): List<Message> {
        return messageDao.getMessagesOnRefresh(chatChannelId, pageSize).orEmpty()
    }

    suspend fun getMessagesOnAppend(chatChannelId: String, pageSize: Int, nextKey: Long): List<Message> {
        return messageDao.getMessagesOnAppend(chatChannelId, pageSize, nextKey).orEmpty()
    }

    fun getMessagesOnPrepend(
        chatChannelId: String,
        pageSize: Int,
        anchorMessageTimeStart: Long
    ): List<Message> {
        return messageDao.getMessagesOnPrepend(chatChannelId, pageSize, anchorMessageTimeStart).orEmpty()
    }

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return messageDao.getMessages(chatChannelId).orEmpty()
    }

    fun getLiveProjectById(id: String): LiveData<Project> {
        return projectDao.getLiveProjectById(id)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProjectById(project.id)
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteCommentById(comment.commentId)
    }

    suspend fun deleteUserById(userId: String) {
        userDao.deleteUserById(userId)
    }

    suspend fun getImageMessages(chatChannelId: String, limit: Int = 0): List<Message> {
        return if (limit != 0) {
            val list = messageDao.getMessages(chatChannelId, image).orEmpty()
            list.take(minOf(limit, list.size))
        } else {
            messageDao.getMessages(chatChannelId, image).orEmpty()
        }
    }

    suspend fun getForwardChannels(chatChannelId: String): List<ChatChannel> {
        return chatChannelDao.getForwardChannels(chatChannelId).orEmpty()
    }

    /*suspend fun updateDeliveryListOfMessages(messages: List<Message>): Result<List<Message>> {
        val currentUser = currentUser.value!!

        val newMessageList = mutableListOf<Message>()

        for (message in messages) {
            val m = messageDao.getMessage(message.messageId)
            if (m == null) {
                newMessageList.add(message)
            }
        }

        return when (val result = FireUtility.updateDeliveryListOfMessages(currentUser, newMessageList)) {
            is Result.Error -> result
            is Result.Success -> {
                for (message in newMessageList) {
                    val newList = message.deliveryList.toMutableList()
                    newList.add(currentUser.id)
                    message.deliveryList = newList
                }
                Result.Success(newMessageList)
            }
        }
    }*/

    suspend fun updateLocalProjects(updatedUser: User, projects: List<String>) {

        val updatedProjects = mutableListOf<Project>()

        for (projectId in projects) {
            val project = projectDao.getProject(projectId)
            if (project != null) {
                project.creator = updatedUser.minify()
                updatedProjects.add(project)
            }
        }

        insertProjects(updatedProjects)
    }

    fun updateDeliveryListOfMessages(
        chatChannel: ChatChannel,
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.updateDeliveryListOfMessages(chatChannel, currentUserId, messages, onComplete)
    }

    suspend fun getLastMessageForChannel(chatChannelId: String): Message? {
        return messageDao.getLastMessageForChannel(chatChannelId)
    }

    suspend fun deleteAllMessagesInChannel(chatChannelId: String) {
        messageDao.deleteAllMessagesInChannel(chatChannelId)
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