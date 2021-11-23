package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.*
import com.jamid.codesquare.document
import com.jamid.codesquare.image
import com.jamid.codesquare.randomId
import java.io.File

class MainRepository(db: CodesquareDatabase) {

    val projectDao = db.projectDao()
    private val userDao = db.userDao()
    val chatChannelDao = db.chatChannelDao()
    val messageDao = db.messageDao()
    val projectRequestDao = db.projectRequestDao()
    val commentDao = db.commentDao()
    val notificationDao = db.notificationDao()

    val currentUser: LiveData<User> = userDao.currentUser()
    val onMessagesModeChanged = messageDao.onMessagesModeChanged()

    val chatChannels = chatChannelDao.chatChannels()

    val errors = MutableLiveData<Exception>()

    suspend fun insertProjects(projects: List<Project>) {
        val currentUser = currentUser.value
        if (currentUser != null) {
            projectDao.insert(processProjects(currentUser, projects))
        }
    }

    fun processProjects(currentUser: User, projects: List<Project>): List<Project> {
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

        return projects
    }

    private suspend fun insertCurrentUser(user: User) {
        user.isCurrentUser = true
        userDao.insert(user)
    }

    suspend fun insertUser(localUser: User) {
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
                val notification = notificationDao.getNotificationByType(project.id, "like")
                notificationDao.deleteNotificationByType(project.id, "like")
                FireUtility.dislikeProject(currentUser.id, project, notification)
            } else {
                val notification = Notification(randomId(), project.title, currentUser.name + " has liked your project.", System.currentTimeMillis(), currentUser.id, project.creator.userId, project.id, "like", "project")
                notificationDao.insert(notification)
                FireUtility.likeProject(currentUser, project, notification)
            }

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "Error while liking or disliking a post -> " + result.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    // insert the new project
                    val data = mutableMapOf<String, Any>()
                    data["creatorId"] = project.creator.userId
                    data["senderId"] = currentUser.id
                    data["title"] = project.title

//                    FireUtility.callFunction(data)


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
                    Log.e(TAG, "Error while saving or unSaving a post -> " + result.exception.localizedMessage.orEmpty())
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

    /*suspend fun getAllLocalChatChannels(): List<ChatChannel> {
        return chatChannelDao.allChannels().orEmpty()
    }*/

