package com.xyecoc.mail.data.local

import androidx.room.*
import com.xyecoc.mail.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MailDao {
    @Query("SELECT * FROM mails WHERE folder = :folder ORDER BY id DESC")
    fun getMailsByFolder(folder: String): Flow<List<MailItem>>

    @Query("SELECT * FROM mails WHERE id = :mailId")
    suspend fun getMailById(mailId: Long): MailItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMails(mails: List<MailItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMail(mail: MailItem)

    @Query("UPDATE mails SET read = :isRead WHERE id = :mailId")
    suspend fun updateReadStatus(mailId: Long, isRead: Boolean)

    @Query("UPDATE mails SET important = :isImportant WHERE id = :mailId")
    suspend fun updateImportantStatus(mailId: Long, isImportant: Boolean)

    @Query("UPDATE mails SET folder = :newFolder WHERE id = :mailId")
    suspend fun moveToFolder(mailId: Long, newFolder: String)

    @Query("DELETE FROM mails WHERE id = :mailId")
    suspend fun deleteMail(mailId: Long)

    @Query("DELETE FROM mails WHERE id IN (:mailIds)")
    suspend fun deleteMails(mailIds: List<Long>)

    @Query("DELETE FROM mails WHERE folder = :folder")
    suspend fun clearFolder(folder: String)

    @Query("SELECT COUNT(*) FROM mails WHERE folder = :folder AND read = 0")
    fun getUnreadCount(folder: String = "inbox"): Flow<Int>

    @Query("DELETE FROM mails WHERE folder = :folder AND id >= :minId AND id NOT IN (:fetchedIds)")
    suspend fun deleteMailsNotInList(folder: String, minId: Long, fetchedIds: List<Long>)

    @Query("DELETE FROM mails WHERE folder = :folder AND id >= :minId AND id <= :maxId AND id NOT IN (:fetchedIds)")
    suspend fun deleteMailsNotInListRange(folder: String, minId: Long, maxId: Long, fetchedIds: List<Long>)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<Folder>)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("DELETE FROM tags")
    suspend fun clearAll()
}

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters ORDER BY id DESC")
    fun getAllFilters(): Flow<List<FilterItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilters(filters: List<FilterItem>)

    @Query("DELETE FROM filters WHERE id = :filterId")
    suspend fun deleteFilter(filterId: Long)

    @Query("DELETE FROM filters")
    suspend fun clearAll()
}

@Dao
interface AliasDao {
    @Query("SELECT * FROM aliases ORDER BY email ASC")
    fun getAllAliases(): Flow<List<AliasAddress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<AliasAddress>)

    @Query("DELETE FROM aliases WHERE id = :aliasId")
    suspend fun deleteAlias(aliasId: Long)

    @Query("DELETE FROM aliases")
    suspend fun clearAll()
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedbacks ORDER BY id DESC")
    fun getAllFeedbacks(): Flow<List<FeedbackItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbacks(items: List<FeedbackItem>)

    @Query("DELETE FROM feedbacks WHERE id = :id")
    suspend fun deleteFeedback(id: Long)

    @Query("DELETE FROM feedbacks")
    suspend fun clearAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE email = :email")
    suspend fun getAccount(email: String): UserAccount?

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccount)

    @Query("DELETE FROM accounts WHERE email = :email")
    suspend fun deleteAccount(email: String)
}
