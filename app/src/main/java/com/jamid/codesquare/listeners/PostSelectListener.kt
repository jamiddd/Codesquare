package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.PostWrapper

interface PostSelectListener {
    fun onPostSelectItemClick(postWrapper: PostWrapper, position: Int, onChange: (newPostWrapper: PostWrapper) -> Unit)
}