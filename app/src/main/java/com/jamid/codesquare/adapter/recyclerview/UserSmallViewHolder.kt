package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.User
import com.jamid.codesquare.listeners.UserClickListener

class UserSmallViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val image: SimpleDraweeView = view.findViewById(R.id.user_img_small)
    private val name: TextView = view.findViewById(R.id.user_name_small)
    private val tag: TextView = view.findViewById(R.id.user_tag)

    private val userClickListener = view.context as UserClickListener

    fun bind(user: User?) {
        if (user != null) {
            name.text = user.name

            image.setImageURI(user.photo)

            tag.text = user.tag

            view.setOnClickListener {
                userClickListener.onUserClick(user)
            }
        }
    }

    companion object {

        fun newInstance(parent: ViewGroup)
            = UserSmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent,false))

    }

}