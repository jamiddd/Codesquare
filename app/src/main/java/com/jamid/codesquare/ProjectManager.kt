package com.jamid.codesquare

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.data.Result

@Suppress("ObjectPropertyName")
object ProjectManager {

    private val _myProjectRequests = MutableLiveData<List<ProjectRequest>>()
    val myProjectRequests: LiveData<List<ProjectRequest>> = _myProjectRequests

    private suspend fun processProjectRequests(requests: List<ProjectRequest>): List<ProjectRequest> {

        val finalProjectRequestList = mutableListOf<ProjectRequest>()

        for (request in requests) {
            when (val res1 = FireUtility.getProject(request.projectId)) {
                is Result.Error -> {
                    Log.e(TAG, res1.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    val project = res1.data
                    request.project = project
                }
                null -> {
                    Log.d(TAG, "Probably project doesn't exist with project id: ${request.projectId}")
                }
            }

            when (val res2 = FireUtility.getUser(request.senderId)) {
                is Result.Error -> {
                    Log.e(TAG, res2.exception.localizedMessage.orEmpty())
                }
                is Result.Success -> {
                    val user = res2.data
                    request.sender = user
                }
                null -> {
                    Log.d(TAG, "Probably user doesn't exist with user id: ${request.senderId}")
                }
            }

            finalProjectRequestList.add(request)

        }

        return finalProjectRequestList

    }

    suspend fun setMyProjectRequests(projectRequests: List<ProjectRequest>) {
        val newList = processProjectRequests(projectRequests)
        _myProjectRequests.postValue(newList)
    }

    private const val TAG = "ProjectManager"

}