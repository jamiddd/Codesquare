package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.Post

abstract class SuperPostViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    abstract fun bind(mPost: Post?)
}