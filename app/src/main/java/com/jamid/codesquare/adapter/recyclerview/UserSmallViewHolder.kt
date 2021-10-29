package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.common.callercontext.ContextChain
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.User
import com.jamid.codesquare.listeners.UserClickListener
import com.facebook.drawee.generic.RoundingParams
import com.jamid.codesquare.convertDpToPx
import com.jamid.codesquare.updateLayout


class UserSmallViewHolder(val view: View, private val administrators: List<String>): RecyclerView.ViewHolder(view) {

    var isGrid = false

    private val image: SimpleDraweeView = view.findViewById(R.id.user_img_small)
    private val name: TextView = view.findViewById(R.id.user_name_small)
    private val tag: TextView = view.findViewById(R.id.user_tag)

    private val userClickListener = view.context as UserClickListener

    fun bind(user: User?) {
        if (user != null) {
            name.text = user.name

            image.setImageURI(user.photo)

            val blueColor = ContextCompat.getColor(view.context, R.color.purple_500)

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
        }
    }

    companion object {

        fun newInstance(parent: ViewGroup, administrators: List<String>)
            = UserSmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent,false), administrators)

    }

}