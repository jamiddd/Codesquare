package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.adapter.comparators.ReferenceItemComparator
import com.jamid.codesquare.data.ReferenceItem
import com.jamid.codesquare.data.Result

class PostAdapter2: PagingDataAdapter<ReferenceItem, PostViewHolder>(ReferenceItemComparator()) {
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        getItem(position)?.let {
            FireUtility.getPost(it.id) { it1 ->
                val res = it1 ?: return@getPost

                when (res) {
                    is Result.Error -> Log.e(TAG, "onBindViewHolder: ${res.exception.localizedMessage}")
                    is Result.Success -> {
                        holder.bind(res.data)
                    }
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder.newInstance(parent)
    }

    companion object {
        private const val TAG ="PostAdapter2"
    }

}