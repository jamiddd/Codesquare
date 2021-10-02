package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.ProjectRequest

@ExperimentalPagingApi
class ProjectRequestRemoteMediator(query: Query, repo: MainRepository): FirebaseRemoteMediator<Int, ProjectRequest>(query, repo) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val projectRequests = items.toObjects(ProjectRequest::class.java)
        repository.insertProjectRequests(projectRequests)
    }

    override suspend fun onRefresh() {

    }
}