package com.jamid.codesquare.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.ARCHIVE
import com.jamid.codesquare.USERS
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project

@ExperimentalPagingApi
class ArchiveFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(ARCHIVE)

        getItems {
            viewModel.getArchivedProjects(query)
        }

    }


    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

}