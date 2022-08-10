package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.PostMinimal2
import com.jamid.codesquare.data.User
// something simple
interface PostClickListener {
    fun onPostClick(post: Post) {}
    fun onPostClick(postMinimal2: PostMinimal2) {}
    fun onPostLikeClick(post: Post) {}
    fun onPostSaveClick(post: Post) {}
    fun onPostJoinClick(post: Post) {}
    fun onPostCreatorClick(post: Post) {}
    fun onPostCommentClick(post: Post) {}
    fun onPostOptionClick(post: Post, creator: User) {}
    fun onPostOptionClick(postMinimal2: PostMinimal2, creator: User) {}
    fun onPostContributorsClick(post: Post) {}
    fun onPostSupportersClick(post: Post) {}
    fun onPostNotFound(post: Post) {}
    fun onPostLoad(post: Post) {}
    fun onAdInfoClick() {}
    fun onAdError(post: Post) {}
    fun onPostLocationClick(post: Post) {}
    fun onPostUpdate(newPost: Post) {}
}