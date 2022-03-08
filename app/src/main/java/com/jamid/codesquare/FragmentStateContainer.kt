package com.jamid.codesquare

import android.content.Context
import android.graphics.Bitmap
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.User

/*
class FragmentStateContainer(context: Context) {


    // Project fragment

    data class Za(val projectId: String, var users: List<User> = emptyList(), var lastComment: Comment? = null, var chatChannel: ChatChannel? = null)

    private val states = mutableMapOf<String, Za>()
    private val states1 = mutableMapOf<String, Zb>()

    fun getProjectFragmentData(projectId: String): Za? {
        return states[projectId]
    }

    fun addProjectFragmentData(projectId: String, users: List<User> = emptyList(), comment: Comment? = null, chatChannel: ChatChannel? = null) {
        if (states.containsKey(projectId)) {
            val existingData = states[projectId]!!
            if (!users.isNullOrEmpty()) {
                existingData.users = users
            }

            if (comment != null) {
                existingData.lastComment = comment
            }

            if (chatChannel != null) {
                existingData.chatChannel = chatChannel
            }

            states[projectId] = existingData

        } else {
            val newData = Za(projectId, users, comment, chatChannel)
            states[projectId] = (newData)
        }
    }

    fun clearProjectFragmentData(projectId: String) {
        states.remove(projectId)
    }

    /////////////////////////////////////////////////////


    // Chat fragment
    data class Zb(
        val chatChannelId: String,
        var chatChannelImage: String = "",
        var scrollPosition: Int = 0,
        var smallImage: Bitmap? = null,
        var shouldShowKeyboard: Boolean = true,
        var shouldShowRefresher: Boolean = true
    )

    fun addChatFragmentData(chatChannelId: String, chatChannelImage: String = "", p: Int = 0, i: Bitmap? = null, shouldShowKeyboard: Boolean = true, shouldShowRefresher: Boolean = true) {
        if (states1.containsKey(chatChannelId)) {
            val existingData = states1[chatChannelId]!!
            if (chatChannelImage.isNotBlank()) {
                existingData.chatChannelImage = chatChannelImage
            }

            if (p != 0) {
                existingData.scrollPosition = p
            }

            if (i != null) {
                existingData.smallImage = i
            }

            existingData.shouldShowKeyboard = shouldShowKeyboard
            existingData.shouldShowRefresher = shouldShowKeyboard

            states1[chatChannelId] = existingData
        } else {
            val newData = Zb(chatChannelId, chatChannelImage, p, i)
            states1[chatChannelId] = newData
        }
    }

    fun getChatChannelFragmentData(chatChannelId: String): Zb? {
        return states1[chatChannelId]
    }

    fun clearChatFragmentData(chatChannelId: String) {
        states1.remove(chatChannelId)
    }

}*/
