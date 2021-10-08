package com.jamid.codesquare

import android.app.Application
import android.location.Address
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.*
import com.jamid.codesquare.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val repo: MainRepository

    init {
        val db = CodesquareDatabase.getInstance(application.applicationContext, viewModelScope)
        repo = MainRepository.getInstance(db)

        FireUtility.getLatestUserData {
            if (it.isSuccessful && it.result.exists()) {
                val user = it.result.toObject(User::class.java)!!
                viewModelScope.launch(Dispatchers.IO) {
                    insertCurrentUser(user)
                }
                getChannelUsers(user.chatChannels)
            }
        }

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

    fun updateUser(userId: String, changes: Map<String, Any?>, onComplete: (task: Task<Void>) -> Unit) {
        FireUtility.updateUser(userId, changes, onComplete)
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
    fun getFeedItems(query: Query): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProjectRemoteMediator(query, repo, true)
        ) {
            repo.projectDao.getPagedProjects()
        }.flow.cachedIn(viewModelScope)
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
    fun getPagedMessages(query: Query): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = MessageRemoteMediator(query, repo)
        ) {
            repo.messageDao.getPagedMessages()
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

        suspend fun onSender(project: Project, sender: User) {
            val existingCollaborations = sender.collaborations.toMutableList()
            existingCollaborations.add(projectRequest.projectId)

            var existingCollaborationsCount = sender.collaborationsCount
            existingCollaborationsCount += 1

            val existingProjectRequests = sender.projectRequests.toMutableList()
            existingProjectRequests.remove(projectRequest.projectId)

            val existingChatChannels = sender.chatChannels.toMutableList()
            existingChatChannels.add(project.chatChannel)

            repo.insertUser(sender)
        }

        suspend fun onProject(project: Project) {
            when (val res = FireUtility.acceptProjectRequest(project, projectRequest)) {
                is Result.Error -> {
                    Log.d(TAG, "Something went wrong while accepting project request.")
                    setCurrentError(res.exception)
                }
                is Result.Success -> {

                    TODO("Something is wrong here ///  basically the downloads need to be done later on")
                    val existingRequests = project.requests.toMutableList()
                    existingRequests.remove(projectRequest.requestId)

                    project.requests = existingRequests

                    val existingContributors = project.contributors.toMutableList()
                    existingContributors.add(projectRequest.senderId)

                    project.contributors = existingContributors

                    insertProject(project)

                    val chatChannel = repo.getChatChannel(project.chatChannel)

                    if (chatChannel != null) {
                        chatChannel.contributors = existingContributors
                        chatChannel.contributorsCount += 1
                        repo.insertChatChannels(listOf(chatChannel))
                    } else {
                        val ref = Firebase.firestore.collection("chatChannels")
                            .document(project.chatChannel)

                        when (val res1 = FireUtility.getDocument(ref)) {
                            is Result.Error -> {
                                Log.d(TAG, "Something went wrong while getting chat channel for this project .."  + res1.exception.localizedMessage)
                                setCurrentError(res1.exception)
                            }
                            is Result.Success -> {
                                val chatChannel1 = res1.data.toObject(ChatChannel::class.java)!!
                                repo.insertChatChannels(listOf(chatChannel1))
                            }
                        }
                    }

                    val sender = repo.getUser(projectRequest.senderId)

                    if (sender != null) {
                        onSender(project, sender)
                    } else {
                        val ref = Firebase.firestore.collection("users")
                            .document(projectRequest.senderId)

                        when (val result = FireUtility.getDocument(ref)) {
                            is Result.Error -> {
                                Log.d(TAG, "Something went wrong while getting request sender .."  + result.exception.localizedMessage)
                                setCurrentError(result.exception)
                            }
                            is Result.Success -> {
                                // already updated
                                val sender1 = result.data.toObject(User::class.java)!!
                                repo.insertUser(sender1)
                            }
                        }
                    }

                    repo.deleteProjectRequest(projectRequest)

                }
            }
        }

        val localProject = repo.getProject(projectRequest.projectId)
        if (localProject != null) {
            onProject(localProject)
        } else {
            val projectRef = Firebase.firestore.collection("projects")
                .document(projectRequest.projectId)

            when (val res = FireUtility.getDocument(projectRef)) {
                is Result.Error -> {
                    setCurrentError(res.exception)
                }
                is Result.Success -> {
                    val project = res.data.toObject(Project::class.java)!!
                    onProject(project)
                }
            }
        }
    }

    fun rejectRequest(projectRequest: ProjectRequest) = viewModelScope.launch (Dispatchers.IO) {
        when (val res = FireUtility.rejectRequest(projectRequest)) {
            is Result.Error -> {
                setCurrentError(res.exception)
            }
            is Result.Success -> {
                val project = repo.getProject(projectRequest.projectId)
                if (project != null) {
                    val existingRequests = project.requests.toMutableList()
                    existingRequests.remove(projectRequest.requestId)
                    project.requests = existingRequests

                    insertProject(project)
                }
            }
        }
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

    fun getProjectContributors(project: Project, onComplete: (task: Task<QuerySnapshot>) -> Unit) {
        FireUtility.getProjectContributors(project, onComplete)
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
        }.flow.cachedIn(viewModelScope)
    }

    fun onCommentLiked(comment: Comment) = viewModelScope.launch (Dispatchers.IO)  {
        repo.onCommentLiked(comment)
    }

    // to insert all the users related to the local projects
    fun getChannelUsers(chatChannels: List<String>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.getChannelUsers(chatChannels)) {
            is Result.Error -> {
                setCurrentError(result.exception)
            }
            is Result.Success -> {
                Log.d(TAG, result.data.toString())
                repo.insertUsers(result.data)
            }
        }
    }

    fun sendTextMessage(chatChannelId: String, content: String) = viewModelScope.launch (Dispatchers.IO) {
        val currentUser = currentUser.value!!
        when (val result = FireUtility.sendTextMessage(currentUser, chatChannelId, content)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {
                repo.insertMessages(listOf(result.data))
                val chatChannel = repo.getChatChannel(chatChannelId)
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
    fun sendMessagesSimultaneously(chatChannelId: String, listOfMessages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        when (val result = FireUtility.sendMessagesSimultaneously(chatChannelId, listOfMessages)) {
            is Result.Error -> setCurrentError(result.exception)
            is Result.Success -> {

                val messages = result.data
                repo.insertMessages(messages)

                val chatChannel = repo.getChatChannel(chatChannelId)

                if (chatChannel != null) {
                    chatChannel.lastMessage = messages.last()
                    chatChannel.updatedAt = messages.last().createdAt
                    repo.insertChatChannels(listOf(chatChannel))
                }

                setChatUploadImages(emptyList())
                setChatUploadDocuments(emptyList())
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

    fun insertMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        TODO()
    }

    fun updateMessage(message: Message) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateMessage(message)
    }

    fun updateMessages(messages: List<Message>) = viewModelScope.launch (Dispatchers.IO) {
        repo.updateMessages(messages)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

}