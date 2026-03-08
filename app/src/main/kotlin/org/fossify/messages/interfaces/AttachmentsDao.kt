package org.gault.messages.interfaces

import androidx.room.Dao
import androidx.room.Query
import org.gault.messages.models.Attachment

@Dao
interface AttachmentsDao {
    @Query("SELECT * FROM attachments")
    fun getAll(): List<Attachment>
}
