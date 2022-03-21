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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class MainViewModel(application: Application): AndroidViewModel(application) {

    private val repo: MainRepository
    private val chatRepository: ChatRepository

    init {
        val db = CodesquareDatabase.getInstance(application.applicationContext)
        repo = MainRepository.getInstance(db)
        chatRepository = ChatRepository(viewModelScope, application.applicationContext, db)
    }

    private val _currentError = MutableLiveData<Exception?>()
    val currentError: LiveData<Exception?> = _currentError

    val currentUser: LiveData<User> = repo.currentUser

    /**
     * A placeholder project to be used while creating new project.
    * */
    private val _currentProject = MutableLiveData<Project>().apply { value = null }
    val currentProject: LiveData<Project> = _currentProject

    private val _currentImage = MutableLiveData<Uri?>()
    val currentImage: LiveData<Uri?> = _currentImage

    private val userCache = mutableMapOf<String, User>()
    private val projectCache = mutableMapOf<String, Project>()
    private val chatChannelCache = mutableMapOf<String, ChatChannel>()
    private val commentChannelCache = mutableMapOf<String, CommentChannel>()
    private val commentCache = mutableMapOf<String, Comment>()

    var currentChatChannel: String? = null

    val multipleImagesContainer = MutableLiveData<List<Uri>>().apply { value = emptyList() }

    val multipleDocumentsContainer = MutableLiveData<List<Uri>>().apply { value = emptyList() }

    private val _currentFocusedUser = MutableLiveData<User>().apply { value = null }
    val currentFocusedUser: LiveData<User> = _currentFocusedUser

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
     * Placeholder to hold chat images for upload
     * */
    private val _chatImagesUpload = MutableLiveData<List<Uri>>()
    val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload

    private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
    val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload

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

    /**
     * All the messages that are currently selected where the state of the message is [MESSAGE_SELECTED]
     *
     * */
    val selectedMessages = repo.onMessagesModeChanged

    /**
     * Placeholder for a message when only one message is selected
     * */
    private val _singleSelectedMessage = MutableLiveData<Message?>()
    val singleSelectedMessage: LiveData<Message?> = _singleSelectedMessage

    val chatChannels = repo.chatChannels
    val errors = repo.errors

    var currentUserBitmap: Bitmap? = null

    val allUnreadNotifications = repo.allUnreadNotifications

    val allPreviousQueries = repo.allPreviousQueries

    fun setNetworkAvailability(state: Boolean) {
        _isNetworkAvailable.postValue(state)
    }

    /*private val _searchResult = MutableLiveData<List<SearchQuery>>()
    val searchResult: LiveData<List<SearchQuery>> = _searchResult*/

    private val _recentSearchList = MutableLiveData<List<SearchQuery>>().apply { value = null }
    val recentSearchList: LiveData<List<SearchQuery>> = _recentSearchList

    private val client = ClientSearch(ApplicationID(BuildConfig.ALGOLIA_ID), APIKey(BuildConfig.ALGOLIA_SECRET))

    private val _networkError = MutableLiveData<Exception>().apply { value = null }
   /* val networkError: LiveData<Exception> = _networkError*/

    fun setCurrentlySelectedMessage(message: Message?) {
        _singleSelectedMessage.postValue(message)
    }

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

    fun setChatUploadImages(images: List<Uri>) {
        _chatImagesUpload.postValue(images)
    }

    fun setChatUploadDocuments(documents: List<Uri>) {
        _chatDocumentsUpload.postValue(documents)
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

                val chatChannel = ChatChannel(
                    chatChannelId,
                    project.id,
                    project.name,
                    project.images.first(),
                    project.contributors.size.toLong(),
                    listOf(project.creator.userId),
                    listOf(project.creator.userId),
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

    /*@ExperimentalPagingApi
    fun getPagedChatChannels(query: Query): Flow<PagingData<ChatChannel>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ChatChannelRemoteMediator(query, repo)
        ) {
            repo.chatChannelDao.getPagedChatChannels()
        }.flow.cachedIn(viewModelScope)
    }*/

   /* @ExperimentalPagingApi
    fun getPagedForwardChatChannels(originalChannel: String, query: Query): Flow<PagingData<ChatChannel>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ChatChannelRemoteMediator(query, repo)
        ) {
            repo.chatChannelDao.getPagedChatChannels()
        }.flow.cachedIn(viewModelScope)
    }*/

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


    fun acceptRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        val project = projectRequest.project
        if (project != null) {
            onAccept(project, projectRequest)
        } else {
            val projectRef = Firebase.firestore
                .collection("projects")
                .document(projectRequest.projectId)

            when (val newProjectResult = FireUtility.getDocument(projectRef)) {
                is Result.Error -> setCurrentError(newProjectResult.exception)
                is Result.Success -> {
                    val newProject = newProjectResult.data.toObject(Project::class.java)!!
                    onAccept(newProject, projectRequest)
                }
            }
        }
    }


    /*
    *
    * */

    private fun onAccept(project: Project, projectRequest: ProjectRequest) {
        val currentUser = UserManager.currentUser
        val title = project.name
        val content = currentUser.name + " has accepted your project request"
        val notification = Notification.createNotification(content, currentUser.id, projectRequest.senderId, projectId = project.id, title = title)

        FireUtility.acceptProjectRequest(project, projectRequest) {
            if (it.isSuccessful) {
                if (notification.senderId != notification.receiverId) {
                    FireUtility.checkIfNotificationExistsByContent(notification) { exists, error ->
                        if (error != null) {
                            setCurrentError(error)
                        } else {
                            if (!exists) {
                                FireUtility.sendNotification(notification) { it1 ->
                                    if (it1.isSuccessful) {
                                        postAcceptRequest(project, projectRequest, notification)
                                    } else {
                                        setCurrentError(it1.exception)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Not possible")
                }
            } else {
                setCurrentError(it.exception)
            }
        }
    }



    // make local changes after accepting request
    private fun postAcceptRequest(project: Project, projectRequest: ProjectRequest, notification: Notification) = viewModelScope.launch (Dispatchers.IO) {

        // 1. project
        val newRequestsList = project.requests.toMutableList()
        newRequestsList.remove(projectRequest.requestId)
        project.requests = newRequestsList

        // 2. insert the new contributor
        val otherUserRef = Firebase.firestore.collection(USERS).document(projectRequest.senderId)
        when (val requestSenderResult = FireUtility.getDocument(otherUserRef)) {
            is Result.Error -> setCurrentError(requestSenderResult.exception)
            is Result.Success -> {
                val otherUser = requestSenderResult.data.toObject(User::class.java)!!
                insertUser(otherUser)
            }
        }

        // 3. chat channel
        val chatChannel = repo.getLocalChatChannel(project.chatChannel)
        if (chatChannel != null) {
            val newContributorsList = chatChannel.contributors.toMutableList()
            newContributorsList.add(projectRequest.senderId)
            chatChannel.contributors = newContributorsList

            chatChannel.contributorsCount = chatChannel.contributorsCount + 1

            insertChatChannels(listOf(chatChannel))
        }

        // 4. delete project request
        repo.deleteProjectRequest(projectRequest)

        insertNotifications(notification)

        val requestNotificationId = projectRequest.notificationId
        deleteNotificationById(requestNotificationId)
    }

    /*private fun postReject(requestId: String, project: Project) {
        val existingRequests = project.requests.toMutableList()
        existingRequests.remove(requestId)
        project.requests = existingRequests
        insertNewProject(project)
    }*/

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
                Notification.createNotification(content, currentUser.id, parent.creator.userId, title = title, commentId = comment.commentId)
            }
            is Comment -> {
                parentChannelId = parent.commentChannelId
                val content = currentUser.name + " replied to your comment"
                Notification.createNotification(content, currentUser.id, parent.senderId, commentId = parent.commentId)
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

    fun deleteChatUploadImageAtPosition(delPos: Int) {
        val chatImages = chatImagesUpload.value
        if (chatImages != null) {
            val existingList = chatImages.toMutableList()
            existingList.removeAt(delPos)
            setChatUploadImages(existingList)
        }
    }

    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateMessage(message)
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return repo.getLocalChannelContributors(chatChannel)
    }

    fun updateProject(onComplete: (Project, task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val currentProject = currentProject.value!!
        FireUtility.updateProject(currentProject, onComplete)
    }

    fun updateProject(projectId: String, changes: Map<String, Any>, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("projects").document(projectId)
        FireUtility.updateDocument(ref, changes, onComplete)
    }

    fun updateLocalProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalProject(project)
    }

    fun getLocalChatChannel(chatChannelId: String, onComplete: (ChatChannel?) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val chatChannel = repo.getLocalChatChannel(chatChannelId)
        onComplete(chatChannel)
    }

    suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
        return repo.getLocalChatChannel(chatChannel)
    }

    fun getLimitedMediaMessages(chatChannelId: String, limit: Int, type: String = image, onComplete: (List<Message>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        onComplete(repo.getLimitedMediaMessages(chatChannelId, limit, type))
    }

    private fun insertMessages(imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
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

    suspend fun getChatChannel(channelId: String): Result<DocumentSnapshot> {
        val ref = Firebase.firestore.collection("chatChannels").document(channelId)
        return FireUtility.getDocument(ref)
    }

    fun getChatChannel(channelId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("chatChannels").document(channelId)
        FireUtility.getDocument(ref, onComplete)
    }

    /**
     * When selecting one or more messages, to make adjustment to other messages.
     * States [MESSAGE_IDLE], [MESSAGE_READY], [MESSAGE_SELECTED].
     *
     * [MESSAGE_IDLE] : When the message is not selected and also not ready to be selected
     *
     * [MESSAGE_READY] : When the message is ready to be selected
     *
     * [MESSAGE_SELECTED] : When the message is selected
     *
     * @param chatChannelId The channel id for the messages
     * @param state The state to update rest of the messages
     *
    * */
    fun updateRestOfTheMessages(chatChannelId: String, state: Int) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateRestOfTheMessages(chatChannelId, state)
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
        val project = projectInvite.project
        val currentUserId = projectInvite.receiverId
        if (project != null) {
            val newList = project.contributors.addItemToList(currentUserId)
            project.contributors = newList
            insertProjects(project)
        }

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

    fun getLatestMessages(chatChannel: ChatChannel, onComplete: () -> Unit) {
        if (chatChannel.lastMessage != null) {
            chatRepository.getLatestMessages(chatChannel, onComplete)
        }
    }

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

    fun getReactiveChatChannel(chatChannelId: String): LiveData<ChatChannel> {
        return repo.getReactiveChatChannel(chatChannelId)
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

    companion object {
        private const val TAG = "MainViewModel"
        const val MESSAGE_IDLE = -1
        const val MESSAGE_SELECTED = 1
        const val MESSAGE_READY = 0
    }

}