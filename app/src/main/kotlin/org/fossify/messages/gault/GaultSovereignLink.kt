package org.fossify.messages.gault

import android.content.Context
import android.content.Intent
import org.fossify.messages.services.GaultDataRelay

/**
 * Convenience API used by satellite features to interact with the OGN hub + mesh.
 *
 * Satellites MUST ensure harvested data is public and non-personal.
 */
class GaultSovereignLink(private val context: Context) {

    // 1. DATA HARVESTING (Public Only)
    fun harvestPublicData(dataType: String, dataContent: String) {
        val intent = Intent(GaultDataRelay.ACTION_DATA_HARVEST).apply {
            putExtra(GaultDataRelay.EXTRA_TYPE, dataType)
            putExtra(GaultDataRelay.EXTRA_CONTENT, dataContent)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    // 2. THE $5 GATEKEEPER
    fun checkMasterUnlock(): Boolean {
        val prefs = context.getSharedPreferences(GaultDataRelay.PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(GaultDataRelay.KEY_MASTER_UNLOCKED, false)
    }

    // 3. ZERO-CELL RELAY (Mesh Handshake)
    fun initiateMeshRelay(packet: ByteArray) {
        // Hook point: implement Nearby relay in your gplay flavor, or keep as a no-op
        // in foss/core flavors.
        //
        // For now, we forward to the relay service via intent so it's centralized.
        val intent = Intent(context, org.fossify.messages.services.GaultDataRelay::class.java).apply {
            action = ACTION_SEND_MESH_PACKET
            putExtra(EXTRA_MESH_PACKET, packet)
        }
        context.startService(intent)
    }

    companion object {
        const val ACTION_SEND_MESH_PACKET = "com.gault.network.SEND_MESH_PACKET"
        const val EXTRA_MESH_PACKET = "packet"
    }
}

