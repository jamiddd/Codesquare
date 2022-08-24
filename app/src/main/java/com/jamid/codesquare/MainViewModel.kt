package com.jamid.codesquare

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.algolia.search.client.ClientSearch
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.multipleindex.IndexQuery
import com.algolia.search.model.multipleindex.IndexedQuery
import com.algolia.search.model.response.ResponseSearch
import com.algolia.search.model.response.ResultMultiSearch
import com.android.billingclient.api.ProductDetails
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.form.LoginFormState
import com.jamid.codesquare.data.form.RegisterFormState
import com.jamid.codesquare.data.form.UpdateUserFormState
import com.jamid.codesquare.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.set

typealias AlgoliaQuery = com.algolia.search.model.search.Query
// something simple
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: MainRepository
    val chatRepository: ChatRepository
    private val userRepository: UserRepository

    private val _competitions = MutableLiveData<List<Competition>>().apply { value = emptyList() }
    val competitions: LiveData<List<Competition>> = _competitions

    init {
        val db = CollabDatabase.getInstance(application.applicationContext)
        repo = MainRepository.getInstance(db)
        chatRepository = ChatRepository(db, viewModelScope, application.applicationContext)
        userRepository = UserRepository(db, viewModelScope)

        /* TODO("Think about this, if this is necessary") */
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearPosts()
        }

//        someFix()

    }

    private val _locationPermission = MutableLiveData<Boolean>()
    /*val locationPermission: LiveData<Boolean> = _locationPermission*/

    private val _readPermission = MutableLiveData<Boolean>()
    val readPermission: LiveData<Boolean> = _readPermission

    fun someFix() = viewModelScope.launch(Dispatchers.IO) {
//        chatRepository.messageDao.deleteMessagesAfter()

        /*Firebase.firestore.collection(CHAT_CHANNELS)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {
                    val batch = Firebase.firestore.batch()
                    it.forEach { it1 ->
                        batch.update(it1.reference, mapOf("data1" to null, "data2" to null))
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "someFix: Success")
                        }
                        .addOnFailureListener { it1 ->
                            Log.e(TAG, "someFix: ${it1.localizedMessage}")
                        }

                }
            }.addOnFailureListener {
                Log.e(TAG, "someFix: ${it.localizedMessage}")
            }


        Firebase.firestore.collectionGroup("likedBy")
            .get()
            .addOnSuccessListener {
                viewModelScope.launch(Dispatchers.IO) {
                    if (it != null && !it.isEmpty) {
                        val batch = Firebase.firestore.batch()

                        it.forEach { it1 ->
                            val user = FireUtility.getUserSync(it1.id)
                            if (user != null) {
                                batch.set(it1.reference, user.minify())
                            }
                        }

                        val task = batch.commit()
                        task.await()

                        if (task.isSuccessful) {
                            Log.d(TAG, "someFix: task finished")
                        } else {
                            Log.d(TAG, "someFix: ${task.exception?.localizedMessage}")
                        }
                    }
                }


            }.addOnFailureListener {
                Log.e(TAG, "someFix: ${it.localizedMessage}")
            }*/
    }

    private val _externallyCreatedDocument = MutableLiveData<Uri?>()
    val externallyCreatedDocument: LiveData<Uri?> = _externallyCreatedDocument

    fun setExternallyCreatedDocumentUri(uri: Uri?) {
        _externallyCreatedDocument.postValue(uri)
    }

    private val _currentError = MutableLiveData<Exception?>()

    val currentError: LiveData<Exception?> = _currentError
    val currentUser: LiveData<User> = repo.currentUser

    /**
     * A placeholder post to be used while creating new post.
     * */
    private val _currentPost = MutableLiveData<Post>().apply { value = null }
    val currentPost: LiveData<Post> = _currentPost

    private val defaultFeedOption = FeedOption("Random", FeedSort.MOST_RECENT, FeedOrder.DESC)

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
    private val _productDetails =
        MutableLiveData<List<ProductDetails>>().apply { value = emptyList() }
    val productDetails: LiveData<List<ProductDetails>> = _productDetails


    fun setProductDetailsList(productDetails: List<ProductDetails> = emptyList()) {
        _productDetails.postValue(productDetails)
    }

    fun setCurrentFeedOption(feedOption: FeedOption) {
        _feedOption.postValue(feedOption)
    }

    fun setDefaultFeedOption() {
        _feedOption.postValue(defaultFeedOption)
    }

    /**
     * List of all the available subscriptions for the user
     * */
    private val _products = MutableLiveData<List<OneTimeProduct>>().apply { value = emptyList() }
