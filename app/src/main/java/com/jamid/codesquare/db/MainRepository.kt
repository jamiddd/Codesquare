package com.jamid.codesquare.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File// something simple

class MainRepository(private val db: CollabDatabase, private val scope: CoroutineScope, private val root: File) {

    private val interestDao = db.interestDao()
    val postDao = db.postDao()

    val userMinimalDao = db.userMinimalDao()
    val userDao = db.userDao()
    val messageDao = db.messageDao()
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

    private var chatChannelsListenerRegistration: ListenerRegistration? = null

    private var currentUserId: String = ""

    init {
        Firebase.auth.addAuthStateListener {
            val currentFirebaseUser = it.currentUser
            if (currentFirebaseUser == null) {
                // is not signed in
            } else {
                // is signed in
                currentUserId = currentFirebaseUser.uid
                setChannelListener()
            }
        }

    }

    fun insertMessages(root: File, messages: List<Message>, preProcessed: Boolean = false) =
        scope.launch(Dispatchers.IO) {
            if (!preProcessed) {
                processMessages(root, messages)
                messageDao.insertMessages(messages)
            } else {
                messageDao.insertMessages(messages)
            }
        }

//    fun chatChannels(currentUserId: String) = chatChannelDao.chatChannels(currentUserId)



    fun chatChannelWrappers(currentUserId: String) = db.chatChannelWrapperDao().chatChannelWrappers(currentUserId)

    val allUnreadNotifications = notificationDao.allUnreadNotifications()

    val errors = MutableLiveData<Exception>()

