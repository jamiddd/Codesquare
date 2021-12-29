package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import java.io.File

class MainRepository(private val db: CodesquareDatabase) {

    val projectDao = db.projectDao()
    private val userDao = db.userDao()
    private val interestDao = db.interestDao()
    val chatChannelDao = db.chatChannelDao()
    val messageDao = db.messageDao()
    val projectRequestDao = db.projectRequestDao()
    val commentDao = db.commentDao()
    val notificationDao = db.notificationDao()
    private val searchQueryDao = db.searchQueryDao()
    val projectInviteDao = db.projectInviteDao()

    val currentUser: LiveData<User> = userDao.currentUser()
    val onMessagesModeChanged = messageDao.onMessagesModeChanged()

    /*val previousProjectQueries = searchQueryDao.previousQueries(QUERY_TYPE_PROJECT)
    val previousUserQueries = searchQueryDao.previousQueries(QUERY_TYPE_USER)*/

    val allPreviousQueries = searchQueryDao.prevQueries()

    val chatChannels = chatChannelDao.chatChannels()

    val allUnreadNotifications = notificationDao.allUnreadNotifications()

    val errors = MutableLiveData<Exception>()

    suspend fun insertProjects(projects: Array<out Project>, shouldProcess: Boolean = true) {
        val currentUser = currentUser.value
        if (currentUser != null) {
            if (shouldProcess) {
                projectDao.insert(processProjects(projects).toList())
            } else {
                projectDao.insert(projects.toList())
            }
        }
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

    // clear all tables
    fun clearDatabases(onComplete: () -> Unit) {
        db.clearAllTables()
        /*userDao.clearTable()
        projectDao.clearTable()
        chatChannelDao.clearTable()
        messageDao.clearTable()
        projectRequestDao.clearTable()
        notificationDao.clearTable()
        commentDao.clearTable()
        searchQueryDao.clearTable()*/
        onComplete()
    }

    suspend fun insertProjectInvites(invites: Array<out ProjectInvite>) {
        projectInviteDao.insert(invites.toList())
    }

    suspend fun deleteProjectInvite(invite: ProjectInvite) {
        projectInviteDao.insert(invite)
    }

    suspend fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) {
        chatChannelDao.insert(channels)
    }

    private suspend fun processChatChannel(chatChannel: ChatChannel): ChatChannel {
        val lastMessage = chatChannel.lastMessage

        if (lastMessage != null && lastMessage.sender.isEmpty()) {
            val sender = getUser(lastMessage.senderId)
            if (sender != null) {
                lastMessage.sender = sender
                chatChannel.lastMessage = lastMessage
            } else {
                when (val result = FireUtility.getUser(lastMessage.senderId)) {
                    is Result.Error -> {
                        errors.postValue(result.exception)
                    }
                    is Result.Success -> {
                        val unknownContributor = result.data
                        insertUser(unknownContributor)
                        lastMessage.sender = unknownContributor
                        chatChannel.lastMessage = lastMessage
                    }
                    null -> {
                        Log.d(TAG, "Probably the document doesn't exist")
                    }
                }
            }
        }
        return chatChannel
    }

