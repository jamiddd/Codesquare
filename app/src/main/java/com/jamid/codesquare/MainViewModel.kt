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

    fun insertUser(localUser: User) = viewModelScope.launch (Dispatchers.IO) {
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
                val changes = mapOf(
                    "projectsCount" to currentUser.projectsCount + 1,
                    "projects" to existingList
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

    fun insertProject(currentProject: Project) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertProjects(listOf(currentProject))
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

    fun onLikePressed(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onLikePressed(project)
    }

    fun onSavePressed(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onSavePressed(project)
    }

    fun onJoinProject(project: Project) = viewModelScope.launch (Dispatchers.IO) {
        repo.onJoinProject(project)
    }

}