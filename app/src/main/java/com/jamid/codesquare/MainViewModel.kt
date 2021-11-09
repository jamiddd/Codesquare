package com.jamid.codesquare

import android.app.Application
import android.location.Address
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
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
import java.util.*

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val repo: MainRepository

    init {
        val db = CodesquareDatabase.getInstance(application.applicationContext, viewModelScope)
        repo = MainRepository.getInstance(db)

        /*FireUtility.getLatestUserData {
            if (it.isSuccessful && it.result.exists()) {
                val user = it.result.toObject(User::class.java)!!
                viewModelScope.launch(Dispatchers.IO) {
                    insertCurrentUser(user)
                }
            }
        }*/

    }

    private val _currentError = MutableLiveData<Exception?>()
    val currentError: LiveData<Exception?> = _currentError

    private val _addresses = MutableLiveData<List<Address>>().apply { value = emptyList() }
    val addresses: LiveData<List<Address>> = _addresses

    val currentUser: LiveData<User> = repo.currentUser

    private val _currentProject = MutableLiveData<Project>().apply { value = null }
    val currentProject: LiveData<Project> = _currentProject

    private val _currentImage = MutableLiveData<Uri?>()
    val currentImage: LiveData<Uri?> = _currentImage

    val currentCommentChannelIds = Stack<String>()
    var currentChatChannel: String? = null

    private val _chatImagesUpload = MutableLiveData<List<Uri>>()
    val chatImagesUpload: LiveData<List<Uri>> = _chatImagesUpload

    private val _chatDocumentsUpload = MutableLiveData<List<Uri>>()
    val chatDocumentsUpload: LiveData<List<Uri>> = _chatDocumentsUpload

    val chatScrollPositions = mutableMapOf<String, Int>()

    val onMessagesModeChanged = repo.onMessagesModeChanged

    private val _singleSelectedMessage = MutableLiveData<Message?>()
    val singleSelectedMessage: LiveData<Message?> = _singleSelectedMessage

    private val _searchProjectsResult = MutableLiveData<List<SearchResult>?>()
    val searchProjectsResult: LiveData<List<SearchResult>?> = _searchProjectsResult

    private val _searchUsersResult = MutableLiveData<List<SearchResult>?>()
    val searchUsersResult: LiveData<List<SearchResult>?> = _searchUsersResult

    fun setProjectsResult(results: List<SearchResult>?) {
        _searchProjectsResult.postValue(results)
    }

    fun setUsersResult(results: List<SearchResult>?) {
        _searchUsersResult.postValue(results)
    }

    fun setCurrentlySelectedMessage(message: Message?) {
        _singleSelectedMessage.postValue(message)
    }

    private val _forwardList = MutableLiveData<List<ChatChannel>>()
    val forwardList: LiveData<List<ChatChannel>> = _forwardList

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

    fun removeChannelFromForwardList(chatChannel: ChatChannel) {
        val oldList = forwardList.value
        if (oldList != null && oldList.isNotEmpty()) {
            val newList = oldList.toMutableList()
            newList.remove(chatChannel)
            _forwardList.postValue(newList)
        }
    }

    fun clearForwardList() {
        _forwardList.postValue(emptyList())
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
            existingProject.title = title
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

    fun setCurrentProjectLocation(address: String) {
        val existingProject = currentProject.value
        if (existingProject != null) {
            val existingLocation = existingProject.location
            val newLocation = Location(existingLocation.latitude, existingLocation.longitude, address)
            existingProject.location = newLocation
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

    fun insertAddresses(addresses: List<Address>) {
        _addresses.postValue(addresses)
    }

    fun createProject(onComplete: (task: Task<Void>) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val project = currentProject.value!!
        val currentUser = currentUser.value!!
        FireUtility.createProject(currentUser, project) {
            if (it.isSuccessful) {
                val existingList = currentUser.projects.toMutableList()
                existingList.add(project.id)

                val chatChannels = currentUser.chatChannels.toMutableList()
                chatChannels.add(project.chatChannel)

                val changes = mapOf(
                    "projectsCount" to currentUser.projectsCount + 1,
                    "projects" to existingList,
                    "chatChannels" to chatChannels
                )

                updateUserLocally(changes)

                project.isMadeByMe = true
                insertProject(project)

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
        val currentUser = currentUser.value!!
        FireUtility.updateUser2(currentUser, changes) {
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

    fun updateUserLocally(changes: Map<String, Any?>) = viewModelScope.launch (Dispatchers.IO) {
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

    }

    fun signOut() = viewModelScope.launch (Dispatchers.IO) {
        repo.clearDatabases()
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

    private fun insertProject(project: Project) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertProjects(listOf(project))
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
    fun getPagedChatChannels(query: Query): Flow<PagingData<ChatChannel>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ChatChannelRemoteMediator(query, repo)
        ) {
            repo.chatChannelDao.getPagedChatChannels()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getPagedForwardChatChannels(originalChannel: String, query: Query): Flow<PagingData<ChatChannel>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ChatChannelRemoteMediator(query, repo)
        ) {
            repo.chatChannelDao.getPagedChatChannels()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getPagedMessages(chatChannel: ChatChannel, imagesDir: File, documentsDir: File, chatChannelId: String, query: Query): Flow<PagingData<Message>> {
        return Pager(config =
        PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            maxSize = 150,
            prefetchDistance = 20,
            initialLoadSize= 40),
            remoteMediator = MessageRemoteMediator(chatChannel, imagesDir, documentsDir, viewModelScope, query, repo)
        ) {
            repo.messageDao.getChannelPagedMessages(chatChannelId)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun getPagedProjectRequests(query: Query): Flow<PagingData<ProjectRequest>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRequestRemoteMediator(query, repo)
        ) {
            repo.projectRequestDao.getPagedProjectRequests()
        }.flow.cachedIn(viewModelScope)
    }

    fun onLikePressed(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onLikePressed(project)
    }

    fun onSavePressed(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onSavePressed(project)
    }

    fun onJoinProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onJoinProject(project)
    }

    fun insertUserAndProject(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        projectRequest.sender?.let { repo.insertUser(it) }
        val project = projectRequest.project
        if (project != null) {
            repo.insertProjects(listOf(project))
        }
    }

    fun acceptRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        val project = projectRequest.project
        if (project != null) {
            onAccept(project, projectRequest)
        } else {
            val projectRef = Firebase.firestore.collection("projects").document(projectRequest.projectId)

            when (val newProjectResult = FireUtility.getDocument(projectRef)) {
                is Result.Error -> setCurrentError(newProjectResult.exception)
                is Result.Success -> {
                    val newProject = newProjectResult.data.toObject(Project::class.java)!!
                    onAccept(newProject, projectRequest)
                }
            }
        }
    }

    private fun onAccept(project: Project, projectRequest: ProjectRequest) {
        FireUtility.acceptProjectRequest(project, projectRequest) {
            if (it.isSuccessful) {
                postAcceptRequest(project, projectRequest)
            } else {
                setCurrentError(it.exception)
            }
        }
    }

    // make local changes after accepting request
    private fun postAcceptRequest(project: Project, projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {

        // 1. project
        val newRequestsList = project.requests.toMutableList()
        newRequestsList.remove(projectRequest.requestId)
        project.requests = newRequestsList

        // 2. insert the new contributor
        val otherUserRef = Firebase.firestore.collection("users").document(projectRequest.senderId)
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
    }

    fun rejectRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        when (val res = FireUtility.rejectRequest(projectRequest)) {
            is Result.Error -> setCurrentError(res.exception)
            is Result.Success -> {
                // remove project request locally, and also update if there is any local project
                val project = projectRequest.project
                if (project != null) {
                    postReject(projectRequest.requestId, project)
                    repo.deleteProjectRequest(projectRequest)
                } else {
                    val newProject = repo.getProject(projectRequest.projectId)
                    if (newProject != null) {
                        postReject(projectRequest.requestId, newProject)
                        repo.deleteProjectRequest(projectRequest)
                    }
                }
            }
        }
    }

    private fun postReject(requestId: String, project: Project) {
        val existingRequests = project.requests.toMutableList()
        existingRequests.remove(requestId)
        project.requests = existingRequests
        insertProject(project)
    }

    @ExperimentalPagingApi
    fun getCollaborations(query: Query): Flow<PagingData<Project>> {
        val currentUser = currentUser.value!!
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo)
        ) {
            repo.projectDao.getPagedCollaborations("%${currentUser.id}%", currentUser.id)
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

    fun likeUser(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        val currentUser = currentUser.value!!
        when (val likeUserResult = FireUtility.likeUser(currentUser, userId)) {
            is Result.Error -> {
                setCurrentError(likeUserResult.exception)
            }
            is Result.Success -> {
                insertCurrentUser(likeUserResult.data)
            }
        }
    }

    fun dislikeUser(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        val currentUser = currentUser.value!!
        when (val likeUserResult = FireUtility.dislikeUser(currentUser, userId)) {
            is Result.Error -> {
                setCurrentError(likeUserResult.exception)
            }
            is Result.Success -> {
                insertCurrentUser(likeUserResult.data)
            }
        }
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

    fun getProjectContributors(limit: Long = 0, project: Project, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.getProjectContributors(limit, project, onComplete)
    }

    fun getCommentChannel(project: Project, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("commentChannels")
            .document(project.commentChannel)
        FireUtility.getDocument(ref, onComplete)
    }

    fun sendComment(comment: Comment, parentChannelId: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        when (val result = FireUtility.sendComment(comment, parentChannelId)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                val project = repo.getProject(comment.projectId)
                if (project != null) {
                    project.comments += 1
                    insertProject(project)
                }

                if (comment.commentLevel >= 1) {
                    val parentComment1 = repo.getComment(comment.parentId)
                    if (parentComment1 != null) {
                        parentComment1.repliesCount += 1
                        insertComment(parentComment1)
                    }
                }

                insertComment(result.data)
            }
        }
    }

    fun insertComment(parentComment: Comment) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertComment(parentComment)
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

    fun onCommentLiked(comment: Comment) = viewModelScope.launch (Dispatchers.IO)  {
        repo.onCommentLiked(comment)
    }


    fun getChannelUsers(channel: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.getChannelUsers(channel, onComplete)
    }

    /*fun getAllChannelUsers(channels: List<String>, onComplete: (channel: ChatChannel, task: Task<QuerySnapshot>) -> Unit) {
        for (channel in channels) {
            getChannelUsers(channel, onComplete(ch))
        }
    }*/

    // to insert all the users related to the local projects
    /*fun getChannelUsers(chatChannels: List<String>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.getChannelUsers(chatChannels)) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                repo.insertUsers(result.data)

                val channels = repo.getAllLocalChatChannels()
                if (channels.isNotEmpty()) {
                    for (channel in channels) {

                        val lastMessage = channel.lastMessage

                        if (lastMessage != null) {
                            val sender = repo.getUser(lastMessage.senderId)
                            if (sender != null) {
                                lastMessage.sender = sender
                                channel.lastMessage = lastMessage
                            }
                        }
                    }

                    insertChatChannels(channels)
                }
            }
        }
    }*/

    fun sendTextMessage(externalImagesDir: File, externalDocumentsDir: File, chatChannelId: String, content: String, replyTo: String? = null, replyMessage: MessageMinimal? = null) = viewModelScope.launch (Dispatchers.IO) {
        val currentUser = currentUser.value!!
        when (val result = FireUtility.sendTextMessage(currentUser, chatChannelId, content, replyTo, replyMessage)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                repo.insertMessages(externalImagesDir, externalDocumentsDir, listOf(result.data))
                val chatChannel = repo.getLocalChatChannel(chatChannelId)
                if (chatChannel != null) {
                    chatChannel.lastMessage = result.data
                    chatChannel.updatedAt = result.data.createdAt

                    Log.d(TAG, chatChannel.toString())

                    repo.insertChatChannels(listOf(chatChannel))
                }
            }
        }
    }


    // TODO("Need to implement this in such a way that
    //  the message should be set as downloaded, because one shouldn't download an image which was
    //  already there in the phone locally, we just need to copy paste the image from the location
    //  to our app's location.")
    fun sendMessagesSimultaneously(imagesDir: File, documentsDir: File, chatChannelId: String, listOfMessages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        setChatUploadImages(emptyList())
        setChatUploadDocuments(emptyList())

        when (val result = FireUtility.sendMessagesSimultaneously(chatChannelId, listOfMessages)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {

                val messages = result.data
                repo.insertMessages(imagesDir, documentsDir, messages)

                val chatChannel = repo.getLocalChatChannel(chatChannelId)

                if (chatChannel != null) {
                    chatChannel.lastMessage = messages.last()
                    chatChannel.updatedAt = messages.last().createdAt
                    repo.insertChatChannels(listOf(chatChannel))
                }
            }
        }
    }

    fun deleteChatUploadImageAtPosition(delPos: Int) {
        val chatImages = chatImagesUpload.value
        if (chatImages != null) {
            val existingList = chatImages.toMutableList()
            existingList.removeAt(delPos)
            setChatUploadImages(existingList)
        }
    }

    fun insertMessage(imagesDir: File, documentsDir: File, message: Message) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertMessages(imagesDir, documentsDir, listOf(message))
    }

    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateMessage(message)
    }

    fun updateMessages(messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateMessages(messages)
    }

    suspend fun getProjectByChatChannel(channelId: String): Project? {
        return repo.getProjectByChatChannel(channelId)
    }

    suspend fun getLocalChannelContributors(chatChannel: String): List<User> {
        return repo.getLocalChannelContributors(chatChannel)
    }

    fun updateProject(projectId: String, changes: Map<String, Any>, onComplete: (task: Task<Void>) -> Unit) {
        val ref = Firebase.firestore.collection("projects").document(projectId)
        FireUtility.updateDocument(ref, changes, onComplete)
    }

    fun updateLocalProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateLocalProject(project)
    }

    suspend fun getLocalChatChannel(chatChannel: String): ChatChannel? {
        return repo.getLocalChatChannel(chatChannel)
    }

    fun getLiveProjectByChatChannel(chatChannel: String): LiveData<Project> {
        return repo.getLiveProjectByChatChannel(chatChannel)
    }

    suspend fun getLimitedMediaMessages(chatChannel: String, limit: Int): List<Message> {
        return repo.getLimitedMediaMessages(chatChannel, limit)
    }

    fun deleteChatUploadDocumentAtPosition(delPos: Int) {
        val chatDocuments = chatDocumentsUpload.value
        if (chatDocuments != null) {
            val existingList = chatDocuments.toMutableList()
            existingList.removeAt(delPos)
            setChatUploadDocuments(existingList)
        }
    }

    // insert channel messages and also update the channel along with it
    fun insertChannelMessages(chatChannel: ChatChannel, imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        if (messages.isNotEmpty()) {
            val lastMessage = messages.first()
            chatChannel.lastMessage = lastMessage
            chatChannel.updatedAt = lastMessage.createdAt
            repo.insertChatChannels(listOf(chatChannel))
            insertMessages(imagesDir, documentsDir, messages)
        }
    }

    private fun insertMessages(imagesDir: File, documentsDir: File, messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertMessages(imagesDir, documentsDir, messages)
    }

    fun updateDeliveryListOfMessages(chatChannel: ChatChannel, currentUserId: String, messages: List<Message>, onComplete: (task: Task<Void>) -> Unit) {
        repo.updateDeliveryListOfMessages(chatChannel, currentUserId, messages, onComplete)
    }

    fun insertChatChannels(chatChannels: List<ChatChannel>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertChatChannels(chatChannels)
    }

    fun insertChatChannelsWithoutProcessing(channels: List<ChatChannel>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertChatChannelsWithoutProcessing(channels)
    }

    fun getChatPagingInstance(
        currentChatChannel: String,
        externalImagesDir: File,
        externalDocumentsDir: File
    ): ChatPaging {
        return ChatPaging(externalImagesDir, externalDocumentsDir, currentChatChannel, repo, viewModelScope)
    }

    suspend fun getLocalUser(userId: String): User? {
        return repo.getUser(userId)
    }

    suspend fun getDocumentMessages(chatChannelId: String): List<Message> {
        return repo.getDocumentMessages(chatChannelId)
    }

    @ExperimentalPagingApi
    fun getTagProjects(tag: String, query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo, false)
        ) {
            repo.projectDao.getTagProjects("%$tag%")
        }.flow.cachedIn(viewModelScope)
    }

    fun getLiveProjectById(id: String): LiveData<Project> {
        return repo.getLiveProjectById(id)
    }

    fun deleteProject(project: Project, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.deleteProject(project) {
            onComplete(it)
            if (it.isSuccessful) {
                viewModelScope.launch (Dispatchers.IO) {
                    repo.deleteProject(project)
                }
            }
        }
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
        repo.insertComment(comment)
    }

    fun deleteUserById(userId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteUserById(userId)
    }

    suspend fun getImageMessages(chatChannelId: String, limit: Int = 0): List<Message> {
        return repo.getImageMessages(chatChannelId, limit)
    }

    suspend fun getForwardChannels(chatChannelId: String): List<ChatChannel> {
        return repo.getForwardChannels(chatChannelId)
    }

    fun sendForwardsToChatChannels(imagesDir: File, documentsDir: File, messages: List<Message>, channels: List<ChatChannel>, onComplete: (result: Result<List<Message>>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val currentUser = currentUser.value!!

        val result = FireUtility.sendMultipleMessageToMultipleChannels(currentUser, messages, channels)
        when (result) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                val newMessages = result.data
                insertMessages(imagesDir, documentsDir, newMessages)

                for (i in channels.indices) {
                    val channelMessages = newMessages.filter {
                        it.chatChannelId == channels[i].chatChannelId
                    }
                    if (channelMessages.isNotEmpty()) {
                        val lastMessage = channelMessages.last()
                        lastMessage.chatChannelId = channels[i].chatChannelId
                        channels[i].lastMessage = lastMessage
                        channels[i].updatedAt = lastMessage.createdAt
                    }
                }

                insertChatChannels(channels)
            }
        }
        onComplete(result)
    }



    fun updateReadList(currentUser: User, imagesDir: File, documentsDir: File, message: Message) = viewModelScope.launch (Dispatchers.IO) {

        val chatChannel = getLocalChatChannel(message.chatChannelId)
        if (chatChannel != null) {
            FireUtility.updateReadList(chatChannel, currentUser, message) {
                if (it.isSuccessful) {
                    val newList = message.readList.addItemToList(currentUser.id)
                    message.readList = newList

                    if (chatChannel.lastMessage?.messageId == message.messageId) {
                        chatChannel.lastMessage = message
                        chatChannel.updatedAt = System.currentTimeMillis()
                        insertChatChannelsWithoutProcessing(listOf(chatChannel))
                    }

                    insertMessages(imagesDir, documentsDir, listOf(message))
                } else {
                    setCurrentError(it.exception)
                }
            }
        }
    }

    fun updateChannelsWithUsers(chatChannels: List<ChatChannel>, users: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        if (chatChannels.isNotEmpty()) {
            for (user in users) {
                for (channel in chatChannels) {
                    if (channel.lastMessage != null && channel.lastMessage!!.senderId == user.id) {
                        channel.lastMessage!!.sender = user
                    }
                }
            }
        }
    }

    private suspend fun getAllLocalChatChannels(): List<ChatChannel> {
        return repo.getAllLocalChatChannels()
    }

    fun getAllChatChannels(userId: String, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.getAllChatChannels(userId, onComplete)
    }

    /*fun insertUsers(users: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertUsers(users)
    }*/

    fun insertChannelWithContributors(channel: ChatChannel, contributors: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        contributors.find {
            channel.lastMessage?.senderId == it.id
        }?.let {
            channel.lastMessage?.sender = it
        }

        if (channel.lastMessage?.senderId == currentUser.value?.id) {
            channel.lastMessage?.sender = currentUser.value!!
        }

        repo.insertUsers(contributors)
        repo.insertChatChannelsWithoutProcessing(listOf(channel))
    }

    fun insertChannelsWithUsers(chatChannels: List<ChatChannel>, users: List<User>) = viewModelScope.launch (Dispatchers.IO) {
        if (chatChannels.isNotEmpty()) {
            for (user in users) {
                for (channel in chatChannels) {
                    if (channel.lastMessage != null && channel.lastMessage!!.senderId == user.id) {
                        channel.lastMessage!!.sender = user
                    }
                }
            }
        }
        repo.insertChatChannelsWithoutProcessing(chatChannels)
    }

    fun getChatChannel(channelId: String, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("chatChannels").document(channelId)
        FireUtility.getDocument(ref, onComplete)
    }

    suspend fun getLastMessageForChannel(chatChannelId: String): Message? {
        return repo.getLastMessageForChannel(chatChannelId)
    }

    fun getDocumentSnapshot(ref: DocumentReference, onComplete: (task: Task<DocumentSnapshot>) -> Unit) {
        FireUtility.getDocument(ref, onComplete)
    }

    fun deleteAllMessagesInChannel(chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
        repo.deleteAllMessagesInChannel(chatChannelId)
    }

    fun getLatestMessages(chatChannelId: String, limit: Int, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        val ref = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())

        FireUtility.getQuerySnapshot(ref, onComplete)
    }

    fun getLatestMessagesAfter(imagesDir: File, documentsDir: File, lastMessage: Message, channel: ChatChannel) {
        val ref = Firebase.firestore.collection("chatChannels")
            .document(lastMessage.chatChannelId)
            .collection("messages")
            .document(lastMessage.messageId)

        FireUtility.getDocument(ref) {
            if (it.isSuccessful) {
                val ref1 = Firebase.firestore.collection("chatChannels")
                    .document(lastMessage.chatChannelId)
                    .collection("messages")
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .startAfter(it.result.data)

                FireUtility.getQuerySnapshot(ref1) { it1 ->
                    if (it1.isSuccessful) {
                        val latestMessages = it1.result.toObjects(Message::class.java)
                        insertChannelMessages(channel, imagesDir, documentsDir, latestMessages)
                    } else {
                        setCurrentError(it1.exception)
                    }
                }
            } else {
                setCurrentError(it.exception)
            }
        }

    }

    fun updateRestOfTheMessages(chatChannelId: String, isSelected: Int) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateRestOfTheMessages(chatChannelId, isSelected)
    }

    suspend fun getSelectedMessages(): List<Message> {
        return repo.getSelectedMessages()
    }

    suspend fun getLocalMessage(messageId: String): Message? {
        return repo.getLocalMessage(messageId)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

}