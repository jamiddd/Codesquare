package com.jamid.codesquare.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jamid.codesquare.CODESQUARE_DB
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.dao.*

@Database(
    entities = [Project::class, User::class, Interest::class, ProjectInvite::class, ChatChannel::class, Message::class, ProjectRequest::class, Comment::class, Notification::class, SearchQuery::class],
    version = 4
)
@TypeConverters(Converters::class)
abstract class CodesquareDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun userDao(): UserDao
    abstract fun chatChannelDao(): ChatChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun projectRequestDao(): ProjectRequestDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun searchQueryDao(): SearchQueryDao
    abstract fun projectInviteDao(): ProjectInviteDao
    abstract fun interestDao(): InterestDao

    companion object {

        @Volatile
        private var instance: CodesquareDatabase? = null

        fun getInstance(context: Context): CodesquareDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context)
            }
        }

        private fun createDatabase(applicationContext: Context): CodesquareDatabase {
            return Room.databaseBuilder(
                applicationContext,
                CodesquareDatabase::class.java,
                CODESQUARE_DB
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }

}