    suspend fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) {
        chatChannelDao.insert(channels)
    }

    private suspend fun processChatChannel(chatChannel: ChatChannel): ChatChannel {
        val lastMessage = chatChannel.lastMessage

        if (lastMessage != null && lastMessage.sender.isEmpty()) {
            val sender = getUser(lastMessage.senderId)
            if (sender != null) {
                lastMessage.sender = sender
            } else {
                errors.postValue(Exception("No user found for the last message with message id: ${lastMessage.messageId} and chatChannel id: ${chatChannel.chatChannelId}, name => ${chatChannel.projectTitle}"))
            }
        }
        return chatChannel
    }

    suspend fun insertChatChannel(chatChannel: ChatChannel) {
        chatChannelDao.insert(processChatChannel(chatChannel))
    }

    // the contributors must be downloaded before the channels
    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        for (chatChannel in chatChannels) {
            processChatChannel(chatChannel)
        }
        chatChannelDao.insert(chatChannels)
    }

    suspend fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
            // check if the message has user attached to it
            val user = getUser(message.senderId)
            if (user != null) {
                message.sender = user
            } else {
                throw NullPointerException("The user doesn't exist for a message with user id - ${message.senderId}")
            }

            // check if the media is already downloaded in the local folder
            if (message.type == image) {
                val name = message.content + message.metadata?.ext.orEmpty()
                val f = File(imagesDir, name)
                message.isDownloaded = f.exists()
            }

            if (message.type == document) {
                val name = message.content + message.metadata?.ext.orEmpty()
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

                val notification = notificationDao.getNotificationByType(project.id, "join")

                when (val undoSendRequestResult = FireUtility.undoJoinProject(currentUser.id, project.id, requestId, notification)) {
                    is Result.Error -> {
                        Log.e(TAG, undoSendRequestResult.exception.localizedMessage.orEmpty())
                    }
                    is Result.Success -> {
                        if (notification != null) {
                            notificationDao.deleteNotificationByType(project.id, "join")
                        }

                        val projectRequestsList = currentUser.projectRequests.toMutableList()
                        projectRequestsList.remove(requestId)
                        currentUser.projectRequests = projectRequestsList

                        val requestsList = project.requests.toMutableList()
                        requestsList.remove(requestId)
                        project.requests = requestsList

                        projectRequestDao.deleteProjectRequest(requestId)
                    }
                }
            } else {

                val notification = Notification(randomId(), project.title, "${currentUser.name} has requested to join your project.", System.currentTimeMillis(), currentUser.id, project.creator.userId, project.id, "join", "project")

                // join project
                when (val sendRequestResult = FireUtility.joinProject(currentUser, project, notification)) {
                    is Result.Error -> {
                        Log.e(TAG, sendRequestResult.exception.localizedMessage.orEmpty())
                    }
                    is Result.Success -> {
                        val projectRequest = sendRequestResult.data

                        notificationDao.insert(notification)

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
        val currentUser = currentUser.value
        if (currentUser != null) {
            for (comment in comments) {
                comment.isLiked = currentUser.likedComments.contains(comment.commentId)
            }
            commentDao.insert(comments)
        }
    }

    suspend fun onCommentLiked(comment: Comment) {
        val currentUser = currentUser.value
        if (currentUser != null) {

            val isLiked = currentUser.likedComments.contains(comment.commentId)

            val result = if (isLiked) {
                // dislike
                val notification = notificationDao.getNotificationByType(comment.commentId, "like")
                notificationDao.deleteNotificationByType(comment.commentId, "type")
                FireUtility.dislikeComment(currentUser.id, comment, notification)
            } else {
                // like
                val notification = Notification(randomId(), "Your comment", currentUser.name + " has liked your comment.", System.currentTimeMillis(), currentUser.id, comment.senderId, comment.commentId, "like", "comment")
                notificationDao.insert(notification)
                FireUtility.likeComment(currentUser.id, comment, notification)
            }

            when (result) {
                is Result.Error -> {
                    Log.e(TAG, "Error while liking or disliking a comment -> " + result.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    // insert the new project
                    val newLikedCommentsList = currentUser.likedComments.toMutableList()
                    val newCommentLikesList = comment.likes.toMutableList()

                    if (isLiked) {
                        comment.likesCount = comment.likesCount - 1
                        comment.isLiked = false
                        newLikedCommentsList.remove(comment.commentId)
                        newCommentLikesList.remove(currentUser.id)
                    } else {
                        comment.likesCount = comment.likesCount + 1
                        comment.isLiked = true
                        newLikedCommentsList.add(comment.commentId)
                        newCommentLikesList.add(currentUser.id)
                    }

                    comment.likes = newCommentLikesList
                    commentDao.insert(comment)

                    // insert the new user
                    currentUser.likedComments = newLikedCommentsList
                    userDao.insert(currentUser)
                }
            }

        }
    }

    suspend fun insertUsers(users: List<User>) {
        Log.d(TAG, "Inserting multiple users")
        userDao.insert(users)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    /*suspend fun updateMessages(messages: List<Message>) {
        messageDao.update(messages)
    }*/

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
        if (!preProcessed) {
            messageDao.insertMessages(processMessages(imagesDir, documentsDir, messages))
        } else {
            messageDao.insertMessages(messages)
        }
    }

    suspend fun getMessagesBefore(chatChannel: String, time: Long, limit: Int): List<Message> {
        return messageDao.getMessagesBefore(chatChannel, time, limit)
    }


    suspend fun getMessagesOnAppend(chatChannelId: String, pageSize: Int, nextKey: Long): List<Message> {
        return messageDao.getMessagesOnAppend(chatChannelId, pageSize, nextKey).orEmpty()
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

    suspend fun updateRestOfTheMessages(chatChannelId: String, selected: Int) {
        messageDao.updateRestOfTheMessagesInChannel(chatChannelId, selected)
    }

    /*suspend fun getSelectedMessages(): List<Message> {
        return messageDao.getSelectedMessages().orEmpty()
    }*/

    suspend fun getLocalMessage(messageId: String): Message? {
        return messageDao.getMessage(messageId)
    }

    suspend fun insertNotifications(notifications: List<Notification>) {
        notificationDao.insert(notifications)
    }

    suspend fun deleteNotificationByType(contextId: String, type: String) {
        notificationDao.deleteNotificationByType(contextId, type)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearNotifications()
    }

    suspend fun clearProjects() {
        projectDao.clearTable()
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