package com.jamid.codesquare

import android.app.Application
import android.location.Address
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.*
import com.jamid.codesquare.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
                    setCurrentError(res.exception)
                }
                is Result.Success -> {
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

}