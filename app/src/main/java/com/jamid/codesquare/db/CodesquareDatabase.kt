package com.jamid.codesquare.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jamid.codesquare.CODESQUARE_DB
import com.jamid.codesquare.data.*
import com.jamid.codesquare.data.dao.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Project::class, User::class, ChatChannel::class, Message::class, ProjectRequest::class, Comment::class], version = 2)
@TypeConverters(Converters::class)
abstract class CodesquareDatabase: RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun userDao(): UserDao
    abstract fun chatChannelDao(): ChatChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun projectRequestDao(): ProjectRequestDao
    abstract fun commentDao(): CommentDao

    companion object {

        @Volatile private var instance: CodesquareDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): CodesquareDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context, scope)
            }
        }

        private fun createDatabase(applicationContext: Context, scope: CoroutineScope) : CodesquareDatabase {
            return Room.databaseBuilder(applicationContext, CodesquareDatabase::class.java, CODESQUARE_DB)
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private class DatabaseCallback(val scope: CoroutineScope): RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch (Dispatchers.IO) {
                instance?.apply {
                    // do something every time the app opens
                    projectDao().clearTable()
                }
            }
        }
    }
}