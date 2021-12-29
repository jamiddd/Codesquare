package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Project

@ExperimentalPagingApi
class ProjectRemoteMediator(query: Query, repo: MainRepository, private val shouldClear: Boolean = false): FirebaseRemoteMediator<Int, Project>(query, repo) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val projects = items.toObjects(Project::class.java)
        repository.insertProjects(projects.toTypedArray())
    }

    override suspend fun onRefresh() {
        if (shouldClear) {
            repository.clearProjects()
        }
    }
}