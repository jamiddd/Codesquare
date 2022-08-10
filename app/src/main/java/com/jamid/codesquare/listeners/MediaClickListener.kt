package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
// something simple
interface MediaClickListener {

    fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int)
    fun onMediaMessageItemClick(message: Message)

   /* fun onMediaDocumentClick(mediaItemWrapper: MediaItemWrapper, pos: Int)
    fun onMediaDocumentLongClick(mediaItemWrapper: MediaItemWrapper, pos: Int)*/

    fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int)

}