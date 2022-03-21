package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.ProjectInvite

@ExperimentalPagingApi
class ProjectInviteRemoteMediator(q: Query, private val repository: MainRepository): FirebaseRemoteMediator<Int, ProjectInvite>(q){

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val invites = items.toObjects(ProjectInvite::class.java).toTypedArray()
        repository.insertProjectInvites(invites)
    }

    override suspend fun onRefresh() {
        repository.clearProjectInvites()
    }

}