package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.ProjectRequest

@ExperimentalPagingApi
class ProjectRequestRemoteMediator(query: Query, private val repo: MainRepository): FirebaseRemoteMediator<Int, ProjectRequest>(query) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val projectRequests = items.toObjects(ProjectRequest::class.java).toTypedArray()
        repo.insertProjectRequests(projectRequests)
    }

    override suspend fun onRefresh() {

    }
}