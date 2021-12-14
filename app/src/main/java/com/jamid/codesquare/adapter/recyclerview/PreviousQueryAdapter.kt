package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.PreviousQueryComparator
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.SearchResultItemBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class PreviousQueryAdapter(
    private val searchItemClickListener: SearchItemClickListener
): ListAdapter<SearchQuery, PreviousQueryAdapter.PreviousQueryViewHolder>(PreviousQueryComparator()){

    inner class PreviousQueryViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        fun bind(query: SearchQuery) {
            val binding = SearchResultItemBinding.bind(view)

            binding.searchResultContent.text = query.queryString

            binding.searchResultForward.setOnClickListener {
                searchItemClickListener.onSearchItemForwardClick(query)
            }

            view.setOnClickListener {
                searchItemClickListener.onSearchItemClick(query)
            }

            view.setOnClickListener {
                searchItemClickListener.onSearchOptionClick(it, query)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousQueryViewHolder {
        return PreviousQueryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false))
    }

    override fun onBindViewHolder(holder: PreviousQueryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}