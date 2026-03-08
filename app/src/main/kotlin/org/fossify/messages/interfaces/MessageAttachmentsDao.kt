package org.gault.messages.interfaces

import androidx.room.Dao
import androidx.room.Query
import org.gault.messages.models.MessageAttachment

@Dao
interface MessageAttachmentsDao {
    @Query("SELECT * FROM message_attachments")
    fun getAll(): List<MessageAttachment>
}
