package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.PostMinimal2

interface PostClickListener {
    fun onPostClick(post: Post)
    fun onPostClick(postMinimal2: PostMinimal2)
    fun onPostLikeClick(post: Post, onChange: (newPost: Post) -> Unit)
    fun onPostSaveClick(post: Post, onChange: (newPost: Post) -> Unit)
    fun onPostJoinClick(post: Post, onChange: (newPost: Post) -> Unit)
    fun onPostCreatorClick(post: Post)
    fun onPostCommentClick(post: Post)
    fun onPostOptionClick(post: Post)
    fun onPostOptionClick(postMinimal2: PostMinimal2)
    fun onPostUndoClick(post: Post, onChange: (newPost: Post) -> Unit)
    fun onPostContributorsClick(post: Post)
    fun onPostSupportersClick(post: Post)
    fun onPostNotFound(post: Post)
    fun onPostLoad(post: Post)
    fun onAdInfoClick()
    fun onAdError(post: Post)
    fun onPostLocationClick(post: Post)
    suspend fun onCheckForStaleData(post: Post, onUpdate: (newPost: Post) -> Unit)
    fun onPostUpdate(newPost: Post)

}