//    val products: LiveData<List<OneTimeProduct>> = _products

    fun setProducts(list: List<OneTimeProduct> = emptyList()) {
        _products.postValue(list)
    }


    fun chatChannels(currentUserId: String) = repo.chatChannels(currentUserId)
    val errors = repo.errors

    fun messageRequests(): LiveData<List<ChatChannel>> {
        return repo.messageRequests()
    }

    val allUnreadNotifications = repo.allUnreadNotifications

    val allPreviousQueries = repo.allPreviousQueries

    private val _recentSearchList = MutableLiveData<List<SearchQuery>>().apply { value = null }
    val recentSearchList: LiveData<List<SearchQuery>> = _recentSearchList

    private val client =
        ClientSearch(ApplicationID(BuildConfig.ALGOLIA_ID), APIKey(BuildConfig.ALGOLIA_SECRET))

    fun setSearchData(searchList: List<SearchQuery>?) {
        _recentSearchList.postValue(searchList)
    }

    fun searchInterests(query: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val index = client.initIndex(IndexName("interests"))
            val response = index.search(com.algolia.search.model.search.Query(query))
            val results = response.hits.deserialize(InterestItem.serializer())
            val searchData = mutableListOf<SearchQuery>()
            for (result in results) {
                val searchQuery = SearchQuery(
                    result.objectID,
                    result.content,
                    System.currentTimeMillis(),
                    QUERY_TYPE_INTEREST
                )
                searchData.add(searchQuery)
            }

            setSearchData(searchData)

            insertInterests(*results.toTypedArray())
        } catch (e: Exception) {
            setCurrentError(e)
        }

    }

    private fun insertInterests(vararg interests: InterestItem) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertInterestItems(interests.toList())
        }

    val recentPostSearchList = MutableLiveData<List<PostMinimal2>>()
    val recentUserSearchList = MutableLiveData<List<UserMinimal2>>()

    @Suppress("UNCHECKED_CAST")
    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        val newQueries = mutableListOf<IndexedQuery>()

        val iq = IndexQuery(
            IndexName(POSTS), AlgoliaQuery(query)
        )

        val iq1 = IndexQuery(IndexName(USERS), AlgoliaQuery(query))

        newQueries.add(iq)
        newQueries.add(iq1)

        try {
            val response = client.search(newQueries)
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
                        val searchQuery = SearchQuery(
                            user.objectID,
                            user.name,
                            System.currentTimeMillis(),
                            QUERY_TYPE_USER
                        )

                        if (!(currentUser.blockedUsers.contains(user.objectID) || currentUser.blockedBy.contains(
                                user.objectID
                            ))
                        ) {
                            searchList.add(searchQuery)
                            usersList.add(user)
                        }
                    } else {
                        val post = hit.deserialize(PostMinimal2.serializer())
                        val searchQuery = SearchQuery(
                            post.objectID,
                            post.name,
                            System.currentTimeMillis(),
                            QUERY_TYPE_POST
                        )

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

    fun insertUsers(vararg users: User) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertUsers(users)
    }

    fun insertUsers(users: List<User>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertUsers(users)
    }

    // saving the user in both memory and cache
    fun saveUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
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

    /*fun setCurrentPostImages(images: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.mediaList = images
            setCurrentPost(existingPost)
        }
    }*/

    /*fun addToExistingPostImages(images: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val existingImages = existingPost.mediaList.toMutableList()
            existingImages.addAll(images)
            existingPost.mediaList = existingImages
            setCurrentPost(existingPost)
        }
    }*/

    /*fun addTagsToCurrentPost(tags: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val newList = existingPost.tags.toMutableList()
            newList.addAll(tags)
            existingPost.tags = newList.distinct()
            setCurrentPost(existingPost)
        }
    }*/

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

    fun insertCurrentUser(localUser: User) = viewModelScope.launch(Dispatchers.IO) {
        localUser.isCurrentUser = true
        insertUser(localUser)
    }

    fun insertUser(localUser: User) = viewModelScope.launch(Dispatchers.IO) {
        userCache[localUser.id] = localUser
        repo.insertUser(localUser)
    }

    /*fun deletePostImageAtPosition(pos: Int) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            val existingImages = existingPost.mediaList.toMutableList()
            existingImages.removeAt(pos)
            existingPost.mediaList = existingImages
            setCurrentPost(existingPost)
        }
    }*/

    fun createPost(thumbnailUrl: String, onComplete: (task: Task<Void>) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val post = currentPost.value!!
            FireUtility.createPost(
                post,
                createPostMediaList.value!!.sortedBy { it.type },
                thumbnailUrl
            ) {
                if (it.isSuccessful) {
                    post.isMadeByMe = true
                    insertNewPost(post)
                } else {
                    setCurrentError(it.exception)
                }
                onPostCreate(post)
                onComplete(it)
            }
        }

    fun setCurrentImage(image: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        _currentImage.postValue(image)
    }

    /*fun uploadImage(locationId: String, image: Uri, onComplete: (downloadUri: Uri?) -> Unit) {
        FireUtility.uploadImage(locationId, image, onComplete)
    }*/

    /*fun updateUser(
        updatedUser: User,
        changes: Map<String, Any?>,
        onComplete: (task: Task<Void>) -> Unit
    ) {
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
    }*/

    /*private fun updateLocalMessages(updatedUser: User) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateLocalMessages(updatedUser)
    }*/

    /*private fun updateLocalPosts(updatedUser: User, posts: List<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateLocalPosts(updatedUser, posts)
        }*/

    /*fun checkIfUsernameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.checkIfUserNameTaken(username, onComplete)
    }*/

    fun signOut(onComplete: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        repo.clearDatabases(onComplete)
    }

    @ExperimentalPagingApi
    fun getPostsNearMe(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20)
        ) {
            repo.postDao.getPostsNearMe()
        }.flow.cachedIn(viewModelScope)
    }

    /*private val _newPostVideoError = MutableLiveData<String>()
    val newPostVideoError: LiveData<String> = _newPostVideoError

    fun setNewPostVideoError(errorMsg: String) {
        _newPostVideoError.postValue(errorMsg)
    }*/


    /*@OptIn(ExperimentalPagingApi::class)
    fun something(lastUpdatedTime: Long? = null): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = 20,
                pageSize = 10,
                prefetchDistance = 5,
                maxSize = 50
            ), null, PostRemoteMediator2(repo.postDao)
        ) {
            PostPagingSource(repo.postDao)
        }.flow.cachedIn(viewModelScope)
    }*/


    /*@OptIn(ExperimentalPagingApi::class)
    fun getFeedItems(query: Query, tag: String? = null): Flow<PagingData<Post2>> {
        return Pager(
            config = PagingConfig(10),
            remoteMediator = PostRemoteMediator2(repo.postDao)
        ) {
            PostPagingSource(repo.postDao)
        }.flow.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }.cachedIn(viewModelScope)
    }*/

    @ExperimentalPagingApi
    fun getFeedItems(query: Query, tag: String? = null): Flow<PagingData<Post>> {

        val feedSetting = feedOption.value!!
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return if (tag != null) {

            val formattedTag = "%$tag%"

            Pager(
                config = PagingConfig(pageSize = 15),
                remoteMediator = PostRemoteMediator(query, repo, true) {
                    !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                        .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                        it.contributors
                    ).isNotEmpty()))
                }
            ) {
                when (feedSetting.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        repo.postDao.getTagPostsByContributors(formattedTag)
                    }
                    FeedSort.LIKES -> {
                        repo.postDao.getTagPostsByLikes(formattedTag)
                    }
                    FeedSort.MOST_VIEWED -> {
                        repo.postDao.getTagPostsByViews(formattedTag)
                    }
                    FeedSort.MOST_RECENT -> {
                        repo.postDao.getTagPostsByTime(formattedTag)
                    }
                    FeedSort.LOCATION -> {
                        repo.postDao.getTagPostsByTime(formattedTag)
                    }
                }
            }.flow/*.map { pagingData ->
                pagingData.map { post ->
                    Post2.Collab(post)
                }
            }.map {
                it.insertSeparators { before, after ->
                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null
                    }

                    if (before == null) {
                        // we're at the beginning of the list
                        return@insertSeparators null
                    }
                    // check between 2 items
                    val random = Random().nextInt(3)
                    val random1 = Random().nextInt(3)

                    if (random == random1) {
                        Post2.Advertise(randomId())
                    } else {
                        // no separator
                        null
                    }
                }
            }*/.cachedIn(viewModelScope)
        } else {
            Pager(
                config = PagingConfig(pageSize = 15),
                remoteMediator = PostRemoteMediator(query, repo, true) {
                    !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                        .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                        it.contributors
                    ).isNotEmpty()))
                }
            ) {
                when (feedSetting.sort) {
                    FeedSort.CONTRIBUTORS -> {
                        repo.postDao.getPagedPostsByContributors()
                    }
                    FeedSort.LIKES -> {
                        repo.postDao.getPagedPostsByLikes()
                    }
                    FeedSort.MOST_VIEWED -> {
                        repo.postDao.getPagedPostsByViews()
                    }
                    FeedSort.MOST_RECENT -> {
                        repo.postDao.getPagedPostsByTime()
                    }
                    FeedSort.LOCATION -> {
                        repo.postDao.getPagedPostsByTime()
                    }
                }
            }.flow/*.map { pagingData ->
                pagingData.map { post ->
                    Post2.Collab(post)
                }
            }.map {
                it.insertSeparators { before, after ->
                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null
                    }

                    if (before == null) {
                        // we're at the beginning of the list
                        return@insertSeparators null
                    }
                    // check between 2 items
                    val random = Random().nextInt(3)
                    val random1 = Random().nextInt(3)

                    if (random == random1) {
                        Post2.Advertise(randomId())
                    } else {
                        // no separator
                        null
                    }
                }
            }*/.cachedIn(viewModelScope)
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

    /*fun getCurrentUserChatChannels(query: Query): Flow<PagingData<ChatChannel>> {
        TODO()
    }*/

    @ExperimentalPagingApi
    fun getCurrentUserPosts(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getCurrentUserPagedPosts()
        }.flow/*.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }*/.cachedIn(viewModelScope)
    }


    @ExperimentalPagingApi
    fun getPostsTest(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getPostMoreLikes()
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


    private val _currentSearchQueryString = MutableLiveData<String?>().apply { value = null }
    val currentSearchQueryString: LiveData<String?> = _currentSearchQueryString


    fun setCurrentSearchQueryString(s: String?) {
        _currentSearchQueryString.postValue(s)
    }

    @ExperimentalPagingApi
    fun getCollaborations(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val currentUserId = UserManager.currentUserId
        val blockedBy = UserManager.currentUser.blockedBy
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getPagedCollaborations("%${currentUserId}%", currentUserId)
        }.flow/*.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }*/.cachedIn(viewModelScope)
    }


    @ExperimentalPagingApi
    fun getOtherUserPosts(query: Query, otherUser: User): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getPagedOtherUserPosts(otherUser.id)
        }.flow/*.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }*/.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getOtherUserCollaborations(query: Query, otherUser: User): Flow<PagingData<Post2>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getOtherUserPagedCollaborations("%${otherUser.id}%", otherUser.id)
        }.flow.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }.cachedIn(viewModelScope)
    }

    /*fun getOtherUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        FireUtility.getDocument(ref, onComplete)
    }*/

    fun likeLocalUserById(userId: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.likeLocalUserById(userId)
    }

    fun dislikeLocalUserById(userId: String) = viewModelScope.launch(Dispatchers.IO) {
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

    private fun onCommentSend(comment: Comment) = viewModelScope.launch(Dispatchers.IO) {
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

    private fun insertComment(comment: Comment) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertComments(listOf(comment))
    }

    @ExperimentalPagingApi
    fun getPagedComments(commentChannelId: String, query: Query): Flow<PagingData<Comment>> {
        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = CommentRemoteMediator(query, repo) { comment ->
                !(currentUser.blockedUsers.contains(comment.senderId) || currentUser.blockedBy.contains(
                    comment.senderId
                ))
            }
        ) {
            repo.commentDao.getPagedComments(commentChannelId)
        }.flow
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return withContext(Dispatchers.IO) {
            repo.getLocalChannelContributors(chatChannel)
        }
    }

    fun updatePost(thumbnailUrl: String, onComplete: (Post, task: Task<Void>) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val currentPost = currentPost.value!!
            FireUtility.updatePost(
                currentPost,
                createPostMediaList.value!!,
                thumbnailUrl
            ) { newPost, t ->
                onPostUpdate(newPost)
                onComplete(newPost, t)
            }
        }

    private suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
        return repo.getLocalChatChannel(chatChannel)
    }

    /*fun getLimitedMediaMessages(
        chatChannelId: String,
        limit: Int,
        type: String = image,
        onComplete: (List<Message>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        onComplete(repo.getLimitedMediaMessages(chatChannelId, limit, type))
    }*/

    fun insertMessages(messages: List<Message>) = viewModelScope.launch(Dispatchers.IO) {
        chatRepository.insertMessages(messages)
    }

    fun insertChatChannels(chatChannels: List<ChatChannel>) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertChatChannels(chatChannels)
        }

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return repo.getDocumentMessages(chatChannelId)
    }

    fun getDocumentMessages(chatChannelId: String, limit: Int = 6): LiveData<List<Message>> {
        return chatRepository.messageDao.getDocumentMessages(chatChannelId, limit)
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getTagPosts(tag: String, query: Query): Flow<PagingData<Post2>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo, false) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getTagPosts("%$tag%")
        }.flow.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }.cachedIn(viewModelScope)
    }

    fun setCurrentPostLinks(links: List<String>) {
        val existingPost = currentPost.value
        if (existingPost != null) {
            existingPost.sources = links
            setCurrentPost(existingPost)
        }
    }

    fun deleteComment(comment: Comment) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteComment(comment)
        updateLocalPostAfterCommentDeletion(comment)
        if (comment.parentCommentChannelId != null) {
            updateRepliesCountOfParentCommentById(comment.parentId)
        }
    }

    fun updateComment(comment: Comment) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateComment(comment)
    }

    fun deleteUserById(userId: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteUserById(userId)
    }

    /*suspend fun getImageMessages(chatChannelId: String, limit: Int = 0): List<Message> {
        return repo.getImageMessages(chatChannelId, limit)
    }*/

    fun updateReadList(message: Message) = viewModelScope.launch(Dispatchers.IO) {
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

                    insertMessages(listOf(message))
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

    /*fun getChatChannel(channelId: String, onComplete: (ChatChannel?) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            onComplete(chatRepository.getChatChannel(channelId))
        }*/

    @OptIn(ExperimentalPagingApi::class)
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

    fun insertPosts(posts: List<Post>) = viewModelScope.launch(Dispatchers.IO) {
        for (post in posts) {
            postCache[post.id] = post
        }
        repo.insertPosts(posts.toTypedArray())
    }


    val testImage = MutableLiveData<Uri>().apply { value = null }

    fun insertPosts(vararg posts: Post) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertPosts(posts)
    }

    /* private val _reportUploadImages = MutableLiveData<List<Uri>>()
     val reportUploadImages: LiveData<List<Uri>> = _reportUploadImages

     fun setReportUploadImages(images: List<Uri>) {
         _reportUploadImages.postValue(images)
     }*/

    fun sendFeedback(feedback: Feedback, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.sendFeedback(feedback, onComplete)
    }

    fun updateNotification(notification: Notification) = viewModelScope.launch(Dispatchers.IO) {
        repo.notificationDao.insert(notification)
    }

    fun insertNotifications(notifications: List<Notification>) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertNotifications(notifications)
        }

    fun insertNotifications(vararg notifications: Notification) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertNotifications(notifications)
        }

    fun insertPostRequests(requests: List<PostRequest>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertPostRequests(requests)
    }

    fun insertPostRequests(vararg postRequest: PostRequest) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertPostRequests(postRequest)
        }

    fun insertSearchQuery(searchQuery: SearchQuery) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertSearchQuery(searchQuery)
    }

    fun setOtherUserAsAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.setOtherUserAsAdmin(chatChannelId, userId, onComplete)
    }

    fun removeOtherUserFromAdmin(
        chatChannelId: String,
        userId: String,
        onComplete: (task: Task<Void>) -> Unit
    ) {
        FireUtility.removeUserFromAdmin(chatChannelId, userId, onComplete)
    }

    private suspend fun getLocalPost(postId: String): Post? {
        return repo.getPost(postId)
    }

    fun deleteNotification(notification: Notification) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteNotification(notification)
    }

    fun deletePostRequest(postRequest: PostRequest) = viewModelScope.launch(Dispatchers.IO) {
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
    fun deletePostInvite(postInvite: PostInvite) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePostInvite(postInvite)
    }

    fun insertPostInvites(vararg newPostInvites: PostInvite) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertPostInvites(newPostInvites)
        }

    fun deleteNotificationById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteNotificationById(id)
    }

    fun deleteLocalPost(post: Post) = viewModelScope.launch(Dispatchers.IO) {
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

    /*fun deleteAdPosts() = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteAdPosts()
    }*/

    fun deleteLocalPostRequest(postRequest: PostRequest) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePostRequest(postRequest)
    }

    fun deleteLocalChatChannelById(chatChannelId: String) = viewModelScope.launch(Dispatchers.IO) {
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

    @OptIn(ExperimentalPagingApi::class)
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

    fun disableLocationBasedPosts() = viewModelScope.launch(Dispatchers.IO) {
        repo.disableLocationBasedPosts()
    }

    /*private fun getPostRequest(postId: String, onComplete: (PostRequest?) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            onComplete(repo.getPostRequest(postId))
        }*/

    /* Chat related functions */

    /**
     * List of chat channels for ForwardFragment
     * */
    /* private val _forwardList = MutableLiveData<List<ChatChannel>>()
     val forwardList: LiveData<List<ChatChannel>> = _forwardList
 */
    /**
     * Flag for holding the current state of the chat, whether messages are being selected or not
     * */
//    var isSelectModeOn = false

    /**
     * Placeholder for the current message that has been selected to be replied to
     * */
    /* private val _replyMessage = MutableLiveData<Message>().apply { value = null }
     val replyMessage: LiveData<Message> = _replyMessage*/

    /**
     * List of images to be uploaded in the current chat
     * */
    /*private val _chatImagesUpload = MutableLiveData<List<Uri>>()*/
    /*val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload*/

    /**
     * List of documents to be uploaded in the current chat
     * */
    /*  private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
      val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload*/


    /*private val _chatVideosUpload = MutableLiveData<List<Uri>>()
    val chatVideosUpload: LiveData<List<Uri>> = _chatVideosUpload*/


    /**
     * To get messages for ChatFragment in paged order
     *
     * @param chatChannelId Channel to get messages from local database
     * @param query FireBase query to get messages on boundary callbacks
     *
     * @return A flow of messages in paged order by position in local database by recent order
     * */
    @OptIn(ExperimentalPagingApi::class)
    fun getPagedMessages(chatChannelId: String, query: Query): Flow<PagingData<Message2>> {
        val currentUser = UserManager.currentUser
        return Pager(config =
        PagingConfig(
            pageSize = 30,
            enablePlaceholders = false,
            maxSize = 60,
            prefetchDistance = 10,
            initialLoadSize = 40
        ),
            remoteMediator = MessageRemoteMediator(
                query,
                chatRepository
            ) {
                !(currentUser.blockedUsers.contains(it.senderId) || currentUser.blockedBy.contains(
                    it.senderId
                ))
            }
        ) {
            chatRepository.messageDao.getChannelPagedMessages(chatChannelId)
        }.flow.map { pagingData ->
            pagingData.map { message ->
                Message2.MessageItem(message)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                if (!isSameDay(Date(before.message.createdAt), Date(after.message.createdAt))) {
                    Message2.DateSeparator(
                        Date(before.message.createdAt),
                        SimpleDateFormat(
                            "dd MMMM, yyyy",
                            Locale.UK
                        ).format(before.message.createdAt)
                    )
                } else {
                    // no separator
                    null
                }
            }
        }.cachedIn(viewModelScope)
    }


    /*fun addChannelToForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null) {
            val newList = oldList.toMutableList()
            newList.add(chatChannel)
            _forwardList.postValue(newList)
        } else {
            _forwardList.postValue(listOf(chatChannel))
        }
    }*/


    /*fun removeChannelFromForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null && oldList.isNotEmpty()) {
            val newList = oldList.toMutableList()
            newList.remove(chatChannel)
            _forwardList.postValue(newList)
        }
    }*/


    /**
     * To clear all data present to list accessed by ForwardFragment
     * */
    /*fun clearForwardList() {
        _forwardList.postValue(emptyList())
    }*/


    /**
     * To upload message to firestore
     *
     * @param chatChannelId The chat channel id to where this post belongs
     * @param content Content of the text message
     * @param replyTo An optional message id attached to the current message to be send which is connected respectively
     * @param replyMessage An optional message attached to the current message in minified form
     * */
    fun sendTextMessage(
        chatChannelId: String,
        content: String,
        replyTo: String? = null,
        replyMessage: MessageMinimal? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        when (val result =
            FireUtility.sendTextMessage(chatChannelId, content, replyTo, replyMessage)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                chatRepository.insertMessage(result.data, true)
            }
        }
    }

    /**
     * To send multiple messages together, may include message of all type [image, document, text]
     *
     * @param listOfMessages The messages to be sent
     *
     * */
    fun sendMessages(
        listOfMessages: List<Message>,
        onComplete: ((taskResult: Result<List<Message>>) -> Unit)? = null
    ) = viewModelScope.launch(Dispatchers.IO) {

        val result = FireUtility.sendMessages(listOfMessages)

        if (onComplete != null) {
            onComplete(result)
        }

        when (result) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                val messages = result.data

                chatRepository.insertMessages(messages)
            }
        }
    }

    /*fun setChatUploadImages(images: List<Uri>) {
        _chatImagesUpload.postValue(images)
    }*/

    /*fun setChatUploadDocuments(documents: List<Uri>) {
        _chatDocumentsUpload.postValue(documents)
    }*/

    /* fun setChatUploadVideos(videos: List<Uri>) {
         _chatVideosUpload.postValue(videos)
     }*/


    /*private val _chatUploadMediaItems = MutableLiveData<List<MediaItem>>()
    val chatUploadMediaItems: LiveData<List<MediaItem>> = _chatUploadMediaItems*/

    /* fun setChatUploadMedia(mediaItems: List<MediaItem>) {
         _chatUploadMediaItems.postValue(mediaItems)
     }*/

    private val _preUploadMediaItems = MutableLiveData<List<MediaItem>>()
    val preUploadMediaItems: LiveData<List<MediaItem>> = _preUploadMediaItems

    fun setPreUploadMediaItems(mediaItems: List<MediaItem>) {
        _preUploadMediaItems.postValue(mediaItems)
    }

    private val _selectMediaItems = MutableLiveData<List<MediaItem>>()
    /*val selectMediaItems: LiveData<List<MediaItem>> = _selectMediaItems*/

    fun setSelectMediaItems(mediaItems: List<MediaItem>) {
        _selectMediaItems.postValue(mediaItems)
    }

    /*fun getForwardChannels(userId: String): LiveData<List<ChatChannel>> {
        return chatRepository.getForwardChannels("%$userId%")
    }*/

    /*fun sendForwardsToChatChannels(
        messages: List<Message>,
        channels: List<ChatChannel>,
        onComplete: (result: Result<List<Message>>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
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
    }*/


    /* fun selectedMessages(chatChannelId: String): LiveData<List<Message>> {
         return chatRepository.selectedMessages(chatChannelId)
     }*/


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
     * To update a message in local database
     *
     * @param message The message to be updated
     * */
    fun updateMessage(message: Message) = viewModelScope.launch(Dispatchers.IO) {
        chatRepository.messageDao.update(message)
    }

    /*fun getContributors(chatChannelId: String, limit: Int = 6): LiveData<List<User>> {
        return userRepository.getContributors(chatChannelId, limit)
    }*/

    fun setListenerForEmailVerification() = viewModelScope.launch(Dispatchers.IO) {
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

    fun clearAllChannels() = viewModelScope.launch(Dispatchers.IO) {
        chatRepository.clearChatChannels()
    }

    // We cannot set snapshot listener in every post that's why,
    // when a post request is accepted, to reflect the changes locally,
    // we need to check if something has changed in user document and respectively
    // make the changes locally.
    /*private fun checkAndUpdateLocalPosts(currentUser: User) = viewModelScope.launch(Dispatchers.IO) {
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
    }*/

    fun insertUserToCache(creator: User) {
        userCache[creator.id] = creator
    }

    fun getCachedPost(id: String): Post? {
        return postCache[id]
    }

    fun insertPostToCache(post: Post) {
        postCache[post.id] = post
    }

    fun getUser(senderId: String, function: (User?) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            function(repo.getUser(senderId))
        }

    fun getPost(postId: String, function: (Post?) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        function(repo.getPost(postId))
    }

    fun deletePostById(postId: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePostById(postId)
    }

    private val _googleSignInError = MutableLiveData<Int?>()
    val googleSignInError: LiveData<Int?> = _googleSignInError

    /*fun setGoogleSignInError(code: Int) {
        if (code == -1) {
            _googleSignInError.postValue(null)
        } else {
            _googleSignInError.postValue(code)
        }
    }*/

    @OptIn(ExperimentalPagingApi::class)
    fun getLikes(query: Query): Flow<PagingData<UserMinimal>> {

        Log.d(TAG, "getLikes: ")

        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = LikedByRemoteMediator(query, repo) {
                !(currentUser.blockedUsers.contains(it.userId) || currentUser.blockedBy.contains(it.userId))
            }
        ) {
            repo.userMinimalDao.getPagedUsers()
        }.flow.cachedIn(viewModelScope)
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getReferenceItems(query: Query): Flow<PagingData<ReferenceItem>> {
        val currentUser = UserManager.currentUser
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ReferenceItemRemoteMediator(query, repo) {
                !(currentUser.blockedUsers.contains(it.id) || currentUser.blockedBy.contains(it.id))
            }
        ) {
            repo.referenceItemDao.getReferenceItems()
        }.flow.cachedIn(viewModelScope)
    }

    fun deleteReferenceItem(itemId: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteReferenceItem(itemId)
    }

    private fun updateRepliesCountOfParentCommentById(parentCommentId: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val parentComment = repo.getComment(parentCommentId)
            if (parentComment != null) {
                Log.e(
                    TAG,
                    "updateRepliesCountOfParentCommentById: found parent comment with id $parentCommentId"
                )
                parentComment.repliesCount -= 1
                insertComment(parentComment)
            } else {
                Log.e(
                    TAG,
                    "updateRepliesCountOfParentCommentById: parent comment was null with id $parentCommentId"
                )
            }
        }

    private fun updateLocalPostAfterCommentDeletion(comment: Comment) =
        viewModelScope.launch(Dispatchers.IO) {
            val totalCommentsDeletedFromThePost = 1 + comment.repliesCount
            getPost(comment.postId) {
                if (it != null) {
                    it.commentsCount -= totalCommentsDeletedFromThePost
                    insertPosts(it)
                }
            }
        }

    fun insertPost(post: Post) = viewModelScope.launch(Dispatchers.IO) {
        insertPostToCache(post)
        repo.insertPosts(arrayOf(post))
    }

    private val _isNewPostCreated = MutableLiveData<String?>()
    val isNewPostCreated: LiveData<String?> = _isNewPostCreated

    fun setCreatedNewPost(postId: String?) {
        _isNewPostCreated.postValue(postId)
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getPagedInterestItems(query: Query): Flow<PagingData<InterestItem>> {
        return Pager(
            config = PagingConfig(pageSize = 100),
            remoteMediator = InterestItemRemoteMediator(repo, query)
        ) {
            repo.interestItemDao.getPagedInterestItems()
        }.flow.cachedIn(viewModelScope)
    }

    fun uncheckInterestItem(interestItem: InterestItem) = viewModelScope.launch(Dispatchers.IO) {
        interestItem.isChecked = false
        repo.insertInterestItems(listOf(interestItem))
    }

    fun checkInterestItem(interestItem: InterestItem) = viewModelScope.launch(Dispatchers.IO) {
        interestItem.isChecked = true
        repo.insertInterestItems(listOf(interestItem))
    }

    fun insertInterestItem(interestItem: InterestItem) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertInterestItem(interestItem)
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

    fun updateChatChannel(chatChannel: ChatChannel) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateChatChannel(chatChannel)
    }

    fun getUnreadChatChannels(): LiveData<List<ChatChannel>> {
        return repo.getUnreadChatChannels()
    }

    fun deleteCommentsByUserId(id: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteCommentsByUserId(id)
    }

    fun deletePostsByUserId(id: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePostsByUserId(id)
    }

    fun deletePreviousSearchByUserId(id: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePreviousSearchByUserId(id)
    }


    /* Chat related functions end */


    /* User related functions */
    var userVerificationResult: LiveData<Result<Boolean>> = userRepository.userVerificationResult


    /* User related functions end */

    suspend fun getPostRequestByNotificationId(id: String): PostRequest? {
        return withContext(Dispatchers.IO) {
            repo.getPostRequestByNotificationId(id)
        }
    }

    /*@OptIn(ExperimentalPagingApi::class)
    fun getRankedPosts(): Flow<PagingData<Post>> {

        val minusOne: Long = -1L

        val query = Firebase.firestore.collection("posts")
            .whereNotEqualTo("rank", minusOne)

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo) {
                true
            }
        ) {
            repo.postDao.getRankedPosts()
        }.flow.cachedIn(viewModelScope)
    }*/

    private val _createPostMediaList = MutableLiveData<List<MediaItem>>()
    val createPostMediaList: LiveData<List<MediaItem>> = _createPostMediaList

    fun setCreatePostMediaList(mediaList: List<MediaItem>) {
        _createPostMediaList.postValue(mediaList)
    }

    fun deleteMediaItemAtPosition(pos: Int) {
        val existingList = createPostMediaList.value
        if (existingList != null) {
            Log.d(TAG, "deleteMediaItemAtPosition: Removed item at pos $pos")
            val existingMutableList = existingList.toMutableList()
            existingMutableList.removeAt(pos)
            _createPostMediaList.postValue(existingMutableList)
        }
    }

    fun clearCreatePostMediaItems() = viewModelScope.launch(Dispatchers.IO) {
        _createPostMediaList.postValue(emptyList())
    }

    fun getMultiMediaMessages(chatChannelId: String, limit: Int = 6): LiveData<List<Message>> {
        return chatRepository.messageDao.getMultiMediaMessages(chatChannelId, limit)
    }

    suspend fun getMultimediaMessagesSync(
        chatChannelId: String,
        after: Long? = null
    ): List<Message> {
        return if (after != null) {
            chatRepository.messageDao.getMultiMediaMessagesSyncAfter(chatChannelId, after)
        } else {
            chatRepository.messageDao.getMultiMediaMessagesSync(chatChannelId)
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getRankedCategoryItems(category: String, query: Query): Flow<PagingData<Post2>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(query, repo, igt = true) { true }
        ) {
            repo.postDao.getRankedCategoryPosts(category)
        }.flow.map { pagingData ->
            pagingData.map { post ->
                Post2.Collab(post)
            }
        }.map {
            it.insertSeparators { before, after ->
                if (after == null) {
                    // we're at the end of the list
                    return@insertSeparators null
                }

                if (before == null) {
                    // we're at the beginning of the list
                    return@insertSeparators null
                }
                // check between 2 items
                val random = Random().nextInt(3)
                val random1 = Random().nextInt(3)

                if (random == random1) {
                    Post2.Advertise(randomId())
                } else {
                    // no separator
                    null
                }
            }
        }.cachedIn(viewModelScope)
    }

    fun setCompetitions(competitions: List<Competition>) {
        _competitions.postValue(competitions)
    }

    fun setReadPermission(b: Boolean) {
        _readPermission.postValue(b)
    }

    fun setLocationPermission(b: Boolean) {
        _locationPermission.postValue(b)
    }

    /*fun uploadPostThumbnail(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val postThumbnailUrls = FireUtility.uploadItems2(
            listOf(
                UploadItem(
                    uri,
                    "images/posts/${currentPost.value!!.id}",
                    randomId()
                )
            )
        )
        if (postThumbnailUrls.isNotEmpty()) {
            val a = postThumbnailUrls.first()

            Log.d(TAG, "uploadPostThumbnail: $a")

            val currentPost = currentPost.value!!
            currentPost.thumbnail = a
            _currentPost.postValue(currentPost)
        }
    }*/

    fun getMessageByMediaItemName(name: String, onComplete: (message: Message?) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            onComplete(chatRepository.messageDao.getMessageByMediaItemName(name))
        }


    private val _cameraPhotoUri = MutableLiveData<Uri?>()
    val cameraPhotoUri: LiveData<Uri?> = _cameraPhotoUri

    fun setCameraImage(cameraPhotoUri: Uri?) {
        _cameraPhotoUri.postValue(cameraPhotoUri)
    }

    fun saveMediaToFile(message: Message, destination: File, onComplete: (Message) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val name = message.content + message.metadata!!.ext
            FireUtility.downloadMessageMedia(destination, name, message) {
                if (it.isSuccessful) {
                    message.isDownloaded = true
                    updateMessage(message)
                } else {
                    destination.delete()
                }
                onComplete(message)
            }
        }

    fun downloadMediaThumbnail(
        message: Message,
        destination: File,
        onComplete: (Result<Message>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (message.type == video) {
            val ref =
                Firebase.storage.reference.child("videos/messages/${message.messageId}/thumb_${message.content}.jpg")

            ref.getFile(destination)
                .addOnSuccessListener {
                    message.metadata?.thumbnail = Uri.fromFile(destination).toString()
                    updateMessage(message)
                    onComplete(Result.Success(message))
                }.addOnFailureListener {
                    onComplete(Result.Error(it))
                }
        } else {
            /*try {
                val url = URL(message.metadata!!.url)
                val connection = url.openConnection() as HttpURLConnection?

                connection?.doInput = true
                connection?.connect()

                if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    storeBitmapToFile(destination, bitmap, 20)
                    inputStream?.close()
                    message.metadata!!.thumbnail = Uri.fromFile(destination).toString()
                    updateMessage(message)
                    onComplete(Result.Success(message))
                }
            } catch (e: Exception) {
                onComplete(Result.Error(e))
            }*/
        }
    }

    suspend fun getInterestItem(tag: String): InterestItem? {
        return repo.getInterestItem(tag)
    }

    fun onUserUpdate(updatedUser: User) = viewModelScope.launch(Dispatchers.IO) {
        insertCurrentUser(updatedUser)
//        checkAndUpdateLocalPosts(updatedUser)
        repo.updateLocalPosts(updatedUser)
        repo.updateLocalMessages(updatedUser)
    }

    private val _updateUserForm =
        MutableLiveData<UpdateUserFormState>().apply { UpdateUserFormState() }
    val updateUserForm: LiveData<UpdateUserFormState> = _updateUserForm

    fun updateUserFormChanged(name: String, username: String, tag: String, about: String) =
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)

            if (name.length !in 6..50) {
                if (name.length < 6) {
                    _updateUserForm.postValue(UpdateUserFormState(nameError = R.string.name_too_short_error))
                } else if (name.length > 50) {
                    _updateUserForm.postValue(UpdateUserFormState(nameError = R.string.name_too_long_error))
                }
            } else if (username.length !in 6..16) {
                if (username.length < 6) {
                    _updateUserForm.postValue(UpdateUserFormState(usernameError = R.string.username_too_short))
                } else if (username.length > 16) {
                    _updateUserForm.postValue(UpdateUserFormState(usernameError = R.string.username_too_long))
                }
            }

            if (username != UserManager.currentUser.username) {
                when (val result = FireUtility.usernameExist(username)) {
                    is Result.Error -> {
                        _updateUserForm.postValue(UpdateUserFormState(usernameError = R.string.username_search_error))
                    }
                    is Result.Success -> {
                        if (result.data) {
                            _updateUserForm.postValue(UpdateUserFormState(usernameError = R.string.username_exists_error))
                        }
                    }
                }
            }

            if (tag.isNotBlank() && tag.length > 20) {
                _updateUserForm.postValue(UpdateUserFormState(tagError = R.string.tag_too_long))
            } else if (about.isNotBlank() && tag.length > 200) {
                _updateUserForm.postValue(UpdateUserFormState(aboutError = R.string.about_too_long))
            } else {
                _updateUserForm.postValue(UpdateUserFormState(isValid = true))
            }

        }

    private val _loginForm = MutableLiveData<LoginFormState>().apply { LoginFormState() }
    val loginFormState: LiveData<LoginFormState> = _loginForm

    fun loginDataChanged(email: String, password: String) = viewModelScope.launch(Dispatchers.IO) {

        delay(1500)

        if (!email.isValidEmail()) {
            _loginForm.postValue(LoginFormState(emailError = R.string.invalid_email))
        } else if (!password.isValidPassword()) {
            _loginForm.postValue(LoginFormState(passwordError = R.string.invalid_password))
        } else {
            _loginForm.postValue(LoginFormState(isDataValid = true))
        }
    }

    private val _registerForm = MutableLiveData<RegisterFormState>().apply { RegisterFormState() }
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    fun registerDataChanged(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ) = viewModelScope.launch(Dispatchers.IO) {

        delay(1500)

        if (name.length !in 5..50) {
            if (name.length < 5) {
                _registerForm.postValue(RegisterFormState(nameError = R.string.name_too_short_error))
            }

            if (name.length > 100) {
                _registerForm.postValue(RegisterFormState(nameError = R.string.name_too_long_error))
            }
        } else if (!email.isValidEmail()) {
            _registerForm.postValue(RegisterFormState(emailError = R.string.invalid_email))
        } else if (!password.isValidPassword()) {
            _registerForm.postValue(RegisterFormState(passwordError = R.string.invalid_password))
        } else if (confirmPassword != password) {
            _registerForm.postValue(RegisterFormState(confirmPasswordError = R.string.invalid_confirm_password))
        } else {
            _registerForm.postValue(RegisterFormState(isDataValid = true))
        }
    }

    fun updateCreatePostList(mediaItem: MediaItem, currentCropPos: Int) {
        val mediaList = createPostMediaList.value!!.toMutableList()
        mediaList[currentCropPos] = mediaItem
        setCreatePostMediaList(mediaList)
    }


    private val _updatedOldPost = MutableLiveData<String?>()
    val updatedOldPost: LiveData<String?> = _updatedOldPost

    fun setUpdatedOldPost(id: String?) = viewModelScope.launch(Dispatchers.IO) {
        _updatedOldPost.postValue(id)
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getSavedPosts(query: Query): Flow<PagingData<Post>> {
        val blockedUsers = UserManager.currentUser.blockedUsers
        val blockedBy = UserManager.currentUser.blockedBy

        return Pager(
            config = PagingConfig(pageSize = 15),
            remoteMediator = PostRemoteMediator(query, repo, areSavedPosts = true) {
                !((blockedUsers.contains(it.creator.userId) || blockedUsers.intersect(it.contributors)
                    .isNotEmpty()) || (blockedBy.contains(it.creator.userId) || blockedBy.intersect(
                    it.contributors
                ).isNotEmpty()))
            }
        ) {
            repo.postDao.getPagedSavedPosts()
        }.flow.cachedIn(viewModelScope)
    }

    fun getChannelContributors(chatChannelId: String): LiveData<List<User>> {
        return repo.getChannelContributors(chatChannelId)
    }

    fun getPost(id: String) : LiveData<Post> {
        return repo.postDao.getReactivePost(id)
    }

    fun getSimilarPosts(postId: String, randomTag: String): LiveData<List<Post>> {
        return repo.postDao.getSimilarPostsReactive(postId, "%$randomTag%")
    }

    fun onPostUpdate(newPost: Post) = viewModelScope.launch (Dispatchers.IO) {
        repo.postDao.deletePostById(newPost.id)
        repo.postDao.insert(newPost)
        _currentPost.postValue(null)
        _updatedOldPost.postValue(newPost.id)
        _createPostMediaList.postValue(emptyList())
    }

    fun onPostCreate(post: Post) = viewModelScope.launch (Dispatchers.IO) {
        setCurrentPost(null)
        clearCreatePostMediaItems()
        setCreatedNewPost(post.id)
    }

    fun archivedChatChannels(): LiveData<List<ChatChannel>> {
        return repo.chatChannelDao.archivedChannels()
    }


    /*private val _galleryMediaItems = MutableLiveData<List<MediaItem>>()
    val galleryMediaItems: LiveData<List<MediaItem>> = _galleryMediaItems

    fun addItemsToGalleryMediaItems(items: List<MediaItem>) {
        val existingList = mutableListOf<MediaItem>()
        if (_galleryMediaItems.value != null) {
            existingList.addAll(_galleryMediaItems.value!!)
            existingList.addAll(items)
            _galleryMediaItems.postValue(existingList)
        } else {
            _galleryMediaItems.postValue(items)
        }
    }

    fun prefetchInitialMediaItems(cr: ContentResolver, type: ItemSelectType) = viewModelScope.launch (Dispatchers.IO) {
        loadItemsFromExternal(cr, type)
    }

    var hasReachedEnd = false

    fun loadItemsFromExternal(
        contentResolver: ContentResolver,
        type: ItemSelectType,
        limit: Int = 50,
        lastItemSortAnchor: Long = System.currentTimeMillis()
    ) {
        val mediaItems = mutableListOf<MediaItem>()
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Files.getContentUri("external")
            }

        var selection = when (type) {
            ItemSelectType.GALLERY_ONLY_IMG -> {
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            }
            ItemSelectType.GALLERY_ONLY_VID -> {
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"
            }
            ItemSelectType.GALLERY -> {
                "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"     //Selection criteria
            }
            ItemSelectType.DOCUMENT -> {
                null
            }
        }

        selection = if (selection.isNullOrBlank()) {
            MediaStore.Files.FileColumns.DATE_MODIFIED + "<" + lastItemSortAnchor
        } else {
            selection + " AND " + MediaStore.Files.FileColumns.DATE_MODIFIED + "<" + lastItemSortAnchor
        }

        val selectionArgs = arrayOf<String>()

        val sortOrder: String = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE
        )

        val cursor =
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)

        if (cursor != null) {
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val modifiedCol =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            var count = 0

            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol)

                val (t, fileUri) = when (type) {
                    ItemSelectType.GALLERY_ONLY_IMG -> {
                        image to ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                    ItemSelectType.GALLERY_ONLY_VID -> {
                        video to ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                    ItemSelectType.GALLERY -> {
                        if (mimeType.contains(video)) {
                            video to ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                        } else {
                            image to ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                        }
                    }
                    ItemSelectType.DOCUMENT -> {
                        document to ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri("external"),
                            id
                        )
                    }
                }

                val file = File("")
                file.extension

                val name = cursor.getString(nameCol)
                val dateModified = cursor.getLong(modifiedCol)
                val size = cursor.getLong(sizeCol)
                val createdAt = cursor.getLong(dateAddedCol)

                val ext = if (name.split('.').size > 1) {
                    "." + name.substringAfterLast('.', "")
                } else {
                    if (mimeType.contains("video")) {
                        ".mp4"
                    } else {
                        ".jpg"
                    }
                }

                val mediaItem = MediaItem(
                    fileUri.toString(),
                    name, t, mimeType ?: "", size, ext, "", null, createdAt, dateModified
                )
                mediaItems.add(mediaItem)
                count++
            }
            cursor.close()
        }

        if (mediaItems.size < 50) {
            hasReachedEnd = true
        }

        addItemsToGalleryMediaItems(mediaItems)

    }*/


    companion object {
        private const val TAG = "MainViewModel"
    }

}
