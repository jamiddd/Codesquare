package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.Metadata

interface DocumentClickListener {
    fun onDocumentClick(view: View, metadata: Metadata)
    fun onCloseBtnClick(view: View, metadata: Metadata, position: Int)
}