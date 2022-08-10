package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "liked_by")
@Parcelize
@Keep// something simple
data class LikedBy(
    @PrimaryKey
    val id: String,
    @Embedded(prefix = "liked_by_")
    val userMinimal: UserMinimal,
    val createdAt: Long
): Parcelable {
    constructor(): this("", UserMinimal(), 0)
}