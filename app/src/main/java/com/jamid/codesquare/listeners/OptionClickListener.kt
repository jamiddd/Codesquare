package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.*
// something simple
interface OptionClickListener {
    fun onOptionClick(option: Option, user: User?, post: Post?, chatChannel: ChatChannel?, comment: Comment?, tag: String?, message: Message?)
}