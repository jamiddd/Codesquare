package com.jamid.codesquare.db

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jamid.codesquare.data.Message

class MessagePagingSource(private val repository: MainRepository, private var chatChannelId: String): PagingSource<Long, Message>() {

    override fun getRefreshKey(state: PagingState<Long, Message>): Long? {
        return state.anchorPosition?.let {
            state.closestItemToPosition(it)?.createdAt
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Message> {
        return try {
            val key = params.key
            return if (key == null) {
                // refresh
                val now = System.currentTimeMillis()
                val messages = repository.getMessagesBefore(chatChannelId, now, params.loadSize)
                LoadResult.Page(messages, messages.first().createdAt, messages.last().createdAt)
            } else {
                // append
                val messages = repository.getMessagesBefore(chatChannelId, key, params.loadSize)
                LoadResult.Page(messages, messages.first().createdAt, messages.last().createdAt)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}