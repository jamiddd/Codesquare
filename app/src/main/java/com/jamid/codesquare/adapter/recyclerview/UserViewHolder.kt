package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.User
import com.jamid.codesquare.data.UserMinimal2
import com.jamid.codesquare.databinding.UserGridItemBinding
import com.jamid.codesquare.databinding.UserVerticalItemBinding
import com.jamid.codesquare.listeners.UserClickListener

class UserViewHolder(
    private val view: View,
    private val like: Boolean = false,
    private val small: Boolean = false,
    private val min: Boolean = false,
    private val vague: Boolean = false,
    private val grid: Boolean = false,
    private val associatedChatChannel: ChatChannel? = null,
    private val listener: UserClickListener? = null
): RecyclerView.ViewHolder(view) {
    init {
        Log.d("Something", "Simple: ")
    }

    private val userClickListener: UserClickListener get() {
        return listener ?: view.context as UserClickListener
    }
    private lateinit var img: SimpleDraweeView
    private lateinit var name: TextView
    private lateinit var tag: TextView
    private lateinit var about: TextView
    private var likeBtn: MaterialButton? = null

    private fun setUserLayout(user: User) {
        img.setImageURI(user.photo)
        name.text = user.name

        if (user.tag.isBlank()) {
            tag.hide()
        } else {
            tag.show()
            tag.text = user.tag.trim()
        }

        if (user.about.isBlank()) {
            about.hide()
        } else {
            if (min) {
                about.hide()
            } else {
                about.show()
                about.text = user.about.trim()
            }
        }

        if (like) {
            likeBtn?.show()
        } else {
            likeBtn?.hide()
        }

        Firebase.firestore.collection(USERS)
            .document(UserManager.currentUserId)
            .collection(LIKED_USERS)
            .document(user.id)
            .get()
            .addOnSuccessListener {

                user.isLiked = it.exists()

                likeBtn?.apply {
                    isSelected = user.isLiked

                    setOnClickListener {
                        userClickListener.onUserLikeClick(user.copy())
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "setUserLayout: ${it.localizedMessage}")
            }


        if (associatedChatChannel != null) {
            if (associatedChatChannel.administrators.contains(user.id)) {

                val accent = view.context.accentColor()

                val smallestLength = view.context.resources.getDimension(R.dimen.unit_len)
                val microPadding = smallestLength / 2

                val roundingParams = RoundingParams.asCircle()
                    .setBorderColor(accent)
                    .setBorderWidth(microPadding)
                    .setPadding(smallestLength)

                img.hierarchy = GenericDraweeHierarchyBuilder(view.context.resources)
                    .setRoundingParams(roundingParams)
                    .build()
            }

        }

    }

    private fun updateUi(user: User? = null) {
        if (user != null) {
            val transparent = ContextCompat.getColor(view.context, R.color.transparent)

            name.setBackgroundColor(transparent)
            tag.setBackgroundColor(transparent)
            about.setBackgroundColor(transparent)

            likeBtn?.enable()

            view.setOnClickListener {
                userClickListener.onUserClick(user)
            }

            view.setOnLongClickListener {
                userClickListener.onUserOptionClick(user)
                true
            }

            setUserLayout(user)

        } else {
            val gray = ContextCompat.getColor(view.context, R.color.normal_grey)

            // show loading Ui
            name.text = " "
            name.setBackgroundColor(gray)

            tag.text = " "
            tag.setBackgroundColor(gray)

            about.text = " "
            about.setBackgroundColor(gray)

            likeBtn?.disable()
        }
    }

    fun bind(user: User?) {

        if (user == null)
            return

        if (small) {
            val binding = UserGridItemBinding.bind(view)
            img = binding.userImgSmall
            name = binding.userNameSmall
            tag = binding.userTagSmall
            about = binding.userAboutSmall

            if (grid) {
                view.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }

        } else {
            val binding = UserVerticalItemBinding.bind(view)
            img = binding.userImg
            name = binding.userName
            tag = binding.userTag
            about = binding.userAbout
            likeBtn = binding.userLikeBtn
        }

        if (vague) {

            updateUi()

            FireUtility.getUser(user.id) { mUser ->
                if (mUser != null) {
                    updateUi(mUser)
                } else {
                    view.hide()
                }
            }
        } else {
            updateUi(user)
        }
    }

    fun bind(userMinimal: UserMinimal2) {
        val binding = UserVerticalItemBinding.bind(view)
        val transparent = ContextCompat.getColor(view.context, R.color.transparent)
        binding.userName.setBackgroundColor(transparent)
        binding.userTag.setBackgroundColor(transparent)
        binding.userAbout.setBackgroundColor(transparent)

        binding.userImg.setImageURI(userMinimal.photo)
        binding.userName.text = userMinimal.name

        if (userMinimal.about.isNotBlank()) {
            binding.userAbout.text = userMinimal.about.trim()
            binding.userAbout.show()
        } else {
            binding.userAbout.hide()
        }

        if (userMinimal.tag.isNotBlank()) {
            binding.userTag.text = userMinimal.tag.trim()
            binding.userTag.show()
        } else {
            binding.userTag.hide()
        }

        view.setOnClickListener {
            userClickListener.onUserClick(userMinimal.objectID)
        }

    }

    companion object {
        private const val TAG = "UserViewHolder"

        fun newInstance(parent: ViewGroup, @LayoutRes layoutId: Int): UserViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return UserViewHolder(v)
        }

    }
}