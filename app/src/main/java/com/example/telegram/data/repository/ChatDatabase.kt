package com.example.telegram.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.telegram.data.local.MessageDao
import com.example.telegram.data.local.MessageEntity

@Database(entities = [MessageEntity::class], version = 4)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration(false)  // Add this line
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}