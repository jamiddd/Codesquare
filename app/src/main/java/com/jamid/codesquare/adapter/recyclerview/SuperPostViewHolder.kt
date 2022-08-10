package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.Post2

abstract class SuperPostViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    abstract fun bind(mPost: Post2?)
    init {
        Log.d("Something", "Simple: ")
    }
}