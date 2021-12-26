package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.ProjectInvite

@Dao
abstract class ProjectInviteDao: BaseDao<ProjectInvite>() {

    @Query("SELECT * FROM project_invites ORDER BY createdAt DESC")
    abstract fun getProjectInvites(): PagingSource<Int, ProjectInvite>

}