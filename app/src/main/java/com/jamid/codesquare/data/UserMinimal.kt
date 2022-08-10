package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Parcelize
@Serializable
@Keep
@Entity(tableName = "user_minimal")
data class UserMinimal(
    @PrimaryKey
    var userId: String,
    var name: String,
    var photo: String,
    var username: String,
    var premiumState: Long
): Parcelable {
    constructor() : this(randomId(), "", "", "", -1)

    fun toUser(): User {
        return User.newUser(userId, name, email = "").also {
            it.photo = photo
            it.username = username
        }
    }

    @Exclude
    fun isEmpty() = userId.isBlank()
}