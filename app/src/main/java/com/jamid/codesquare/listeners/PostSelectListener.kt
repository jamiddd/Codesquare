package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.PostWrapper
// something simple
interface PostSelectListener {
    fun onPostSelectItemClick(postWrapper: PostWrapper, position: Int, onChange: (newPostWrapper: PostWrapper) -> Unit)
}