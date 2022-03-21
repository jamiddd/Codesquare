package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Project

@ExperimentalPagingApi
class ProjectRemoteMediator(query: Query, private val repo: MainRepository, private val shouldClear: Boolean = false): FirebaseRemoteMediator<Int, Project>(query) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val projects = items.toObjects(Project::class.java)
        repo.insertProjects(projects.toTypedArray())
    }

    override suspend fun onRefresh() {
        if (shouldClear) {
            repo.clearProjects()
        }
    }
}