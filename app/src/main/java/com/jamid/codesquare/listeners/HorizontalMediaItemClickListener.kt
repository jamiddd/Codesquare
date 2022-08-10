package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.MediaItem
// something simple
interface HorizontalMediaItemClickListener {
    fun onMediaItemClick(mediaItem: MediaItem, pos: Int)
}