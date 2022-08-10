package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.MediaItem

interface HorizontalMediaItemClickListener {
    fun onMediaItemClick(mediaItem: MediaItem, pos: Int)
}