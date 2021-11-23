package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.hide

class NotificationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val notificationTime = view.findViewById<TextView>(R.id.notification_time)
    private val notificationTitle = view.findViewById<TextView>(R.id.notification_title)
    private val notificationBody = view.findViewById<TextView>(R.id.notification_body)
    private val notificationImg = view.findViewById<SimpleDraweeView>(R.id.notification_img)

    fun bind(notification: Notification?) {
        if (notification == null)
            return

        notificationTitle.text = notification.title
        notificationBody.text = notification.content

        notificationTime.text = getTextForTime(notification.createdAt)

        if (notification.clazz == "user") {
            // user based notification
            val userRef = Firebase.firestore.collection("users").document(notification.contextId)
            FireUtility.getDocument(userRef) {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val user = it.result.toObject(User::class.java)!!
                        notificationImg.setImageURI(user.photo)
                    }
                } else {
                    Log.e(TAG, "Something went wrong while fetching user data.")
                    view.hide()
                }
            }

        } else if (notification.clazz == "project") {
            // project based notification
            val projectRef = Firebase.firestore.collection("projects").document(notification.contextId)
            FireUtility.getDocument(projectRef) {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        val project = it.result.toObject(Project::class.java)!!
                        notificationImg.setImageURI(project.images.firstOrNull())
                    }
                } else {
                    Log.e(TAG, "Something went wrong while fetching project data.")
                    view.hide()
                }
            }
        } else if (notification.clazz == "comment") {
            Firebase.firestore.collectionGroup("comments")
                .whereEqualTo("commentId", notification.contextId)
                .limit(1)
                .get()
                .addOnSuccessListener {
                    if (!it.isEmpty) {
                        val comment = it.first().toObject(Comment::class.java)
                        val userRef = Firebase.firestore.collection("users").document(comment.senderId)
                        FireUtility.getDocument(userRef) { it1 ->
                            if (it1.isSuccessful) {
                                if (it1.result.exists()) {
                                    val user = it1.result.toObject(User::class.java)!!
                                    notificationImg.setImageURI(user.photo)
                                }
                            } else {
                                Log.e(TAG, "Something went wrong while fetching user data.")
                                view.hide()
                            }
                        }
                    } else {
                        Log.d(TAG, "Didn't get the comment")
                    }
                }.addOnFailureListener {
                    Log.e(TAG, "Something went wrong while fetching comment data. " + it.localizedMessage)
                }
        }

    }

    companion object {
        private const val TAG = "NotificationViewHolder"
    }

}