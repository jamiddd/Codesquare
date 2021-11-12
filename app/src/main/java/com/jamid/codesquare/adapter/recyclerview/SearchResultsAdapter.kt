package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.SearchResult

class SearchResultsAdapter(): ListAdapter<SearchResult, SearchResultsAdapter.SearchResultViewHolder>(object: DiffUtil.ItemCallback<SearchResult>() {
    override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return oldItem == newItem
    }
}){

    inner class SearchResultViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val searchItemText = view.findViewById<TextView>(R.id.search_result_content)
        private val searchItemForwardBtn = view.findViewById<Button>(R.id.search_result_forward)

        fun bind(searchResult: SearchResult) {
            searchItemText.text = searchResult.title
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}