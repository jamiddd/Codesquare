package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.SearchViewModel
import com.jamid.codesquare.adapter.comparators.PreviousQueryComparator
import com.jamid.codesquare.data.QUERY_TYPE_PROJECT
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.SearchItemClickListener

class SearchResultsAdapter(private val searchItemClickListener: SearchItemClickListener): ListAdapter<SearchQuery, SearchResultsAdapter.SearchResultViewHolder>(PreviousQueryComparator()){

    var shouldShowRightIcon = true

    inner class SearchResultViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val searchItemText = view.findViewById<TextView>(R.id.search_result_content)
        private val searchItemForwardBtn = view.findViewById<Button>(R.id.search_result_forward)

        fun bind(searchQuery: SearchQuery) {
            searchItemText.text = searchQuery.queryString

            view.setOnClickListener {
                searchItemClickListener.onSearchItemClick(searchQuery)
            }

            searchItemForwardBtn.isVisible = shouldShowRightIcon

           /* if (searchQuery.type == QUERY_TYPE_PROJECT) {
                val project = searchViewModel.recentProjectSearchCache[searchQuery.id]
                if (project != null) {
                    view.setOnClickListener {
                        searchItemClickListener.onSearchProjectClick(searchQuery, project)
                    }
                }
            } else {
                val user = searchViewModel.recentUserSearchCache[searchQuery.id]
                if (user != null) {
                    view.setOnClickListener {
                        searchItemClickListener.onSearchUserClick(searchQuery, user)
                    }
                }
            }*/

            searchItemForwardBtn.setOnClickListener {
                searchItemClickListener.onSearchItemForwardClick(searchQuery)
            }

            view.setOnLongClickListener {
                searchItemClickListener.onSearchOptionClick(it, searchQuery)
                true
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}