package com.jamid.codesquare.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import java.io.File
import kotlin.random.Random

class MainRepository(private val db: CollabDatabase) {

    private val interestDao = db.interestDao()
    private val chatChannelDao = db.chatChannelDao()
    val postDao = db.postDao()

    val userDao = db.userDao()
    private val messageDao = db.messageDao()
    val postRequestDao = db.postRequestDao()
    val commentDao = db.commentDao()
    val notificationDao = db.notificationDao()
    private val searchQueryDao = db.searchQueryDao()
    val postInviteDao = db.postInviteDao()
    val likedByDao = db.likedByDao()
    val referenceItemDao = db.referenceItemDao()
    val interestItemDao = db.interestItemDao()

    val currentUser: LiveData<User> = userDao.currentUser()

    val allPreviousQueries = searchQueryDao.prevQueries()

    val chatChannels = chatChannelDao.chatChannels()

    val allUnreadNotifications = notificationDao.allUnreadNotifications()

    val errors = MutableLiveData<Exception>()

    suspend fun clearLikedByTable() {
        likedByDao.clearTable()
    }

    suspend fun insertPosts(posts: Array<out Post>, shouldProcess: Boolean = true) {

        val currentUser = UserManager.currentUser
        val newPosts = posts.toMutableList()

        if (currentUser.premiumState.toInt() == -1) {
            val numberOfAds = (posts.size / 3)

            val indexes = mutableListOf<Int>()
            for (i in 0 until numberOfAds) {
                indexes.add(Random.nextInt(1, posts.size - 1))
            }

            for (i in indexes) {
                val newPost = Post()
                newPost.isAd = true
                newPost.createdAt = posts[i].createdAt
                newPosts.add(i, newPost)
            }
        }

        if (shouldProcess) {
            postDao.insert(processPosts(newPosts.toTypedArray()).toList())
        } else {
            postDao.insert(newPosts.toList())
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
        postDao.clearTable()
        chatChannelDao.clearTable()
        messageDao.clearTable()
        postRequestDao.clearTable()
        notificationDao.clearTable()
        commentDao.clearTable()
        searchQueryDao.clearTable()*/
        onComplete()
    }

    suspend fun insertPostInvites(invites: Array<out PostInvite>) {
        postInviteDao.insert(invites.toList())
    }

    suspend fun deletePostInvite(invite: PostInvite) {
        postInviteDao.deletePostInvite(invite)
    }

    // the contributors must be downloaded before the channels
    suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
        val currentUserId = UserManager.currentUserId
        val newListOfChatChannels = mutableListOf<ChatChannel>()
        for (chatChannel in chatChannels) {
            chatChannel.isNewLastMessage =
                chatChannel.lastMessage != null &&
                        chatChannel.lastMessage!!.senderId != currentUserId &&
                        !chatChannel.lastMessage!!.readList.contains(currentUserId)

            newListOfChatChannels.add(chatChannel)
        }
        chatChannelDao.insert(newListOfChatChannels)
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

    suspend fun insertPostRequests(requests: List<PostRequest>) {
        postRequestDao.insert(requests)
    }

    suspend fun insertPostRequests(postRequests: Array<out PostRequest>) {
        postRequestDao.insert(postRequests.toList())
    }

    suspend fun getPost(postId: String): Post? {
        return postDao.getPost(postId)
    }

    suspend fun getUser(userId: String): User? {
        return userDao.getUser(userId)
    }

    suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
        return chatChannelDao.getChatChannel(chatChannel)
    }

    suspend fun deletePostRequest(postRequest: PostRequest) {
        postRequestDao.deletePostRequest(postRequest)
    }

    suspend fun getComment(parentId: String): Comment? {
        return commentDao.getCommentById(parentId)
    }

    suspend fun insertComments(comments: List<Comment>) {
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

    suspend fun updateLocalPost(post: Post) {
        postDao.update(post)
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

    suspend fun updateLocalPosts(updatedUser: User, posts: List<String>) {

        val updatedPosts = mutableListOf<Post>()

        for (postId in posts) {
            val post = postDao.getPost(postId)
            if (post != null) {
                post.creator = updatedUser.minify()
                updatedPosts.add(post)
            }
        }

        insertPosts(updatedPosts.toTypedArray())
    }

    suspend fun insertNotifications(notifications: List<Notification>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun insertNotifications(notifications: Array<out Notification>) {
        notificationDao.insertNotifications(notifications.toList())
    }

    suspend fun clearPosts() {
        postDao.clearTable()
    }

    suspend fun insertSearchQuery(searchQuery: SearchQuery) {
        searchQueryDao.insert(searchQuery)
    }


    fun getCurrentUserPosts(): LiveData<List<Post>> {
        return postDao.getCurrentUserPosts()
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
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

    suspend fun deleteLocalPost(post: Post) {
        postDao.deletePost(post)
    }

    suspend fun deleteAdPosts() {
        postDao.deleteAdPosts()
    }

    suspend fun deleteLocalChatChannelById(chatChannelId: String) {
        chatChannelDao.deleteChatChannelById(chatChannelId)
    }

    fun getReactiveUser(userId: String): LiveData<User> {
        return userDao.getReactiveUser(userId)
    }

    fun getReactivePost(postId: String): LiveData<Post> {
        return postDao.getReactivePost(postId)
    }

    suspend fun clearPostInvites() {
        postInviteDao.clearTable()
    }

    suspend fun disableLocationBasedPosts() {
        postDao.disableLocationBasedPosts()
    }

    suspend fun getPostRequest(postId: String): PostRequest? {
        return postRequestDao.getPostRequestByPost(postId)
    }

    suspend fun deletePostById(postId: String) {
        postDao.deletePostById(postId)
    }

    suspend fun updateLocalMessages(updatedUser: User) {
        val currentUserMessages = messageDao.getCurrentUserMessages(updatedUser.id)
        for (message in currentUserMessages) {
            message.sender = updatedUser.minify()
        }
        messageDao.insertMessages(currentUserMessages)
    }

    suspend fun clearLikedItems() {
        referenceItemDao.clearTable()
    }

    suspend fun insertReferenceItems(items: List<ReferenceItem>) {
        referenceItemDao.insert(items)
    }

    suspend fun deleteReferenceItem(itemId: String) {
        referenceItemDao.deleteReferenceItem(itemId)
    }

    suspend fun insertInterestItems(interestItems: List<InterestItem>) {
        interestItemDao.insert(interestItems)
    }

    suspend fun clearInterestItems() {
        interestItemDao.clearInterestsTable()
    }

    fun getUnreadGeneralNotifications(): LiveData<List<Notification>> {
        return notificationDao.getUnreadNotifications(0)
    }

    fun getUnreadRequestNotifications(): LiveData<List<Notification>> {
        return notificationDao.getUnreadNotifications(1)
    }

    fun getUnreadInviteNotifications(): LiveData<List<Notification>> {
        return notificationDao.getUnreadNotifications(-1)
    }

    suspend fun updateChatChannel(chatChannel: ChatChannel) {
        chatChannelDao.update(chatChannel)
    }

    fun getUnreadChatChannels(): LiveData<List<ChatChannel>> {
        return chatChannelDao.getUnreadChatChannels()
    }

    companion object {

        @Volatile private var instance: MainRepository? = null

        fun getInstance(db: CollabDatabase): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(db)
            }
        }

    }

}