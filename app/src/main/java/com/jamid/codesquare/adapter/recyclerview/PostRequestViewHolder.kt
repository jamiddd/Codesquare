package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.jamid.codesquare.*
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.RequestItemAltBinding
import com.jamid.codesquare.databinding.RequestItemBinding
import com.jamid.codesquare.listeners.NotificationItemClickListener
import com.jamid.codesquare.listeners.PostRequestListener

class PostRequestViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private lateinit var binding: ViewBinding
    private val projectRequestListener = view.context as PostRequestListener
    private val notificationItemClickListener = view.context as NotificationItemClickListener
    var isMyRequests: Boolean = false

    fun bind(postRequest: PostRequest?) {
        if (postRequest != null) {
            if (isMyRequests) {
                binding = RequestItemAltBinding.bind(view)
                bindForCurrentUser(binding as RequestItemAltBinding, postRequest)
            } else {
                binding = RequestItemBinding.bind(view)
                bindForOtherUsers(binding as RequestItemBinding, postRequest)

                FireUtility.getNotification(UserManager.currentUserId, postRequest.notificationId) {
                    val result = it ?:return@getNotification

                    when (result) {
                        is Result.Error -> {
                            Log.d(TAG, "bind: ${result.exception.localizedMessage}")
                        }
                        is Result.Success -> {
                            notificationItemClickListener.onNotificationRead(result.data)
                        }
                    }
                }

            }
        }
    }

    private fun bindForOtherUsers(binding: RequestItemBinding, postRequest: PostRequest) {
        binding.requestProgress.hide()
        binding.requestPrimaryAction.show()
        binding.requestSecondaryAction.show()
        binding.requestProjectName.text = postRequest.post.name
        binding.requestImg.setImageURI(postRequest.post.image)
        val content = postRequest.sender.name + " wants to join your project."
        binding.requestContent.text = content

        binding.requestPrimaryAction.setOnClickListener {
            onActionStarted(binding)
            projectRequestListener.onPostRequestAccept(postRequest) {
                onActionEnded(binding)
            }
        }

        binding.requestSecondaryAction.setOnClickListener {
            onActionStarted(binding)
            projectRequestListener.onPostRequestCancel(postRequest)
        }

        binding.requestTime.text = getTextForTime(postRequest.createdAt)

        binding.root.setOnClickListener {
            projectRequestListener.onPostRequestClick(postRequest)
        }
    }

    private fun onActionEnded(binding: RequestItemBinding) {
        binding.requestProgress.hide()
        binding.requestSecondaryAction.show()
        binding.requestPrimaryAction.show()
    }

    private fun onActionStarted(binding: RequestItemBinding) {
        binding.requestProgress.show()
        binding.requestSecondaryAction.disappear()
        binding.requestPrimaryAction.disappear()
    }

    private fun bindForCurrentUser(binding: RequestItemAltBinding, postRequest: PostRequest) {
        binding.requestProgress.hide()
        binding.requestContent.hide()
        binding.requestImg.setImageURI(postRequest.post.image)
        binding.requestProjectName.text = postRequest.post.name

        binding.requestPrimaryAction.apply {
            text = view.context.getText(R.string.undo)
            show()

            setOnClickListener {
                projectRequestListener.onPostRequestUndo(postRequest)
            }
        }

        binding.requestTime.text = getTextForTime(postRequest.createdAt)

        binding.root.setOnClickListener {
            projectRequestListener.onPostRequestClick(postRequest)
        }

    }

    companion object {

        fun newInstance(parent: ViewGroup, ism: Boolean): PostRequestViewHolder {
            return if (ism) {
                PostRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item_alt, parent, false)).apply {
                    isMyRequests = ism
                }
            } else {
                PostRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)).apply {
                    isMyRequests = ism
                }
            }
        }

    }

}