package com.xyecoc.mail.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xyecoc.mail.data.model.*

@Database(
    entities = [MailItem::class, Folder::class, Tag::class, FilterItem::class, AliasAddress::class, FeedbackItem::class, UserAccount::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mailDao(): MailDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun filterDao(): FilterDao
    abstract fun aliasDao(): AliasDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xyecoc_mail.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
