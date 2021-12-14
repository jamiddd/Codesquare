package com.jamid.codesquare.listeners

import android.view.View
import com.jamid.codesquare.data.SearchQuery

interface SearchItemClickListener {
    fun onSearchItemClick(searchQuery: SearchQuery)
    fun onSearchItemForwardClick(query: SearchQuery)
    fun onSearchOptionClick(view: View, query: SearchQuery)
}