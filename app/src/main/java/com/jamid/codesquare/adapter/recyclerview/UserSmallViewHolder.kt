package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R
import com.jamid.codesquare.accentColor
import com.jamid.codesquare.convertDpToPx
import com.jamid.codesquare.data.User
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.updateLayout


class UserSmallViewHolder(val projectId: String, val chatChannelId: String, val view: View, private val administrators: List<String>): RecyclerView.ViewHolder(view) {

    var isGrid = false

    private val image: SimpleDraweeView = view.findViewById(R.id.user_img_small)
    private val name: TextView = view.findViewById(R.id.usr_name_small)
    private val tag: TextView = view.findViewById(R.id.user_tag)

    private val userClickListener = view.context as UserClickListener

    fun bind(user: User?) {
        if (user != null) {
            name.text = user.name

            image.setImageURI(user.photo)

            val blueColor = view.context.accentColor()

            if (administrators.contains(user.id)) {
                val roundingParams = RoundingParams.asCircle().setBorderColor(blueColor).setBorderWidth(
                    convertDpToPx(2, view.context).toFloat()).setPadding(convertDpToPx(4, view.context).toFloat())
                image.hierarchy = GenericDraweeHierarchyBuilder(view.context.resources)
                    .setRoundingParams(roundingParams)
                    .build()
            }

            tag.text = user.tag

            view.setOnClickListener {
                userClickListener.onUserClick(user)
            }

            if (isGrid) {
                view.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }

            view.setOnLongClickListener {
                userClickListener.onUserOptionClick(projectId, chatChannelId, it, user, administrators)
                true
            }
        }
    }

    companion object {

        fun newInstance(parent: ViewGroup, projectId: String, chatChannelId: String, administrators: List<String>)
            = UserSmallViewHolder(projectId, chatChannelId, LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent,false), administrators)

    }

}