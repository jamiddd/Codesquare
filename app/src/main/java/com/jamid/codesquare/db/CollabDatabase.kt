package com.jamid.codesquare.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jamid.codesquare.COLLAB_DB
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.dao.*

@Database(
    entities = [InterestItem::class, ReferenceItem::class, LikedBy::class, Post::class, User::class, Interest::class, PostInvite::class, ChatChannel::class, Message::class, PostRequest::class, Comment::class, Notification::class, SearchQuery::class],
    version = 18
)
@TypeConverters(Converters::class)
abstract class CollabDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun chatChannelDao(): ChatChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun postRequestDao(): PostRequestDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun searchQueryDao(): SearchQueryDao
    abstract fun postInviteDao(): PostInviteDao
    abstract fun interestDao(): InterestDao
    abstract fun likedByDao(): LikedByDao
    abstract fun referenceItemDao(): ReferenceItemDao
    abstract fun interestItemDao(): InterestItemDao

    companion object {

        @Volatile
        private var instance: CollabDatabase? = null

        fun getInstance(context: Context): CollabDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context)
            }
        }

        private fun createDatabase(applicationContext: Context): CollabDatabase {
            return Room.databaseBuilder(
                applicationContext,
                CollabDatabase::class.java,
                COLLAB_DB
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }

}