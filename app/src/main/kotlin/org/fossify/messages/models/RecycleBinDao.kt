package org.gault.messages.daos

import androidx.room.*
import org.gault.messages.models.RecycleBinMessage

@Dao
interface RecycleBinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: RecycleBinMessage)

    /**
     * The Gault Vacuum: 
     * Purges P2P messages after a short burst (e.g., 5 mins)
     * Purges Legacy messages after the standard window (e.g., 30 days)
     */
    @Query("""
        DELETE FROM recycle_bin_messages 
        WHERE (is_p2p = 1 AND deleted_ts < :p2pThreshold) 
        OR (is_p2p = 0 AND deleted_ts < :legacyThreshold)
    """)
    fun vacuumBin(p2pThreshold: Long, legacyThreshold: Long)

    @Query("DELETE FROM recycle_bin_messages WHERE id = :id")
    fun deleteById(id: Long)

    @Query("SELECT * FROM recycle_bin_messages")
    fun getAll(): List<RecycleBinMessage>
}