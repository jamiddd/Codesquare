package com.jamid.codesquare.ui

import androidx.paging.*
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.dao.PostDao
// something simple
private const val TAG = "PostPaging"

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator2(
    private val postDao: PostDao
) : RemoteMediator<String, Post>() {

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
        /*return if (lastUpdatedTime == null) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            val cacheTimeout = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
            if (System.currentTimeMillis() - lastUpdatedTime >= cacheTimeout) {
                // Cached data is up-to-date, so there is no need to re-fetch
                // from the network.
                InitializeAction.SKIP_INITIAL_REFRESH
            } else {
                // Need to refresh cached data from network; returning
                // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
                // APPEND and PREPEND from running until REFRESH succeeds.
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
        }*/
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<String, Post>
    ): MediatorResult {

        when (loadType) {
            LoadType.REFRESH -> {

                postDao.clearTable()

                return when (val itemsResult = FireUtility.getItems(null, state.config.initialLoadSize)) {
                    is Result.Error -> {
                        MediatorResult.Error(itemsResult.exception)
                    }
                    is Result.Success -> {
                        val posts = itemsResult.data
                        postDao.insert(posts)
                        MediatorResult.Success(posts.size < state.config.initialLoadSize)
                    }
                }
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(
                    true
                )
            }
            LoadType.APPEND -> {
                val key = state.anchorPosition?.let { state.closestPageToPosition(it)?.nextKey }

                return when (val itemsResult = FireUtility.getItems(key, state.config.pageSize)) {
                    is Result.Error -> {
                        MediatorResult.Error(itemsResult.exception)
                    }
                    is Result.Success -> {
                        val posts = itemsResult.data
                        postDao.insert(posts)
                        MediatorResult.Success(posts.size < state.config.pageSize)
                    }
                }
            }
        }
    }
}

class PostPagingSource(val postDao: PostDao) :
    PagingSource<String, Post>() {

    override fun getRefreshKey(state: PagingState<String, Post>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    suspend fun getItems(key: String?, limit: Int = 10): List<Post> {
        val now = System.currentTimeMillis()
        return if (key != null) {
            val lastPost = postDao.getPostById(key)
            if (lastPost != null) {
                postDao.getPosts(lastPost.createdAt, limit)
            } else {
                postDao.getPosts(now, limit)
            }
        } else {
            postDao.getPosts(now, limit)
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Post> {
        return try {
            val items = getItems(params.key, params.loadSize)
            LoadResult.Page(
                data = items,
                prevKey = null, // Only paging forward.
                nextKey = items.lastOrNull()?.id
            )
        } catch (e: Exception) {
            // Handle errors in this block and return LoadResult.Error if it is an
            // expected error (such as a network failure).
            LoadResult.Error(e)
        }
        /*
        return try {
            when (params) {
                is LoadParams.Refresh -> {
                    params.key  // initial key, or key from refresh key
                    val items = getItems(params.key)
                    val lastPostId = items.last().id
                    val nextK = "_divider_$lastPostId"
                    LoadResult.Page(items, null, nextK)
                }
                is LoadParams.Append -> {
                    // key will be _divider_{something}
                    val prevKey = params.key.substringAfterLast("_divider_", "") // "something"
                    val nextItems = getItems(params.key)
                    val nextK = nextItems.last().id // "somethingElse"
                    val key = prevKey + "_divider_" + nextK // {something_divider_somethingElse} will be the new key
                    LoadResult.Page(nextItems, null, key)
                }
                is LoadParams.Prepend -> {
                    // key wil be
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }*/
    }
}

data class PostPageKey(
    val id: String, // abcdef_ghijkl
    val firstPost: String?, // abcdef
    val firstPostTime: Long?, // ~~
    val lastPost: String?, // ghijkl
    val lastPostTime: Long?, // ~~
    val prevId: String?, // null
    val nextId: String? // mnopqr_stuvwx
)