    // the contributors must be downloaded before the channels
    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        val newListOfChatChannels = mutableListOf<ChatChannel>()
        for (chatChannel in chatChannels) {
            newListOfChatChannels.add(processChatChannel(chatChannel))
        }
        chatChannelDao.insert(newListOfChatChannels)
    }

    private suspend fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
            // check if the message has user attached to it
            val user = getUser(message.senderId)
            if (user != null) {
                message.sender = user
            } else {
                when (val result = FireUtility.getUser(message.senderId)) {
                    is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                    is Result.Success -> {
                        insertUsers(arrayOf(result.data))
                        message.sender = result.data
                    }
                    else -> {
                        //
                    }
                }
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

            if (message.replyTo != null) {
                val localMessage = messageDao.getMessage(message.replyTo!!)
                if (localMessage != null) {
                    message.replyMessage = localMessage.toReplyMessage()
                } else {
                    val docRef = Firebase.firestore.collection("chatChannels")
                        .document(message.chatChannelId)
                        .collection("messages")
                        .document(message.replyTo!!)

                    when (val result = FireUtility.getDocument(docRef)) {
                        is Result.Error -> {
                            Log.e(TAG, result.exception.localizedMessage.orEmpty())
                        }
                        is Result.Success -> {
                            if (result.data.exists()) {
                                val msg = result.data.toObject(Message::class.java)!!
                                val sender = getUser(msg.senderId)
                                if (sender != null) {
                                    msg.sender = sender
                                    message.replyMessage = msg.toReplyMessage()
                                }
                            }
                        }
                    }
                }
            }

            message.isCurrentUserMessage = UserManager.currentUserId == message.senderId

        }

        return messages
    }


    /*suspend fun onJoinProject(project: Project) {
        val currentUser = UserManager.currentUser
        val title = project.name
//        val notification = NotificationProvider.createNotification(project, project.creator.userId, NOTIFICATION_JOIN_PROJECT)
        val content = currentUser.name + " wants to join your project"
        val notification = Notification.createNotification(content, currentUser.id, project.creator.userId, userId = currentUser.id, title = title)
        // join project
        when (val sendRequestResult = FireUtility.joinProject(currentUser, project, notification)) {
            is Result.Error -> {
                Log.e(TAG, "296 -" + sendRequestResult.exception.localizedMessage.orEmpty())
            }
            is Result.Success -> {
                val projectRequest = sendRequestResult.data
                notificationDao.insert(notification)
                val projectRequestsList = currentUser.projectRequests.addItemToList(projectRequest.requestId)
                currentUser.projectRequests = projectRequestsList
                val requestsList = project.requests.addItemToList(projectRequest.requestId)
                project.requests = requestsList
                insertCurrentUser(currentUser)
                project.isRequested = true
                insertProjectsWithoutProcessing(arrayOf(project))
                projectRequestDao.insert(sendRequestResult.data)
            }
        }
    }*/

    suspend fun insertProjectsWithoutProcessing(projects: Array<out Project>) {
        insertProjects(projects, false)
    }

    suspend fun insertProjectRequests(projectRequests: Array<out ProjectRequest>) {

        val finalProjectRequestList = mutableListOf<ProjectRequest>()

        for (request in projectRequests) {
            when (val res1 = FireUtility.getProject(request.projectId)) {
                is Result.Error -> {
                    Log.e(TAG, res1.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    val project = res1.data
                    request.project = project
                }
                null -> {
                    Log.d(TAG, "Probably project doesn't exist with project id: ${request.projectId}")
                }
            }

            when (val res2 = FireUtility.getUser(request.senderId)) {
                is Result.Error -> {
                    Log.e(TAG, res2.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    val user = res2.data
                    request.sender = user
                    finalProjectRequestList.add(request)
                }
                null -> {
                    Log.d(TAG, "Probably user doesn't exist with user id: ${request.senderId}")
                }
            }
        }

        projectRequestDao.insert(finalProjectRequestList)
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
        projectRequestDao.deleteProjectRequest(projectRequest)
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

                when (val result = FireUtility.getOtherUser(comment.senderId)) {
                    is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                    is Result.Success -> {
                        comment.sender = result.data
                    }
                    null -> {
                        Log.d(TAG, "Couldn't fetch user for comment.")
                        comment.sender = User().apply {
                            name = "User not found"
                        }
                    }
                }
            }
            commentDao.insert(comments)
        }
    }

    suspend fun insertUsers(users: Array<out User>) {
        val currentUser = UserManager.currentUser
        val usersList = mutableListOf<User>()
        for (user in users) {
            user.isLiked = currentUser.likedUsers.contains(user.id)
            usersList.add(user)
        }
        userDao.insert(usersList)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
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

    suspend fun getLimitedMediaMessages(channelId:String, limit: Int, type: String = image): List<Message> {
        return messageDao.getLimitedMediaMessages(channelId, limit, type).orEmpty()
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

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return messageDao.getMessages(chatChannelId).orEmpty()
    }

    fun getLiveProjectById(id: String): LiveData<Project> {
        return projectDao.getLiveProjectById(id)
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteComment(comment)
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

        insertProjects(updatedProjects.toTypedArray())
    }

    fun updateDeliveryListOfMessages(
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.updateDeliveryListOfMessages(currentUserId, messages, onComplete)
    }

    suspend fun getLastMessageForChannel(chatChannelId: String): Message? {
        return messageDao.getLastMessageForChannel(chatChannelId)
    }

    suspend fun updateRestOfTheMessages(chatChannelId: String, selected: Int) {
        messageDao.updateRestOfTheMessagesInChannel(chatChannelId, selected)
    }

    suspend fun getLocalMessage(messageId: String): Message? {
        return messageDao.getMessage(messageId)
    }

    suspend fun insertNotifications(notifications: Array<out Notification>) {
        notificationDao.insertNotifications(notifications.toList())
    }

    suspend fun clearProjects() {
        projectDao.clearTable()
    }

    fun getCurrentChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return chatChannelDao.getCurrentChatChannel(chatChannelId)
    }

    /*suspend fun onUndoProject(project: Project, projectRequest: ProjectRequest) {
        val currentUser = currentUser.value
        if (currentUser != null) {
            when (val result = FireUtility.undoJoinProject(currentUser.id, project.id, projectRequest.requestId)) {
                is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                is Result.Success -> {
                    val userList = currentUser.projectRequests.removeItemFromList(projectRequest.requestId)
                    currentUser.projectRequests = userList
                    val projectList = project.requests.removeItemFromList(projectRequest.requestId)
                    project.requests = projectList
                    insertCurrentUser(currentUser)
                    project.isRequested = false
                    insertProjectsWithoutProcessing(arrayOf(project))
                    projectRequestDao.deleteProjectRequest(projectRequest)
                }
            }
        }
    }*/

    suspend fun insertSearchQuery(searchQuery: SearchQuery) {
        searchQueryDao.insert(searchQuery)
    }

    suspend fun deleteAllMessagesByUser(userId: String, chatChannelId: String? = null) {
        if (chatChannelId != null) {
            messageDao.deleteAllMessagesByUser(userId)
        } else {
            messageDao.deleteAllMessagesByUserInChannel(userId, chatChannelId)
        }
    }

    fun getCurrentUserProjects(): LiveData<List<Project>> {
        return projectDao.getCurrentUserProjects()
    }

    suspend fun updateLocalChatChannel(chatChannel: ChatChannel) {
        chatChannelDao.update(chatChannel)
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }

    suspend fun insertOtherUser(otherUser: User) {
        userDao.insert(otherUser)
    }

    suspend fun insertInterests(interests: Array<out Interest>) {
        interestDao.insert(interests.toList())
    }

    suspend fun deleteNotificationById(id: String) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun likeLocalUserById(userId: String) {
        userDao.likeLocalUserById(userId)
    }

    suspend fun updateComment(comment: Comment) {
        commentDao.update(comment)
    }

    suspend fun dislikeLocalUserById(userId: String) {
        userDao.dislikeLocalUserById(userId)
    }

    suspend fun getLocalProject(projectId: String): Project? {
        return projectDao.getProject(projectId)
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