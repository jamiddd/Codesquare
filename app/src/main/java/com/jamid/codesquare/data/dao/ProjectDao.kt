package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.Project

@Dao
abstract class ProjectDao: BaseDao<Project>() {

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    abstract fun getPagedProjects(): PagingSource<Int, Project>

    @Query("DELETE FROM projects")
    abstract suspend fun clearTable()

    @Query("SELECT * FROM projects WHERE isMadeByMe = 1")
    abstract fun getCurrentUserPagedProjects(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    abstract suspend fun getProject(projectId: String): Project?

    @Query("SELECT * FROM projects WHERE contributors LIKE :formattedUid AND project_userId != :uid ORDER BY createdAt DESC")
    abstract fun getPagedCollaborations(formattedUid: String, uid: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE project_userId = :id ORDER BY createdAt DESC")
    abstract fun getPagedOtherUserProjects(id: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE contributors LIKE :formattedUid AND project_userId != :id ORDER BY createdAt DESC")
    abstract fun getOtherUserPagedCollaborations(formattedUid: String, id: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE isSaved = 1 ORDER BY createdAt DESC")
    abstract fun getPagedSavedProjects(): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE chatChannel = :channelId LIMIT 1")
    abstract suspend fun getProjectByChatChannel(channelId: String): Project?

    @Query("SELECT * FROM projects WHERE chatChannel = :channelId LIMIT 1")
    abstract fun getLiveProjectByChatChannel(channelId: String): LiveData<Project>

    @Query("SELECT * FROM projects WHERE tags LIKE :tag ORDER BY createdAt DESC")
    abstract fun getTagProjects(tag: String): PagingSource<Int, Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract fun getLiveProjectById(id: String): LiveData<Project>

    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteProjectById(id: String)

}