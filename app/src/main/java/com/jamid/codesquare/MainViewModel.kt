package com.jamid.codesquare

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import com.algolia.search.client.ClientSearch
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.multipleindex.IndexQuery
import com.algolia.search.model.multipleindex.IndexedQuery
import com.algolia.search.model.response.ResponseMultiSearch
import com.algolia.search.model.response.ResponseSearch
import com.algolia.search.model.response.ResultMultiSearch
import com.android.billingclient.api.ProductDetails
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.FeedSort.*
import com.jamid.codesquare.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class MainViewModel(application: Application): AndroidViewModel(application) {

    private val repo: MainRepository
    private val chatRepository: ChatRepository
    private val userRepository: UserRepository

    init {
        val db = CollabDatabase.getInstance(application.applicationContext)
        repo = MainRepository.getInstance(db)
        chatRepository = ChatRepository(db, viewModelScope, application.applicationContext)
        userRepository = UserRepository(db, viewModelScope)
    }

    private val _currentError = MutableLiveData<Exception?>()

    val currentError: LiveData<Exception?> = _currentError
    val currentUser: LiveData<User> = repo.currentUser

    /**
     * A placeholder post to be used while creating new post.
    * */
    private val _currentPost = MutableLiveData<Post>().apply { value = null }
    val currentPost: LiveData<Post> = _currentPost


    val chatChannelsBitmapMap = mutableMapOf<String, Bitmap>()


    private val _currentQuery = MutableLiveData<Query>().apply { value = null }
    val currentQuery: LiveData<Query> = _currentQuery

    private val defaultFeedOption = FeedOption("Random", MOST_RECENT, FeedOrder.DESC)

    private val _feedOption = MutableLiveData<FeedOption>().apply { value = defaultFeedOption }
    val feedOption: LiveData<FeedOption> = _feedOption

    private val _currentImage = MutableLiveData<Uri?>()
    val currentImage: LiveData<Uri?> = _currentImage

    private val userCache = mutableMapOf<String, User>()
    private val postCache = mutableMapOf<String, Post>()


    /**
     * Flag to check whether sound network is available or not
     * */
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable


    /**
     * List of all the products fetched from play store
     * */
    private val _productDetails = MutableLiveData<List<ProductDetails>>().apply { value = emptyList() }
    val productDetails: LiveData<List<ProductDetails>> = _productDetails

    private val _currentlySelectedProduct = MutableLiveData<ProductDetails?>()

    fun setCurrentlySelectedProduct(productDetails: ProductDetails?) {
        _currentlySelectedProduct.postValue(productDetails)
    }

    fun setProductDetailsList(productDetails: List<ProductDetails> = emptyList()) {
        _productDetails.postValue(productDetails)
    }

    fun setCurrentFeedOption(feedOption: FeedOption) {
        _feedOption.postValue(feedOption)
    }

    fun setDefaultFeedOption() {
        _feedOption.postValue(defaultFeedOption)
    }

    fun setCurrentQuery(query: Query) {
        _currentQuery.postValue(query)
    }

    /**
     * List of all the available subscriptions for the user
     * */
    private val _products = MutableLiveData<List<OneTimeProduct>>().apply { value = emptyList() }
    val products: LiveData<List<OneTimeProduct>> = _products

    fun setProducts(list: List<OneTimeProduct> = emptyList()) {
        _products.postValue(list)
    }


    val chatChannels = repo.chatChannels
    val errors = repo.errors

    var currentUserBitmap: Bitmap? = null

    val allUnreadNotifications = repo.allUnreadNotifications

    val allPreviousQueries = repo.allPreviousQueries

    private val _recentSearchList = MutableLiveData<List<SearchQuery>>().apply { value = null }
    val recentSearchList: LiveData<List<SearchQuery>> = _recentSearchList

    private val client = ClientSearch(ApplicationID(BuildConfig.ALGOLIA_ID), APIKey(BuildConfig.ALGOLIA_SECRET))

    fun setSearchData(searchList: List<SearchQuery>?) {
        _recentSearchList.postValue(searchList)
    }

    fun searchInterests(query: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val index = client.initIndex(IndexName("interests"))
            val response = index.search(com.algolia.search.model.search.Query(query))
            val results = response.hits.deserialize(InterestItem.serializer())
            val searchData = mutableListOf<SearchQuery>()
            for (result in results) {
                val searchQuery = SearchQuery(result.objectID, result.content, System.currentTimeMillis(), QUERY_TYPE_INTEREST)
                searchData.add(searchQuery)
            }

            setSearchData(searchData)

            insertInterests(*results.toTypedArray())
        } catch (e: Exception) {
            setCurrentError(e)
        }

    }

    private fun insertInterests(vararg interests: InterestItem) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertInterestItems(interests.toList())
    }

    val recentPostSearchList = MutableLiveData<List<PostMinimal2>>()
    val recentUserSearchList = MutableLiveData<List<UserMinimal2>>()

    @Suppress("UNCHECKED_CAST")
    fun search(query: String) = viewModelScope.launch (Dispatchers.IO) {
        val newQueries = mutableListOf<IndexedQuery>()
        val iq = IndexQuery(
            IndexName("posts"), com.algolia.search.model.search.Query(query)
        )

        val iq1 = IndexQuery(IndexName("users"), com.algolia.search.model.search.Query(query))

        newQueries.add(iq)
        newQueries.add(iq1)

        try {
            val response: ResponseMultiSearch = client.search(newQueries)

            val list = response.results as List<ResultMultiSearch<ResponseSearch>>

            val usersList = mutableListOf<UserMinimal2>()
            val postsList = mutableListOf<PostMinimal2>()

            val currentUser = UserManager.currentUser

            val blockedUsers = currentUser.blockedUsers
            val blockedBy = currentUser.blockedBy

            val searchList = mutableListOf<SearchQuery>()
            for (result in list) {
                for (hit in result.response.hits) {

                    val type = hit.json["type"].toString()

                    if (type == "\"user\"") {
                        val user = hit.deserialize(UserMinimal2.serializer())
                        val searchQuery = SearchQuery(user.objectID, user.name, System.currentTimeMillis(), QUERY_TYPE_USER)

                        if (!(currentUser.blockedUsers.contains(user.objectID) || currentUser.blockedBy.contains(user.objectID))) {
                            searchList.add(searchQuery)
                            usersList.add(user)
                        }
                    } else {
                        val post = hit.deserialize(PostMinimal2.serializer())
                        val searchQuery = SearchQuery(post.objectID, post.name, System.currentTimeMillis(), QUERY_TYPE_POST)

                        if (!(blockedUsers.contains(post.creator.userId) || blockedBy.contains(post.creator.userId))) {
                            searchList.add(searchQuery)
                            postsList.add(post)
                        }
                    }
                }
            }


/*
            val finalPostsList = postsList.filter {
                !(blockedUsers.contains(it.creator.userId) || blockedBy.contains(it.creator.userId))
            }

            val finalUsersList = usersList.filter {
                !(currentUser.blockedUsers.contains(it.objectID) || currentUser.blockedBy.contains(it.objectID))
            }*/

            recentPostSearchList.postValue(postsList)
            recentUserSearchList.postValue(usersList)

            setSearchData(searchList)
        } catch (e: Exception) {
            Log.e(TAG, "search: ${e.localizedMessage}")
        }
    }

    fun insertUsers(vararg users: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }

    fun insertUsers(users: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }

    // saving the user in both memory and cache
    fun saveUser(user: User) = viewModelScope.launch (Dispatchers.IO) {
        userCache[user.id] = user
        repo.insertUser(user)
    }

    // make sure the user in this comment is not null or empty
    val replyToContent = MutableLiveData<Comment>()

    fun setCurrentError(exception: Exception?) {
        _currentError.postValue(exception)
    }

    fun setCurrentPost(post: Post?) {
        _currentPost.postValue(post)
    }

    fun setCurrentPostTitle(title: String) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.name = title
            setCurrentPost(existingPost)
        }
    }

    fun setCurrentPostContent(content: String) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.content = content
            setCurrentPost(existingPost)
        }
    }

    fun setCurrentPostImages(images: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.images = images
            setCurrentPost(existingPost)
        }
    }

    fun addToExistingPostImages(images: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val existingImages = existingPost.images.toMutableList()
            existingImages.addAll(images)
            existingPost.images = existingImages
            setCurrentPost(existingPost)
        }
    }

    fun addTagsToCurrentPost(tags: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val newList = existingPost.tags.toMutableList()
            newList.addAll(tags)
            existingPost.tags = newList.distinct()
            setCurrentPost(existingPost)
        }
    }

    fun setCurrentPostTags(tags: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.tags = tags
            setCurrentPost(existingPost)
        }
    }

    fun setCurrentPostLocation(location: Location) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.location = location
            setCurrentPost(existingPost)
        }
    }

    fun insertCurrentUser(localUser: User) = viewModelScope.launch (Dispatchers.IO) {
        localUser.isCurrentUser = true
        insertUser(localUser)
    }

    fun insertUser(localUser: User) = viewModelScope.launch (Dispatchers.IO) {
        userCache[localUser.id] = localUser
        repo.insertUser(localUser)
    }

    fun deletePostImageAtPosition(pos: Int) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val existingImages = existingPost.images.toMutableList()
            existingImages.removeAt(pos)
            existingPost.images = existingImages
            setCurrentPost(existingPost)
        }
    }

    fun createPost(onComplete: (task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val post = currentPost.value!!
        FireUtility.createPost(post) {
            if (it.isSuccessful) {
                post.isMadeByMe = true
                insertNewPost(post)
            } else {
                setCurrentError(it.exception)
            }
            onComplete(it)
        }
    }

    fun setCurrentImage(image: Uri?) {
        _currentImage.postValue(image)
    }

    fun uploadImage(locationId: String, image: Uri, onComplete: (downloadUri: Uri?) -> Unit) {
        FireUtility.uploadImage(locationId, image, onComplete)
    }

    fun updateUser(updatedUser: User, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.updateUser2(changes) {
            onComplete(it)
            if (it.isSuccessful) {
                insertUser(updatedUser)
                insertCurrentUser(updatedUser)
                updateLocalPosts(updatedUser, updatedUser.posts)
                updateLocalMessages(updatedUser)
            } else {
                setCurrentError(it.exception)
            }
        }
    }

    private fun updateLocalMessages(updatedUser: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalMessages(updatedUser)
    }

    private fun updateLocalPosts(updatedUser: User, posts: List<String>) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalPosts(updatedUser, posts)
    }

    fun checkIfUsernameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.checkIfUserNameTaken(username, onComplete)
    }

    fun signOut(onComplete: () -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        repo.clearDatabases(onComplete)
    }

    @ExperimentalPagingApi
    fun getPostsNearMe(): Flow<PagingData<Post>>{
        return Pager(
            config = PagingConfig(pageSize = 20)
        ) {
            repo.postDao.getPostsNearMe()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getFeedItems(query: Query, tag: String? = null): Flow<PagingData<Post>> {

        val feedSetting = feedOption.value!!
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return if (tag != null) {

            val formattedTag = "%$tag%"

            Pager(
                config = PagingConfig(pageSize = 20),
                remoteMediator = PostRemoteMediator(query, repo, true) {
                    !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
                }
            ) {
                when (feedSetting.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        repo.postDao.getTagPostsByContributors(formattedTag)
                    }
                    LIKES -> {
                        repo.postDao.getTagPostsByLikes(formattedTag)
                    }
                    MOST_VIEWED -> {
                        repo.postDao.getTagPostsByViews(formattedTag)
                    }
                    MOST_RECENT -> {
                        repo.postDao.getTagPostsByTime(formattedTag)
                    }
                    FeedSort.LOCATION -> {
                        repo.postDao.getTagPostsByTime(formattedTag)
                    }
                }
            }.flow.cachedIn(viewModelScope)
        } else {
            Pager(
                config = PagingConfig(pageSize = 20),
                remoteMediator = PostRemoteMediator(query, repo, true) {
                    !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
                }
            ) {
                when (feedSetting.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        repo.postDao.getPagedPostsByContributors()
                    }
                    LIKES -> {
                        repo.postDao.getPagedPostsByLikes()
                    }
                    MOST_VIEWED -> {
                        repo.postDao.getPagedPostsByViews()
                    }
                    MOST_RECENT -> {
                        repo.postDao.getPagedPostsByTime()
                    }
                    FeedSort.LOCATION -> {
                        repo.postDao.getPagedPostsByTime()
                    }
                }
            }.flow.cachedIn(viewModelScope)
        }
    }

    private fun insertNewPost(post: Post) = viewModelScope.launch(Dispatchers.IO) {
        val currentUser = UserManager.currentUser
        repo.insertPosts(arrayOf(post))
        currentUser.postsCount += 1

        val newPostsList = currentUser.posts.addItemToList(post.id)
        currentUser.posts = newPostsList

        val newChannelsList = currentUser.chatChannels.addItemToList(post.chatChannel)
        currentUser.chatChannels = newChannelsList

        insertCurrentUser(currentUser)
    }

    fun getCurrentUserPosts(): LiveData<List<Post>> {
        return repo.getCurrentUserPosts()
    }

    @ExperimentalPagingApi
    fun getCurrentUserPosts(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
            }
        ) {
            repo.postDao.getCurrentUserPagedPosts()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getPagedPostRequests(): Flow<PagingData<PostRequest>> {
        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection("postRequests")
            .whereEqualTo("receiverId", currentUser.id)
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRequestRemoteMediator(query, repo)
        ) {
            repo.postRequestDao.getPagedPostRequests(currentUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getCollaborations(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val currentUserId = UserManager.currentUserId
        val blockedBy = UserManager.currentUser.blockedBy
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
            }
        ) {
            repo.postDao.getPagedCollaborations("%${currentUserId}%", currentUserId)
        }.flow.cachedIn(viewModelScope)
    }


    @ExperimentalPagingApi
    fun getOtherUserPosts(query: Query, otherUser: User): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
            }
        ) {
            repo.postDao.getPagedOtherUserPosts(otherUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getOtherUserCollaborations(query: Query, otherUser: User): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
            }
        ) {
            repo.postDao.getOtherUserPagedCollaborations("%${otherUser.id}%", otherUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    /*fun getOtherUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        FireUtility.getDocument(ref, onComplete)
    }*/

    fun likeLocalUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.likeLocalUserById(userId)
    }

    fun dislikeLocalUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.dislikeLocalUserById(userId)
    }

    /*@ExperimentalPagingApi
    fun getSavedPosts(query: Query): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo)
        ) {
            repo.postDao.getPagedSavedPosts()
        }.flow.cachedIn(viewModelScope)
    }*/


    fun sendComment(comment: Comment, parent: Any, onComplete: (task: Task<Void>) -> Unit) {
        val parentChannelId: String?
        val currentUser = UserManager.currentUser
        val notification = when (parent) {
            is Post -> {
                parentChannelId = null
                val content = currentUser.name + " commented on your post"
                val title = parent.name
                Notification.createNotification(
                    content,
                    parent.creator.userId,
                    commentId = comment.commentId,
                    title = title
                )
            }
            is Comment -> {
                parentChannelId = parent.commentChannelId
                val content = currentUser.name + " replied to your comment"
                Notification.createNotification(
                    content,
                    parent.senderId,
                    commentId = parent.commentId
                )
            }
            else -> {
                throw IllegalArgumentException("Only post and comment object is accepted.")
            }
        }

        FireUtility.sendComment(comment, parentChannelId) {

            insertComment(comment)

            onComplete(it)

            if (it.isSuccessful) {

                onCommentSend(comment)

                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (!it1.isSuccessful) {
                                        setCurrentError(it1.exception)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                setCurrentError(it.exception)
            }
        }
    }

    private fun onCommentSend(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        val post = getLocalPost(comment.postId)
        if (post != null) {
            post.commentsCount += 1
            insertPost(post)
        }

        if (comment.commentLevel >= 1) {
            val parentComment1 = repo.getComment(comment.parentId)
            if (parentComment1 != null) {
                parentComment1.repliesCount += 1
                insertComment(parentComment1)
            }
        }

        insertComment(comment)
    }

    private fun insertComment(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertComments(listOf(comment))
    }

    @ExperimentalPagingApi
    fun getPagedComments(commentChannelId: String, query: Query): Flow<PagingData<Comment>> {
        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = CommentRemoteMediator(query, repo) { comment ->
                !(currentUser.blockedUsers.contains(comment.senderId) || currentUser.blockedBy.contains(comment.senderId))
            }
        ) {
            repo.commentDao.getPagedComments(commentChannelId)
        }.flow
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return repo.getLocalChannelContributors(chatChannel)
    }

    fun updatePost(onComplete: (Post, task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val currentPost = currentPost.value!!
        FireUtility.updatePost(currentPost, onComplete)
    }


    fun updateLocalPost(post: Post) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalPost(post)
    }

    private suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
        return repo.getLocalChatChannel(chatChannel)
    }

    fun getLimitedMediaMessages(chatChannelId: String, limit: Int, type: String = image, onComplete: (List<Message>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getLimitedMediaMessages(chatChannelId, limit, type))
    }

    fun insertMessages(imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertMessages(imagesDir, documentsDir, messages)
    }

    fun insertChatChannels(chatChannels: List<ChatChannel>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertChatChannels(chatChannels)
    }

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return repo.getDocumentMessages(chatChannelId)
    }

    fun getTagPosts(tag: String, query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo, false) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors).isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(it.contributors).isNotEmpty()))
            }
        ) {
            repo.postDao.getTagPosts("%$tag%")
        }.flow.cachedIn(viewModelScope)
    }

    fun setCurrentPostLinks(links: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.sources = links
            setCurrentPost(existingPost)
        }
    }

    fun deleteComment(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteComment(comment)
        updateLocalPostAfterCommentDeletion(comment)
        if (comment.parentCommentChannelId != null) {
            updateRepliesCountOfParentCommentById(comment.parentId)
        }
    }

    fun updateComment(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateComment(comment)
    }

    fun deleteUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteUserById(userId)
    }

    suspend fun getImageMessages(chatChannelId: String, limit: Int = 0): List<Message> {
        return repo.getImageMessages(chatChannelId, limit)
    }

    fun updateReadList(imagesDir: File, documentsDir: File, message: Message) = viewModelScope.launch (Dispatchers.IO) {
        val chatChannel = getLocalChatChannel(message.chatChannelId)
        if (chatChannel != null) {
            val currentUserId = UserManager.currentUserId
            FireUtility.updateReadList(chatChannel, message) {
                if (it.isSuccessful) {
                    val newList = message.readList.addItemToList(currentUserId)
                    message.readList = newList

                    if (message.type == text) {
                        message.isDownloaded = true
                    }

                    insertMessages(imagesDir, documentsDir, listOf(message))
                } else {
                    setCurrentError(it.exception)
                }
            }
        }
    }

   /* suspend fun getChatChannel(channelId: String): Result<DocumentSnapshot> {
        val ref = Firebase.firestore.collection("chatChannels").document(channelId)
        return FireUtility.getDocument(ref)
    }*/

    /*fun getChatChannel(channelId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("chatChannels").document(channelId)
        FireUtility.getDocument(ref, onComplete)
    }*/

    fun getChatChannel(channelId: String, onComplete: (ChatChannel?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(chatRepository.getChatChannel(channelId))
    }

    fun getNotifications(type: Int = 0): Flow<PagingData<Notification>> {
        val currentUserId = UserManager.currentUserId
        val query = Firebase.firestore.collection(USERS)
            .document(currentUserId)
            .collection(NOTIFICATIONS)

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = NotificationRemoteMediator(query, repo)
        ) {
            repo.notificationDao.getNotifications(currentUserId, type)
        }.flow.cachedIn(viewModelScope)
    }

    fun insertPosts(posts: List<Post>) = viewModelScope.launch (Dispatchers.IO) {
        for (post in posts) {
            postCache[post.id] = post
        }
        repo.insertPosts(posts.toTypedArray())
    }


    val testImage = MutableLiveData<Uri>().apply { value = null }

    fun insertPosts(vararg posts: Post) = viewModelScope.launch (Dispatchers.IO) {
        for (post in posts) {
            postCache[post.id] = post
        }
        repo.insertPosts(posts)
    }

    private val _reportUploadImages = MutableLiveData<List<Uri>>()
    val reportUploadImages: LiveData<List<Uri>> = _reportUploadImages

    fun setReportUploadImages(images: List<Uri>) {
        _reportUploadImages.postValue(images)
    }

    fun sendFeedback(feedback: Feedback, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.sendFeedback(feedback, onComplete)
    }

    fun updateNotification(notification: Notification) = viewModelScope.launch (Dispatchers.IO) {
        repo.notificationDao.insert(notification)
    }

    fun insertNotifications(notifications: List<Notification>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertNotifications(notifications)
    }

    fun insertNotifications(vararg notifications: Notification) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertNotifications(notifications)
    }

    fun insertPostRequests(requests: List<PostRequest>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertPostRequests(requests)
    }

    fun insertPostRequests(vararg postRequest: PostRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertPostRequests(postRequest)
    }

    fun insertSearchQuery(searchQuery: SearchQuery) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertSearchQuery(searchQuery)
    }

    fun setOtherUserAsAdmin(chatChannelId: String, userId: String, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.setOtherUserAsAdmin(chatChannelId, userId, onComplete)
    }

    fun removeOtherUserFromAdmin(chatChannelId: String, userId: String, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.removeUserFromAdmin(chatChannelId, userId, onComplete)
    }

    private suspend fun getLocalPost(postId: String): Post? {
        return repo.getPost(postId)
    }

    fun deleteNotification(notification: Notification) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteNotification(notification)
    }

    fun deletePostRequest(postRequest: PostRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.deletePostRequest(postRequest)
    }

    @ExperimentalPagingApi
    fun getPostInvites(): Flow<PagingData<PostInvite>> {
        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection(USERS)
            .document(currentUser.id)
            .collection(INVITES)

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostInviteRemoteMediator(query, repo)
        ) {
            repo.postInviteDao.getPostInvites()
        }.flow.cachedIn(viewModelScope)
    }

    // in future this should not be private
    fun deletePostInvite(postInvite: PostInvite) = viewModelScope.launch (Dispatchers.IO) {
        repo.deletePostInvite(postInvite)
    }

    fun insertPostInvites(vararg newPostInvites: PostInvite) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertPostInvites(newPostInvites)
    }

    fun deleteNotificationById(id: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteNotificationById(id)
    }

    fun deleteLocalPost(post: Post) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteLocalPost(post)
    }

    /*fun getArchivedPosts(query: Query): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo, false)
        ) {
            repo.postDao.getArchivedPosts()
        }.flow.cachedIn(viewModelScope)
    }*/

    fun deleteAdPosts() = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteAdPosts()
    }

    fun deleteLocalPostRequest(postRequest: PostRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.deletePostRequest(postRequest)
    }

    fun deleteLocalChatChannelById(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteLocalChatChannelById(chatChannelId)
    }

    /*fun getReactiveUser(userId: String): LiveData<User> {
        return repo.getReactiveUser(userId)
    }

    fun getReactivePost(postId: String): LiveData<Post> {
        return repo.getReactivePost(postId)
    }*/

    /*fun getPostSupporters(query: Query, postId: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = UserRemoteMediator(query, repo)
        ) {
            repo.userDao.getPostSupporters("%$postId%")
        }.flow.cachedIn(viewModelScope)
    }*/

    /*fun getUserSupporters(query: Query, userId: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = UserRemoteMediator(query, repo)
        ) {
            repo.userDao.getUserSupporters("%$userId%")
        }.flow.cachedIn(viewModelScope)
    }*/

    fun getMyPostRequests(query: Query): Flow<PagingData<PostRequest>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRequestRemoteMediator(query, repo)
        ) {
            repo.postRequestDao.getMyPostRequests(UserManager.currentUserId)
        }.flow.cachedIn(viewModelScope)
    }

    fun getCachedUser(senderId: String): User? {
        return userCache[senderId]
    }

    fun disableLocationBasedPosts() = viewModelScope.launch (Dispatchers.IO) {
        repo.disableLocationBasedPosts()
    }

    private fun getPostRequest(postId: String, onComplete: (PostRequest?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getPostRequest(postId))
    }

    /* Chat related functions */

    /**
     * List of chat channels for ForwardFragment
     * */
    private val _forwardList = MutableLiveData<List<ChatChannel>>()
    val forwardList: LiveData<List<ChatChannel>> = _forwardList

    /**
     * Flag for holding the current state of the chat, whether messages are being selected or not
     * */
    var isSelectModeOn = false

    /**
     * Placeholder for the current message that has been selected to be replied to
     * */
    private val _replyMessage = MutableLiveData<Message>().apply { value = null }
    val replyMessage: LiveData<Message> = _replyMessage

    /**
     * List of images to be uploaded in the current chat
     * */
    private val _chatImagesUpload = MutableLiveData<List<Uri>>()
    val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload

    /**
     * List of documents to be uploaded in the current chat
     * */
    private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
    val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload

    /**
     * To get messages for ChatFragment in paged order
     *
     * @param chatChannelId Channel to get messages from local database
     * @param query FireBase query to get messages on boundary callbacks
     *
     * @return A flow of messages in paged order by position in local database by recent order
     * */
    fun getPagedMessages(chatChannelId: String, query: Query): Flow<PagingData<Message>> {
        val currentUser = UserManager.currentUser
        return Pager(config =
        PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            maxSize = 150,
            prefetchDistance = 25,
            initialLoadSize= 40),
            remoteMediator = MessageRemoteMediator(
                query,
                chatRepository
            ) {
                !(currentUser.blockedUsers.contains(it.senderId) || currentUser.blockedBy.contains(it.senderId))
            }
        ) {
            chatRepository.messageDao.getChannelPagedMessages(chatChannelId)
        }.flow.cachedIn(viewModelScope)
    }

    /**
     * To add channel to forward list that is accessed by ForwardFragment
     * @param chatChannel The chat channel to add to the list
     * */
    fun addChannelToForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null) {
            val newList = oldList.toMutableList()
            newList.add(chatChannel)
            _forwardList.postValue(newList)
        } else {
            _forwardList.postValue(listOf(chatChannel))
        }
    }


    /**
     * To remove channel from the forward list which is accessed by ForwardFragment
     * @param chatChannel The chat channel to remove from the list
     * */
    fun removeChannelFromForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null && oldList.isNotEmpty()) {
            val newList = oldList.toMutableList()
            newList.remove(chatChannel)
            _forwardList.postValue(newList)
        }
    }

    /**
     * To clear all data present to list accessed by ForwardFragment
     * */
    fun clearForwardList() {
        _forwardList.postValue(emptyList())
    }


    /**
     * To upload message to firestore
     *
     * @param chatChannelId The chat channel id to where this post belongs
     * @param content Content of the text message
     * @param replyTo An optional message id attached to the current message to be send which is connected respectively
     * @param replyMessage An optional message attached to the current message in minified form
     * */
    fun sendTextMessage(chatChannelId: String, content: String, replyTo: String? = null, replyMessage: MessageMinimal? = null) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendTextMessage(chatChannelId, content, replyTo, replyMessage)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                chatRepository.insertMessage(result.data, true)
            }
        }
    }

    /**
     * To send multiple messages together, may include message of all type [image, document, text]
     *
     * @param chatChannelId The chat channel id to where this post belongs
     * @param listOfMessages The messages to be sent
     *
     * */
    fun sendMessagesSimultaneously(chatChannelId: String, listOfMessages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendMessagesSimultaneously(chatChannelId, listOfMessages)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {

                val messages = result.data
                chatRepository.insertMessages(messages)

                setChatUploadImages(emptyList())
                setChatUploadDocuments(emptyList())
            }
        }
    }

    /**
     * To fill [chatImagesUpload] with images retrieved by the user
     * */
    fun setChatUploadImages(images: List<Uri>) {
        _chatImagesUpload.postValue(images)
    }

    /**
     * To fill [chatDocumentsUpload] with documents retrieved by the user
     * */
    fun setChatUploadDocuments(documents: List<Uri>) {
        _chatDocumentsUpload.postValue(documents)
    }

    /**
     * Get all channels for forward fragment based on the user id provided, from local database wrapped in livedata
     *
     * @param userId The userId to filter channels for ForwardFragment
     * @return A list of chat channel for ForwardFragment wrapped in livedata
     *
     * */
    fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return chatRepository.getForwardChannels("%$userId%")
    }

    /**
     * To send messages to selected chat channels as forward messages
     *
     * @param messages messages selected to be forwarded
     * @param channels list of chat channels to which the selected messages needs to be forwarded
     * @param onComplete A callback function that is invoked on completion of sending all the messages to their respective chat channels
     *
     * */
    fun sendForwardsToChatChannels(messages: List<Message>, channels: List<ChatChannel>, onComplete: (result: Result<List<Message>>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val result = FireUtility.sendMultipleMessageToMultipleChannels(messages, channels)
        when (result) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                val newMessages = result.data
                chatRepository.insertMessages(newMessages)
            }
        }
        onComplete(result)
    }

    /**
     * To get the currently selected messages in a chat channel
     *
     * @param chatChannelId The channel id for which messages are being selected
     * @return A list of messages wrapped in livedata
     * */
    fun selectedMessages(chatChannelId: String): LiveData<List<Message>> {
        return chatRepository.selectedMessages(chatChannelId)
    }

    /**
     * To set message as attached reply for another message to be sent currently
     *
     * @param message The message to be attached
     * */
    fun setReplyMessage(message: Message?) {
        _replyMessage.postValue(message)
        if (message != null) {
            disableSelectMode(message.chatChannelId)
        }
    }


    /**
     * To get a live version of the current chat Channel wrapped in livedata from local database
     *
     * @param chatChannelId Id of the chat channel to retrieve
     * @return A reactive chat channel that reacts to changes in the local database
     *
     * */
    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return chatRepository.getReactiveChatChannel(chatChannelId)
    }

    /**
     * To disable select mode and update all messages that are in select mode in local database
     * @see isSelectModeOn
     * */
    fun disableSelectMode(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = false
        chatRepository.updateMessages(chatChannelId, MESSAGE_IDLE)
    }


    /**
     * To enable select mode
     * @see isSelectModeOn
     *
     * @param firstSelectedMessage The message which invoked select mode
     * */
    fun enableSelectMode(firstSelectedMessage: Message) = viewModelScope.launch (Dispatchers.IO) {
        isSelectModeOn = true
        chatRepository.updateMessages(firstSelectedMessage.chatChannelId,
            MESSAGE_READY
        )

        delay(300)

        firstSelectedMessage.state = MESSAGE_SELECTED
        updateMessage(firstSelectedMessage)
    }


    /**
     * To update a message in local database
     *
     * @param message The message to be updated
     * */
    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        Log.d(TAG, "Updating message: ${message.state}")
        chatRepository.updateMessage(message)
    }

    /**
     * To remove an image from the [chatImagesUpload] list
     *
     * @param position The position where the image is to be removed
     * */
    fun removeImageAtPosition(position: Int) {
        val existingList = chatImagesUpload.value
        if (existingList != null) {
            val currentList = existingList.toMutableList()
            currentList.removeAt(position)
            _chatImagesUpload.postValue(currentList)
        }
    }

    /**
     * To remove a document from the [chatDocumentsUpload] list
     *
     * @param position The position where the document is to be removed
     * */
    fun removeDocumentAtPosition(position: Int) {
        val existingList = chatDocumentsUpload.value
        if (existingList != null) {
            val currentList = existingList.toMutableList()
            currentList.removeAt(position)
            _chatDocumentsUpload.postValue(currentList)
        }
    }

    fun getContributors(chatChannelId: String, limit: Int = 6): LiveData<List<User>> {
        return userRepository.getContributors(chatChannelId, limit)
    }

    fun setListenerForEmailVerification() = viewModelScope.launch (Dispatchers.IO) {

        // this is just to check if the email is already verified
        val res = userVerificationResult.value
        if (res != null) {
            if (res is Result.Success && res.data) {
                return@launch
            }
        }

        userRepository.setListenerForEmailVerification(60, 5)
    }

    fun removePostFromUserLocally(chatChannelId: String, postId: String, user: User) {
        val newList = user.chatChannels.removeItemFromList(chatChannelId)
        user.chatChannels = newList
        val newList1 = user.collaborations.removeItemFromList(postId)
        user.collaborations = newList1
        user.collaborationsCount -= 1
        insertUsers(user)
    }

    fun clearAllChannels() = viewModelScope.launch (Dispatchers.IO) {
        chatRepository.clearChatChannels()
    }

    // We cannot set snapshot listener in every post that's why,
    // when a post request is accepted, to reflect the changes locally,
    // we need to check if something has changed in user document and respectively
    // make the changes locally.
    fun checkAndUpdateLocalPosts(currentUser: User) = viewModelScope.launch (Dispatchers.IO) {
        for (post in currentUser.collaborations) {
            val mPost = getLocalPost(post)
            if (mPost != null) {
                if (mPost.isRequested || !mPost.isCollaboration) {
                    mPost.isRequested = false
                    mPost.isCollaboration = true

                    val newContList = mPost.contributors.addItemToList(currentUser.id)
                    mPost.contributors = newContList

                    mPost.updatedAt = System.currentTimeMillis()

                    getPostRequest(mPost.id) {
                        if (it != null) {
                            val newRequestsList = mPost.requests.removeItemFromList(it.requestId)
                            mPost.requests = newRequestsList

                            deletePostRequest(it)
                        }
                        updateLocalPost(mPost)
                    }
                }
            }
        }
    }

    fun insertUserToCache(creator: User) {
        userCache[creator.id] = creator
    }

    fun getCachedPost(id: String): Post? {
        return postCache[id]
    }

    fun insertPostToCache(post: Post) {
        postCache[post.id] = post
    }

    fun getUser(senderId: String, function: (User?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        function(repo.getUser(senderId))
    }

    fun getPost(postId: String, function: (Post?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        function(repo.getPost(postId))
    }

    fun deletePostById(postId: String) = viewModelScope.launch (Dispatchers.IO) {
        if (postCache.containsKey(postId)) {
            postCache.remove(postId)
        }

        repo.deletePostById(postId)
    }

    private val _googleSignInError = MutableLiveData<Int?>()
    val googleSignInError: LiveData<Int?> = _googleSignInError

    fun setGoogleSignInError(code: Int) {
        if (code == -1) {
            _googleSignInError.postValue(null)
        } else {
            _googleSignInError.postValue(code)
        }
    }

    fun getLikes(query: Query): Flow<PagingData<LikedBy>>{
        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = LikedByRemoteMediator(query, repo) {
                !(currentUser.blockedUsers.contains(it.id) || currentUser.blockedBy.contains(it.id))
            }
        ) {
            repo.likedByDao.getLikedBy()
        }.flow
    }

    fun getReferenceItems(query: Query): Flow<PagingData<ReferenceItem>> {
        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ReferenceItemRemoteMediator(query, repo) {
                !(currentUser.blockedUsers.contains(it.id) || currentUser.blockedBy.contains(it.id))
            }
        ) {
            repo.referenceItemDao.getReferenceItems()
        }.flow
    }

    fun deleteReferenceItem(itemId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteReferenceItem(itemId)
    }

    private fun updateRepliesCountOfParentCommentById(parentCommentId: String) = viewModelScope.launch (Dispatchers.IO) {
        val parentComment = repo.getComment(parentCommentId)
        if (parentComment != null) {
            Log.e(TAG, "updateRepliesCountOfParentCommentById: found parent comment with id $parentCommentId")
            parentComment.repliesCount -= 1
            insertComment(parentComment)
        } else {
            Log.e(TAG, "updateRepliesCountOfParentCommentById: parent comment was null with id $parentCommentId")
        }
    }

    private fun updateLocalPostAfterCommentDeletion(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        val totalCommentsDeletedFromThePost = 1 + comment.repliesCount
        getPost(comment.postId) {
            if (it != null) {
                it.commentsCount -= totalCommentsDeletedFromThePost
                insertPosts(it)
            }
        }
    }

    fun insertPost(post: Post) = viewModelScope.launch (Dispatchers.IO) {
        insertPostToCache(post)
        repo.insertPosts(arrayOf(post))
    }

    private val _isNewPostCreated = MutableLiveData<Boolean?>()
    val isNewPostCreated: LiveData<Boolean?> = _isNewPostCreated

    fun setCreatedNewPost(b: Boolean?) {
        _isNewPostCreated.postValue(b)
    }

    fun getPagedInterestItems(query: Query): Flow<PagingData<InterestItem>> {
        return Pager(
            config = PagingConfig(pageSize = 50),
            remoteMediator = InterestItemRemoteMediator(repo, query)
        ) {
            repo.interestItemDao.getPagedInterestItems()
        }.flow.cachedIn(viewModelScope)
    }

    fun uncheckInterestItem(interestItem: InterestItem) = viewModelScope.launch (Dispatchers.IO) {
        interestItem.isChecked = false
        repo.insertInterestItems(listOf(interestItem))
    }

    fun checkInterestItem(interestItem: InterestItem) = viewModelScope.launch (Dispatchers.IO) {
        interestItem.isChecked = true
        repo.insertInterestItems(listOf(interestItem))
    }

    fun insertInterestItem(interestItem: InterestItem) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertInterestItems(listOf(interestItem))
    }

    fun getUnreadGeneralNotifications(): LiveData<List<Notification>> {
        return repo.getUnreadGeneralNotifications()
    }

    fun getUnreadRequestNotifications(): LiveData<List<Notification>> {
        return repo.getUnreadRequestNotifications()
    }

    fun getUnreadInviteNotifications(): LiveData<List<Notification>> {
        return repo.getUnreadInviteNotifications()
    }

    fun updateChatChannel(chatChannel: ChatChannel) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateChatChannel(chatChannel)
    }

    fun getUnreadChatChannels() : LiveData<List<ChatChannel>> {
        return repo.getUnreadChatChannels()
    }

    fun deleteCommentsByUserId(id: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteCommentsByUserId(id)
    }

    fun deletePostsByUserId(id: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deletePostsByUserId(id)
    }

    fun deletePreviousSearchByUserId(id: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deletePreviousSearchByUserId(id)
    }


    /* Chat related functions end */



    /* User related functions */
    var userVerificationResult: LiveData<Result<Boolean>> = userRepository.userVerificationResult


    /* User related functions end */

























    companion object {
        private const val TAG = "MainViewModel"
        const val MESSAGE_IDLE = -1
        const val MESSAGE_SELECTED = 1
        const val MESSAGE_READY = 0
    }

}