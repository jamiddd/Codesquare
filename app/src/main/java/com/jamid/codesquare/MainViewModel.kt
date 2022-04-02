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
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.*
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
        val db = CodesquareDatabase.getInstance(application.applicationContext)
        repo = MainRepository.getInstance(db)
        chatRepository = ChatRepository(db, viewModelScope, application.applicationContext)
        userRepository = UserRepository(db, viewModelScope)

    }

    private val _currentError = MutableLiveData<Exception?>()

    val currentError: LiveData<Exception?> = _currentError
    val currentUser: LiveData<User> = repo.currentUser

    var currentFocusedTag: String? = null

    /**
     * A placeholder project to be used while creating new project.
    * */
    private val _currentProject = MutableLiveData<Project>().apply { value = null }
    val currentProject: LiveData<Project> = _currentProject

    private val _currentImage = MutableLiveData<Uri?>()
    val currentImage: LiveData<Uri?> = _currentImage

    private val userCache = mutableMapOf<String, User>()
    private val chatChannelCache = mutableMapOf<String, ChatChannel>()
    private val commentChannelCache = mutableMapOf<String, CommentChannel>()
    private val commentCache = mutableMapOf<String, Comment>()
    private val projectCache = mutableMapOf<String, Project>()

    var currentChatChannel: String? = null

    val multipleImagesContainer = MutableLiveData<List<Uri>>().apply { value = emptyList() }

    val multipleDocumentsContainer = MutableLiveData<List<Uri>>().apply { value = emptyList() }

    private val _currentFocusedUser = MutableLiveData<User>().apply { value = null }
    val currentFocusedUser: LiveData<User> = _currentFocusedUser

    private val _currentFocusedComment = MutableLiveData<Comment>().apply { value = null }
    val currentFocusedComment: LiveData<Comment> = _currentFocusedComment

    private val _currentFocusedChatChannel = MutableLiveData<ChatChannel>().apply { value = null }
    val currentFocusedChatChannel: LiveData<ChatChannel> = _currentFocusedChatChannel

    private val _currentFocusedProject = MutableLiveData<Project>().apply { value = null }
    val currentFocusedProject: LiveData<Project> = _currentFocusedProject

    fun setCurrentFocusedChatChannel(chatChannel: ChatChannel?) {
        _currentFocusedChatChannel.postValue(chatChannel)
    }

    fun setCurrentFocusedUser(user: User?) {
        _currentFocusedUser.postValue(user)
    }

    fun setCurrentFocusedProject(project: Project?) {
        _currentFocusedProject.postValue(project)
    }


    /**
     * Flag to check whether sound network is available or not
     * */
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable


    /**
     * List of all the products fetched from play store
     * */
    private val _subscriptionDetails = MutableLiveData<List<SkuDetails>>().apply { value = emptyList() }
    val subscriptionDetails: LiveData<List<SkuDetails>> = _subscriptionDetails

    private val _currentlySelectedSku = MutableLiveData<SkuDetails?>()

    fun setCurrentlySelectedSku(skuDetails: SkuDetails?) {
        _currentlySelectedSku.postValue(skuDetails)
    }

    val chatScrollPositions = mutableMapOf<String, Int>()

    fun setSubscriptionDetailsList(detailsList: List<SkuDetails> = emptyList()) {
        _subscriptionDetails.postValue(detailsList)
    }

    /**
     * List of all the available subscriptions for the user
     * */
    private val _subscriptions = MutableLiveData<List<Subscription>>().apply { value = emptyList() }
    val subscriptions: LiveData<List<Subscription>> = _subscriptions

    fun setSubscriptions(list: List<Subscription> = emptyList()) {
        _subscriptions.postValue(list)
    }


    val chatChannels = repo.chatChannels
    val errors = repo.errors

    var currentUserBitmap: Bitmap? = null

    val allUnreadNotifications = repo.allUnreadNotifications

    val allPreviousQueries = repo.allPreviousQueries

    private val _recentSearchList = MutableLiveData<List<SearchQuery>>().apply { value = null }
    val recentSearchList: LiveData<List<SearchQuery>> = _recentSearchList

    private val client = ClientSearch(ApplicationID(BuildConfig.ALGOLIA_ID), APIKey(BuildConfig.ALGOLIA_SECRET))

    private val _networkError = MutableLiveData<Exception>().apply { value = null }
   /* val networkError: LiveData<Exception> = _networkError*/



    fun setSearchData(searchList: List<SearchQuery>?) {
        _recentSearchList.postValue(searchList)
    }

    private fun setNetworkError(exception: Exception?) {
        _networkError.postValue(exception)
    }

    fun searchInterests(query: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val index = client.initIndex(IndexName("interests"))
            val response = index.search(com.algolia.search.model.search.Query(query))
            val results = response.hits.deserialize(Interest.serializer())
            val searchData = mutableListOf<SearchQuery>()
            for (result in results) {
                val searchQuery = SearchQuery(result.id, result.interest, System.currentTimeMillis(), QUERY_TYPE_INTEREST)
                searchData.add(searchQuery)
            }

            setSearchData(searchData)

            insertInterests(*results.toTypedArray())
        } catch (e: Exception) {
            setCurrentError(e)
        }

    }

    private fun insertInterests(vararg interests: Interest) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertInterests(interests)
    }

    @Suppress("UNCHECKED_CAST")
    fun search(query: String) = viewModelScope.launch (Dispatchers.IO) {
        val newQueries = mutableListOf<IndexedQuery>()
        val iq = IndexQuery(
            IndexName("projects"), com.algolia.search.model.search.Query(query)
        )

        val iq1 = IndexQuery(IndexName("users"), com.algolia.search.model.search.Query(query))

        newQueries.add(iq)
        newQueries.add(iq1)

        try {
            val response: ResponseMultiSearch = client.search(newQueries)

            val list = response.results as List<ResultMultiSearch<ResponseSearch>>

            val usersList = mutableListOf<User>()
            val projectsList = mutableListOf<Project>()

            val searchList = mutableListOf<SearchQuery>()
            for (result in list) {
                for (hit in result.response.hits) {
                    val type = hit.json["type"].toString()
                    if (type == "\"user\"") {
                        val user = hit.deserialize(User.serializer())
                        usersList.add(user)
                        val searchQuery = SearchQuery(user.id, user.name, System.currentTimeMillis(), QUERY_TYPE_USER)
                        searchList.add(searchQuery)
                    } else {
                        val project = hit.deserialize(Project.serializer())
                        projectsList.add(project)
                        val searchQuery = SearchQuery(project.id, project.name, System.currentTimeMillis(), QUERY_TYPE_PROJECT)
                        searchList.add(searchQuery)
                    }
                }
            }

            insertProjects(*projectsList.toTypedArray())
            insertUsers(*usersList.toTypedArray())

            setSearchData(searchList)
        } catch (e: Exception) {
            setNetworkError(e)
        }
    }

    fun insertUsers(vararg users: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }

    fun insertUsers(users: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }

    // make sure the user in this comment is not null or empty
    val replyToContent = MutableLiveData<Comment>()

    fun setCurrentError(exception: Exception?) {
        _currentError.postValue(exception)
    }

    fun setCurrentProject(project: Project?) {
        _currentProject.postValue(project)
    }

    fun setCurrentProjectTitle(title: String) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.name = title
            setCurrentProject(existingProject)
        }
    }

    fun setCurrentProjectContent(content: String) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.content = content
            setCurrentProject(existingProject)
        }
    }

    fun setCurrentProjectImages(images: List<String>) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.images = images
            setCurrentProject(existingProject)
        }
    }

    fun addToExistingProjectImages(images: List<String>) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            val existingImages = existingProject.images.toMutableList()
            existingImages.addAll(images)
            existingProject.images = existingImages
            setCurrentProject(existingProject)
        }
    }

    fun setCurrentProjectTags(tags: List<String>) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.tags = tags
            setCurrentProject(existingProject)
        }
    }

    fun setCurrentProjectLocation(location: Location) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.location = location
            setCurrentProject(existingProject)
        }
    }

    fun insertCurrentUser(localUser: User) = viewModelScope.launch (Dispatchers.IO) {
        localUser.isCurrentUser = true
        insertUser(localUser)
    }

    private fun insertUser(localUser: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUser(localUser)
    }

    fun deleteProjectImageAtPosition(pos: Int) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            val existingImages = existingProject.images.toMutableList()
            existingImages.removeAt(pos)
            existingProject.images = existingImages
            setCurrentProject(existingProject)
        }
    }

    fun createProject(onComplete: (task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val project = currentProject.value!!
        val currentUser = UserManager.currentUser

        val chatChannelId = randomId()
        project.chatChannel = chatChannelId

        FireUtility.createProject(project) {
            if (it.isSuccessful) {
                val existingList = currentUser.projects.toMutableList()
                existingList.add(project.id)

                val chatChannels = currentUser.chatChannels.toMutableList()
                chatChannels.add(project.chatChannel)

                project.isMadeByMe = true
                insertNewProject(project)

                val tokens = mutableListOf(currentUser.token)

                ChatChannel.newInstance(project)

                val chatChannel = ChatChannel(
                    chatChannelId,
                    project.id,
                    project.name,
                    project.images.first(),
                    project.contributors.size.toLong(),
                    listOf(project.creator.userId),
                    listOf(project.creator.userId),
                    "",
                    project.createdAt,
                    project.updatedAt,
                    null,
                    tokens
                )

                insertChatChannelsWithoutProcessing(listOf(chatChannel))

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
                insertCurrentUser(updatedUser)
                updateLocalProjects(updatedUser, updatedUser.projects)
            } else {
                setCurrentError(it.exception)
            }
        }
    }

    private fun updateLocalProjects(updatedUser: User, projects: List<String>) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalProjects(updatedUser, projects)
    }

    fun checkIfUsernameTaken(username: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.checkIfUserNameTaken(username, onComplete)
    }

    /*fun updateUserLocally(changes: Map<String, Any?>) = viewModelScope.launch (Dispatchers.IO) {
        val existingUser = currentUser.value
        if (existingUser != null) {
            if (changes.containsKey("name")) {
                val newName = changes["name"] as String
                if (existingUser.name != newName) {
                    existingUser.name = newName
                }
            }

            if (changes.containsKey("username")) {
                val newUsername = changes["username"] as String
                if (existingUser.username != newUsername) {
                    existingUser.username = newUsername
                }
            }

            if (changes.containsKey("tag")) {
                val newTag = changes["tag"] as String
                if (existingUser.tag != newTag) {
                    existingUser.tag = newTag
                }
            }

            if (changes.containsKey("interests")) {
                existingUser.interests = changes["interests"] as List<String>
            }

            if (changes.containsKey("about")) {
                val newAbout = changes["about"] as String
                if (existingUser.about != newAbout) {
                    existingUser.about = newAbout
                }
            }

            if (changes.containsKey("photo")) {
                val newPhoto = changes["photo"] as String?
                if (existingUser.photo != newPhoto) {
                    existingUser.photo = newPhoto
                }
            }

            if (changes.containsKey("projects")) {
                existingUser.projects = changes["projects"] as List<String>
            }

            if (changes.containsKey("projectsCount")) {
                val newProjectsCount = changes["projectsCount"] as Long
                if (existingUser.projectsCount != newProjectsCount) {
                    existingUser.projectsCount = newProjectsCount
                }
            }

            if (changes.containsKey("collaborations")) {
                val newCollaborations = changes["collaborations"] as List<String>
                if (existingUser.collaborations != newCollaborations) {
                    existingUser.collaborations = newCollaborations
                }
            }

            if (changes.containsKey("collaborationsCount")) {
                val newCollaborationsCount = changes["collaborationsCount"] as Long
                if (existingUser.collaborationsCount != newCollaborationsCount) {
                    existingUser.collaborationsCount = newCollaborationsCount
                }
            }

            if (changes.containsKey("projectRequests")) {
                val newProjectRequests = changes["projectRequests"] as List<String>
                if (existingUser.projectRequests != newProjectRequests) {
                    existingUser.projectRequests = newProjectRequests
                }
            }

            if (changes.containsKey("chatChannels")) {
                val newChatChannels = changes["chatChannels"] as List<String>
                if (existingUser.chatChannels != newChatChannels) {
                    existingUser.chatChannels = newChatChannels
                }
            }

            repo.insertCurrentUser(existingUser)
        }

    }*/

    fun signOut(onComplete: () -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        repo.clearDatabases(onComplete)
    }

    @ExperimentalPagingApi
    fun getProjectsNearMe(): Flow<PagingData<Project>>{
        return Pager(
            config = PagingConfig(pageSize = 20)
        ) {
            repo.projectDao.getProjectsNearMe()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getFeedItems(query: Query, tag: String? = null): Flow<PagingData<Project>> {
        return if (tag != null) {
            Pager(
                config = PagingConfig(pageSize = 20),
                remoteMediator = ProjectRemoteMediator(query, repo, true)
            ) {
                repo.projectDao.getTagProjects("%$tag%")
            }.flow.cachedIn(viewModelScope)
        } else {
            Pager(
                config = PagingConfig(pageSize = 20),
                remoteMediator = ProjectRemoteMediator(query, repo, true)
            ) {
                repo.projectDao.getPagedProjects()
            }.flow.cachedIn(viewModelScope)
        }
    }

    private fun insertNewProject(project: Project) = viewModelScope.launch(Dispatchers.IO) {
        val currentUser = UserManager.currentUser
        repo.insertProjects(arrayOf(project))
        currentUser.projectsCount += 1

        val newProjectsList = currentUser.projects.addItemToList(project.id)
        currentUser.projects = newProjectsList

        val newChannelsList = currentUser.chatChannels.addItemToList(project.chatChannel)
        currentUser.chatChannels = newChannelsList

        insertCurrentUser(currentUser)
    }

    fun getCurrentUserProjects(): LiveData<List<Project>> {
        return repo.getCurrentUserProjects()
    }

    @ExperimentalPagingApi
    fun getCurrentUserProjects(query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getCurrentUserPagedProjects()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getPagedProjectRequests(): Flow<PagingData<ProjectRequest>> {
        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection("projectRequests")
            .whereEqualTo("receiverId", currentUser.id)
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRequestRemoteMediator(query, repo)
        ) {
            repo.projectRequestDao.getPagedProjectRequests(currentUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getCollaborations(query: Query): Flow<PagingData<Project>> {
        val currentUserId = UserManager.currentUserId
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getPagedCollaborations("%${currentUserId}%", currentUserId)
        }.flow.cachedIn(viewModelScope)
    }


    @ExperimentalPagingApi
    fun getOtherUserProjects(query: Query, otherUser: User): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getPagedOtherUserProjects(otherUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getOtherUserCollaborations(query: Query, otherUser: User): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getOtherUserPagedCollaborations("%${otherUser.id}%", otherUser.id)
        }.flow.cachedIn(viewModelScope)
    }

    fun getOtherUser(userId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("users").document(userId)
        FireUtility.getDocument(ref, onComplete)
    }

    fun likeLocalUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.likeLocalUserById(userId)
    }

    fun dislikeLocalUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.dislikeLocalUserById(userId)
    }

    @ExperimentalPagingApi
    fun getSavedProjects(query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getPagedSavedProjects()
        }.flow.cachedIn(viewModelScope)
    }

    fun getCommentChannel(project: Project, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("commentChannels")
            .document(project.commentChannel)
        FireUtility.getDocument(ref, onComplete)
    }

    fun sendComment(comment: Comment, parent: Any) {
        val parentChannelId: String?
        val currentUser = UserManager.currentUser
        val notification = when (parent) {
            is Project -> {
                parentChannelId = null
                val content = currentUser.name + " commented on your project"
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
                throw IllegalArgumentException("Only project and comment object is accepted.")
            }
        }

        FireUtility.sendComment(comment, parentChannelId) {
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
        val project = getLocalProject(comment.projectId)
        if (project != null) {
            project.comments += 1
            insertProjects(project)
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

    fun insertComment(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertComments(listOf(comment))
    }

    @ExperimentalPagingApi
    fun getPagedComments(commentChannelId: String, query: Query): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = CommentRemoteMediator(query, repo)
        ) {
            repo.commentDao.getPagedComments(commentChannelId)
        }.flow
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return repo.getLocalChannelContributors(chatChannel)
    }

    fun updateProject(onComplete: (Project, task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val currentProject = currentProject.value!!
        FireUtility.updateProject(currentProject, onComplete)
    }


    fun updateLocalProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalProject(project)
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

    private fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertChatChannelsWithoutProcessing(channels)
    }

    fun getLocalUser(userId: String, onComplete: (User?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getUser(userId))
    }

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return repo.getDocumentMessages(chatChannelId)
    }


    fun getTagProjects(tag: String, query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo, false)
        ) {
            repo.projectDao.getTagProjects("%$tag%")
        }.flow.cachedIn(viewModelScope)
    }

    fun setCurrentProjectLinks(links: List<String>) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            existingProject.sources = links
            setCurrentProject(existingProject)
        }
    }

    fun deleteComment(comment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteComment(comment)
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

    fun insertProjects(vararg projects: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertProjects(projects)
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

    fun insertProjectRequests(requests: List<ProjectRequest>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertProjectRequests(requests)
    }

    fun insertProjectRequests(vararg projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertProjectRequests(projectRequest)
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

    suspend fun getLocalProject(projectId: String): Project? {
        return repo.getProject(projectId)
    }

    fun getLocalProject(projectId: String, onComplete: (Project?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getLocalProject(projectId))
    }

    fun deleteNotification(notification: Notification) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteNotification(notification)
    }

    fun deleteProjectRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteProjectRequest(projectRequest)
    }

    @ExperimentalPagingApi
    fun getProjectInvites(): Flow<PagingData<ProjectInvite>> {
        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection("users")
            .document(currentUser.id)
            .collection("invites")

        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectInviteRemoteMediator(query, repo)
        ) {
            repo.projectInviteDao.getProjectInvites()
        }.flow.cachedIn(viewModelScope)
    }

    fun addCurrentUserToProject(projectInvite: ProjectInvite) = viewModelScope.launch (Dispatchers.IO) {
        deleteProjectInvite(projectInvite)
    }

    // in future this should not be private
    fun deleteProjectInvite(projectInvite: ProjectInvite) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteProjectInvite(projectInvite)
    }

    fun insertProjectInvites(vararg newProjectInvites: ProjectInvite) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertProjectInvites(newProjectInvites)
    }

    fun updateOtherUserLocally(otherUser: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertOtherUser(otherUser)
    }

    fun insertProjectsWithoutProcessing(vararg projects: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertProjectsWithoutProcessing(projects)
    }

    /*fun getLatestMessages(chatChannel: ChatChannel, onComplete: () -> Unit) {
        if (chatChannel.lastMessage != null) {
            chatRepository.getLatestMessages(chatChannel, onComplete)
        }
    }
*/
    fun deleteNotificationById(id: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteNotificationById(id)
    }

    fun deleteLocalProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteLocalProject(project)
    }

    fun getArchivedProjects(query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo, false)
        ) {
            repo.projectDao.getArchivedProjects()
        }.flow.cachedIn(viewModelScope)
    }

    fun deleteAdProjects() = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteAdProjects()
    }

    fun getChannelContributorsLive(formattedChannelId: String): LiveData<List<User>> {
        return repo.getChannelContributorsLive(formattedChannelId)
    }

    fun deleteLocalProjectRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteProjectRequest(projectRequest)
    }

    fun deleteLocalChatChannelById(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteLocalChatChannelById(chatChannelId)
    }

    fun getReactiveUser(userId: String): LiveData<User> {
        return repo.getReactiveUser(userId)
    }

    fun getReactiveProject(projectId: String): LiveData<Project> {
        return repo.getReactiveProject(projectId)
    }

    fun getReactiveComment(commentId: String): LiveData<Comment> {
        return repo.getReactiveComment(commentId)
    }

    fun getProjectSupporters(query: Query, projectId: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = UserRemoteMediator(query, repo)
        ) {
            repo.userDao.getProjectSupporters("%$projectId%")
        }.flow.cachedIn(viewModelScope)
    }

    fun getUserSupporters(query: Query, userId: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = UserRemoteMediator(query, repo)
        ) {
            repo.userDao.getUserSupporters("%$userId%")
        }.flow.cachedIn(viewModelScope)
    }

    fun getMyProjectRequests(query: Query): Flow<PagingData<ProjectRequest>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRequestRemoteMediator(query, repo)
        ) {
            repo.projectRequestDao.getMyProjectRequests(UserManager.currentUserId)
        }.flow.cachedIn(viewModelScope)
    }

    fun getCachedChatChannel(chatChannelId: String): ChatChannel? {
        return chatChannelCache[chatChannelId]
    }

    fun putChatChannelToCache(channel: ChatChannel) {
        chatChannelCache[channel.chatChannelId] = channel
    }

    fun getCachedCommentChannel(channelId: String): CommentChannel? {
        return commentChannelCache[channelId]
    }

    fun putCommentChannelToCache(commentChannel: CommentChannel) {
        commentChannelCache[commentChannel.commentChannelId] = commentChannel
    }

    fun getCachedComment(commentId: String): Comment? {
        return commentCache[commentId]
    }

    fun insertCommentToCache(comment: Comment) {
        commentCache[comment.commentId] = comment
    }

    fun getCachedUser(senderId: String): User? {
        return userCache[senderId]
    }

    fun disableLocationBasedProjects() = viewModelScope.launch (Dispatchers.IO) {
        repo.disableLocationBasedProjects()
    }

    fun getProjectRequest(projectId: String, onComplete: (ProjectRequest?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getProjectRequest(projectId))
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
            )
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
     * @param chatChannelId The chat channel id to where this project belongs
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
     * @param chatChannelId The chat channel id to where this project belongs
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

    fun getMediaMessages(chatChannelId: String, limit: Int = 6): LiveData<List<Message>> {
        return chatRepository.getMediaMessages(chatChannelId, limit)
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

    fun removeProjectFromUserLocally(chatChannelId: String, projectId: String, user: User) {
        val newList = user.chatChannels.removeItemFromList(chatChannelId)
        user.chatChannels = newList
        val newList1 = user.collaborations.removeItemFromList(projectId)
        user.collaborations = newList1
        user.collaborationsCount -= 1
        insertUsers(user)
    }

    fun clearAllChannels() = viewModelScope.launch (Dispatchers.IO) {
        chatRepository.clearChatChannels()
    }

    fun setCurrentFocusedComment(comment: Comment) {
        _currentFocusedComment.postValue(comment)
    }

    // We cannot set snapshot listener in every project that's why,
    // when a project request is accepted, to reflect the changes locally,
    // we need to check if something has changed in user document and respectively
    // make the changes locally.
    fun checkAndUpdateLocalProjects(currentUser: User) = viewModelScope.launch (Dispatchers.IO) {
        for (project in currentUser.collaborations) {
            val mProject = getLocalProject(project)
            if (mProject != null) {
                if (mProject.isRequested || !mProject.isCollaboration) {
                    mProject.isRequested = false
                    mProject.isCollaboration = true

                    val newContList = mProject.contributors.addItemToList(currentUser.id)
                    mProject.contributors = newContList

                    mProject.updatedAt = System.currentTimeMillis()

                    getProjectRequest(mProject.id) {
                        if (it != null) {
                            val newRequestsList = mProject.requests.removeItemFromList(it.requestId)
                            mProject.requests = newRequestsList

                            deleteProjectRequest(it)
                        }
                        updateLocalProject(mProject)
                    }
                }
            }
        }
    }

    fun insertUserToCache(creator: User) {
        userCache[creator.id] = creator
    }

    fun getCachedProject(id: String): Project? {
        return projectCache[id]
    }

    fun insertProjectToCache(project: Project) {
        projectCache[project.id] = project
    }

    fun getUser(senderId: String, function: (User?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        function(repo.getUser(senderId))
    }

    fun getProject(projectId: String, function: (Project?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        function(repo.getProject(projectId))
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