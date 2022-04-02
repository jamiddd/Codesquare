package com.jamid.codesquare.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import java.io.File
import kotlin.random.Random

class MainRepository(private val db: CodesquareDatabase) {

    private val interestDao = db.interestDao()
    private val chatChannelDao = db.chatChannelDao()
    val projectDao = db.projectDao()

    val userDao = db.userDao()
    val messageDao = db.messageDao()
    val projectRequestDao = db.projectRequestDao()
    val commentDao = db.commentDao()
    val notificationDao = db.notificationDao()
    private val searchQueryDao = db.searchQueryDao()
    val projectInviteDao = db.projectInviteDao()

    val currentUser: LiveData<User> = userDao.currentUser()

    val allPreviousQueries = searchQueryDao.prevQueries()

    val chatChannels = chatChannelDao.chatChannels()

    val allUnreadNotifications = notificationDao.allUnreadNotifications()

    val errors = MutableLiveData<Exception>()

    suspend fun insertProjects(projects: Array<out Project>, shouldProcess: Boolean = true) {

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
            projectDao.insert(processProjects(newProjects.toTypedArray()).toList())
        } else {
            projectDao.insert(newProjects.toList())
        }
    }

    private suspend fun insertCurrentUser(user: User) {
        user.isCurrentUser = true
        userDao.insert(user)
    }

    suspend fun insertUser(localUser: User) {
        if (localUser.id == UserManager.currentUserId) {
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
        projectInviteDao.deleteProjectInvite(invite)
    }

    suspend fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) {
        chatChannelDao.insert(channels)
    }

    // the contributors must be downloaded before the channels
    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        /*val newListOfChatChannels = mutableListOf<ChatChannel>()
        for (chatChannel in chatChannels) {
            newListOfChatChannels.add(processChatChannel(chatChannel))
        }*/
        chatChannelDao.insert(chatChannels)
    }

    private fun processMessages(imagesDir: File, documentsDir: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
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

            if (message.type == text) {
                message.isDownloaded = true
            }

            message.isCurrentUserMessage = UserManager.currentUserId == message.senderId

        }

        return messages
    }

    suspend fun insertProjectsWithoutProcessing(projects: Array<out Project>) {
        insertProjects(projects, false)
    }

    suspend fun insertProjectRequests(requests: List<ProjectRequest>) {
        projectRequestDao.insert(requests)
    }

    suspend fun insertProjectRequests(projectRequests: Array<out ProjectRequest>) {
        projectRequestDao.insert(projectRequests.toList())
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

    suspend fun insertComments(comments: List<Comment>) {
        val currentUser = UserManager.currentUser
        for (comment in comments) {
            comment.isLiked = currentUser.likedComments.contains(comment.commentId)
        }
        commentDao.insert(comments)
    }

    suspend fun insertUsers(users: List<User>) {
        val newUsers = processUsers(*users.toTypedArray())
        userDao.insert(newUsers.toList())
    }

    suspend fun insertUsers(users: Array<out User>) {
        val newUsers = processUsers(*users)
        userDao.insert(newUsers.toList())
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return userDao.getChannelContributors(chatChannel) ?: emptyList()
    }

    suspend fun updateLocalProject(project: Project) {
        projectDao.update(project)
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

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return messageDao.getMessages(chatChannelId).orEmpty()
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

    suspend fun insertNotifications(notifications: List<Notification>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun insertNotifications(notifications: Array<out Notification>) {
        notificationDao.insertNotifications(notifications.toList())
    }

    suspend fun clearProjects() {
        projectDao.clearTable()
    }

    suspend fun insertSearchQuery(searchQuery: SearchQuery) {
        searchQueryDao.insert(searchQuery)
    }


    fun getCurrentUserProjects(): LiveData<List<Project>> {
        return projectDao.getCurrentUserProjects()
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

    // update function would be better but not working
    suspend fun likeLocalUserById(userId: String) {
        val user = userDao.getUser(userId)
        if (user != null) {
            user.isLiked = true
            user.likesCount += 1

            userDao.insert(user)
        }
    }

    suspend fun updateComment(comment: Comment) {
        commentDao.update(comment)
    }

    // update function would be better but not working
    suspend fun dislikeLocalUserById(userId: String) {
        val user = userDao.getUser(userId)
        if (user != null) {
            user.isLiked = false
            user.likesCount -= 1

            userDao.insert(user)
        }
    }

    suspend fun getLocalProject(projectId: String): Project? {
        return projectDao.getProject(projectId)
    }

    suspend fun deleteLocalProject(project: Project) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteAdProjects() {
        projectDao.deleteAdProjects()
    }

    fun getChannelContributorsLive(formattedChannelId: String): LiveData<List<User>> {
        return userDao.getChannelContributorsLive(formattedChannelId)
    }

    suspend fun deleteLocalChatChannelById(chatChannelId: String) {
        chatChannelDao.deleteChatChannelById(chatChannelId)
    }

    fun getReactiveUser(userId: String): LiveData<User> {
        return userDao.getReactiveUser(userId)
    }

    fun getReactiveProject(projectId: String): LiveData<Project> {
        return projectDao.getReactiveProject(projectId)
    }

    fun getReactiveComment(commentId: String): LiveData<Comment> {
        return commentDao.getReactiveComment(commentId)
    }

    suspend fun clearProjectInvites() {
        projectInviteDao.clearTable()
    }

    suspend fun disableLocationBasedProjects() {
        projectDao.disableLocationBasedProjects()
    }

    suspend fun getProjectRequest(projectId: String): ProjectRequest? {
        return projectRequestDao.getProjectRequestByProject(projectId)
    }

    companion object {

        @Volatile private var instance: MainRepository? = null

        fun getInstance(db: CodesquareDatabase): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(db)
            }
        }

    }

}