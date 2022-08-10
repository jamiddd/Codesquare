package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.PostWrapper
import com.jamid.codesquare.databinding.PostSelectItemBinding
import com.jamid.codesquare.listeners.PostSelectListener

class PostSelectionAdapter(val listener: PostSelectListener? = null): PagingDataAdapter<PostWrapper, PostSelectionAdapter.PostSelectViewHolder>(PostWrapper.comparator) {
    init {
        Log.d("Something", "Simple: ")
    }
    inner class PostSelectViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: PostSelectItemBinding
        private lateinit var postWrap: PostWrapper

        fun reset() {
            postWrap.isSelected = false
            binding.postSelectRadio.isChecked = false
        }

        fun bind(postWrapper: PostWrapper?) {

            if (postWrapper == null)
                return

            postWrap = postWrapper

            binding = PostSelectItemBinding.bind(view)
            val post = postWrap.post

            // TODO("Change it to thumbnail")
            binding.postSelectImg.setImageURI(post.mediaList.firstOrNull())

            binding.postSelectName.text = post.name

            binding.root.setOnClickListener {
                listener?.onPostSelectItemClick(postWrap, bindingAdapterPosition) {
                    bind(postWrap)
                }
            }

            binding.postSelectRadio.isChecked = postWrapper.isSelected
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostSelectViewHolder {
        return PostSelectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.post_select_item, parent, false))
    }

    override fun onBindViewHolder(holder: PostSelectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}