    private fun setChannelListener() {
        chatChannelsListenerRegistration?.remove()
        chatChannelsListenerRegistration = Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e(
                        TAG,
                        "initializeListeners: Something went wrong - ${error.localizedMessage}"
                    )
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {

                    Log.d(TAG, "setChannelListener: Found channels (${value.size()})")

                    scope.launch(Dispatchers.IO) {
                        clearChatChannels()

                        val chatChannels = value.toObjects(ChatChannel::class.java)
                        insertChatChannels2(chatChannels)
                    }
                }
            }
    }

    fun insertChannelMessages(messages: List<Message>) = scope.launch(Dispatchers.IO) {
        val uid = Firebase.auth.currentUser?.uid
        if (messages.isNotEmpty() && uid != null) {

            val firstTimeMessages = messages.filter { message ->
                !message.deliveryList.contains(uid)
            }

            val alreadyDeliveredMessages = messages.filter { message ->
                message.deliveryList.contains(uid)
            }

            insertMessages(root, alreadyDeliveredMessages)

            // update the delivery list
            updateDeliveryListOfMessages(uid, firstTimeMessages) { it1 ->
                if (!it1.isSuccessful) {
                    errors.postValue(it1.exception)
                } else {
                    insertMessages(root, firstTimeMessages)
                }
            }
        }
    }

    private fun updateDeliveryListOfMessages(
        currentUserId: String,
        messages: List<Message>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.updateDeliveryListOfMessages(currentUserId, messages, onComplete)
    }

    suspend fun clearChatChannels() {
        db.chatChannelWrapperDao().clearTable()
    }

    suspend fun clearLikedByTable() {
        likedByDao.clearTable()
    }

    fun messageRequests(): LiveData<List<ChatChannelWrapper>> {
        return db.chatChannelWrapperDao().messageRequests(currentUserId = UserManager.currentUserId)
    }

    private val oldAdsList = mutableListOf<Post>()

    private fun getAdsForPostBatch(posts: List<Post>): List<Post> {
        val totalAds = posts.size / 4 // 25% ads

        val likesCountList = posts.map { it.likesCount }
        val createdAtList = posts.map { it.createdAt }
        val updatedAtList = posts.map { it.updatedAt }
        val viewsCountList = posts.map { it.viewsCount }

        return Array(totalAds) {
            Post().apply {
                id = randomId()
                viewsCount = viewsCountList.random()
                likesCount = likesCountList.random()
                createdAt = createdAtList.random()
                updatedAt = updatedAtList.random()
                isAd = true
            }
        }.toList()
    }

    var limit1: Long = 0
    var limit2: Long = System.currentTimeMillis()

    suspend fun insertPosts(posts: Array<out Post>, shouldProcess: Boolean = true) {

        var requireAdInsertion = false
        if (posts.size > 1) {
            val currentUpperLimit = posts.first().createdAt
            val currentLowerLimit = posts.last().createdAt

            if (currentLowerLimit > limit2 && currentUpperLimit < limit1) {
                requireAdInsertion = false
            }

            if (currentUpperLimit > limit1) {
                limit1 = currentUpperLimit
                requireAdInsertion = true
            }

            if (currentLowerLimit < limit2) {
                limit2 = currentLowerLimit
                requireAdInsertion = true
            }
        }

        val newPosts = posts.toMutableList()
        if (requireAdInsertion) {
            oldAdsList.addAll(getAdsForPostBatch(posts.toList()))
            newPosts.addAll(oldAdsList)
        } else {
            Log.d(TAG, "insertPosts: No need to insert ads")
        }

        if (shouldProcess) {
            processPosts(newPosts)
        }

        postDao.insert(newPosts)

        val pipeline = Fresco.getImagePipeline()
        for (post in newPosts) {
            val mediaItems = post.mediaList
            val mediaString = post.mediaString

            for (index in mediaString.indices) {
                if (mediaString[index].digitToInt() == 0) {
                    val image = mediaItems[index]
                    val imageRequest = ImageRequest.fromUri(image)
                    pipeline.prefetchToDiskCache(imageRequest, null)
                }
            }

            val ir = ImageRequest.fromUri(post.creator.photo)
            pipeline.prefetchToDiskCache(ir, null)
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
   /* suspend fun insertChatChannels(chatChannels: List<ChatChannel>) {
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
    }*/

    suspend fun insertChatChannels2(chatChannels: List<ChatChannel>) {
        db.chatChannelWrapperDao().insert(chatChannels.map {
            it.toChatChannelWrapper()
        })
    }

    private fun processMessages(root: File, messages: List<Message>): List<Message> {
        // filter the messages which are marked as not delivered by the message
        for (message in messages) {
            // check if the media is already downloaded in the local folder

            if (message.type == text) {
                message.isDownloaded = true
            } else {
                val name = message.content + message.metadata!!.ext
                val f = File(root, "${message.type}s/${message.chatChannelId}/$name")
                message.isDownloaded = f.exists()
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
        return db.chatChannelWrapperDao().getChatChannel(chatChannel)?.chatChannel
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

    suspend fun updateLocalPosts(updatedUser: User) {

        val updatedPosts = mutableListOf<Post>()

        for (postId in updatedUser.posts) {
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
        db.chatChannelWrapperDao().deleteChatChannelById(chatChannelId)
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
        interestItemDao.insertInterests(interestItems)
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
        return notificationDao.getUnreadNotifications(2)
    }

//    suspend fun updateChatChannel(chatChannel: ChatChannel) {
//        chatChannelDao.update(chatChannel)
//    }

    fun getUnreadChatChannels(): LiveData<List<ChatChannelWrapper>> {
        return db.chatChannelWrapperDao().getUnreadChatChannels()
    }

    suspend fun deleteCommentsByUserId(id: String) {
        commentDao.deleteCommentsByUserId(id)
    }

    suspend fun deletePostsByUserId(id: String) {
        postDao.deletePostsByUserId(id)
    }

    suspend fun deletePreviousSearchByUserId(id: String) {
        searchQueryDao.deletePreviousSearchByUserId(id)
    }

    suspend fun insertInterestItem(interestItem: InterestItem) {
        interestItemDao.insert(interestItem)
    }

    suspend fun updateAllGeneralNotificationsToRead() {
        notificationDao.updateAllGeneralNotificationsToRead()
    }

    suspend fun updateAllRequestNotificationsToRead() {
        notificationDao.updateAllRequestNotificationsToRead()
    }

    suspend fun updateAllInviteNotificationsToRead() {
        notificationDao.updateAllInviteNotificationsToRead()
    }

    suspend fun getPostRequestByNotificationId(id: String): PostRequest? {
        return postRequestDao.getPostRequestByNotificationId(id)
    }

    suspend fun getUnreadNotifications(): List<Notification> {
        return notificationDao.getUnreadNotificationsAlt()
    }

    suspend fun getInterestItem(tag: String): InterestItem? {
        return interestItemDao.getInterestItem(tag)
    }

    fun getChannelContributors(chatChannelId: String): LiveData<List<User>> {
        return userDao.getChannelContributorsLive("%$chatChannelId%")
    }

    fun archivedChannels(currentUserId: String): LiveData<List<ChatChannelWrapper>> {
        return db.chatChannelWrapperDao().archivedChannels(currentUserId)
    }

    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannelWrapper> {
        return db.chatChannelWrapperDao().getReactiveChatChannel(chatChannelId)
    }

    suspend fun insertChatChannelWrappers(chatChannelWrappers: List<ChatChannelWrapper>) {
        db.chatChannelWrapperDao().insert(chatChannelWrappers)
    }

    companion object {

        @Volatile private var instance: MainRepository? = null

        fun getInstance(db: CollabDatabase, scope: CoroutineScope, root: File): MainRepository {
            return instance ?: synchronized(this) {
                instance ?: MainRepository(db, scope, root)
            }
        }

    }

}