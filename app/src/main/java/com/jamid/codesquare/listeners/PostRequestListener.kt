package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.PostRequest
// something simple
interface PostRequestListener {
    fun onPostRequestAccept(postRequest: PostRequest, onFailure: () -> Unit)
    fun onPostRequestCancel(postRequest: PostRequest)
    fun onPostRequestPostDeleted(postRequest: PostRequest)
    fun onPostRequestSenderDeleted(postRequest: PostRequest)
    fun updatePostRequest(newPostRequest: PostRequest)
    fun onPostRequestUndo(postRequest: PostRequest)
    fun onPostRequestClick(postRequest: PostRequest)
    fun onCheckForStaleData(postRequest: PostRequest)
}