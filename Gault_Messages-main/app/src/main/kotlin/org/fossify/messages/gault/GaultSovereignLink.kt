package org.fossify.messages.gault

import android.content.Context
import android.content.Intent

/**
 * Sovereign link to the Official Gault Network (OGN). Inject into all Gault apps.
 * Handles public data harvesting, the $5 gatekeeper (Movie Hub / Command Center unlock),
 * and zero-cell P2P mesh relay via Nearby Connections.
 */
class GaultSovereignLink(val context: Context) {

    /**
     * Collects public, non-personal data and sends it to the Official Gault Network
     * for mesh distribution. Only use for data relevant to the app's purpose; no PII.
     */
    fun harvestPublicData(dataType: String, dataContent: String) {
        val intent = Intent(ACTION_DATA_HARVEST).apply {
            putExtra(EXTRA_TYPE, dataType)
            putExtra(EXTRA_CONTENT, dataContent)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Checks whether the Master Unlock (subscription verified / Clear Tone received)
     * is active. When true, Movie Hub and Command Center UI should be unlocked.
     */
    fun checkMasterUnlock(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MASTER_UNLOCKED, false)
    }

    /**
     * Initiates mesh relay (Wi‑Fi Direct / Bluetooth hopping) so this device
     * acts as a "Gault Repeater" without cell service. Uses Nearby Connections API.
     */
    fun initiateMeshRelay(packet: ByteArray) {
        val intent = Intent(ACTION_MESH_RELAY).apply {
            putExtra(EXTRA_PACKET, packet)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    companion object {
        const val PREFS_NAME = "GaultNetworkPrefs"
        const val KEY_MASTER_UNLOCKED = "MASTER_UNLOCKED"

        const val ACTION_DATA_HARVEST = "com.gault.network.DATA_HARVEST"
        const val EXTRA_TYPE = "type"
        const val EXTRA_CONTENT = "content"

        const val ACTION_MESH_RELAY = "com.gault.network.MESH_RELAY"
        const val EXTRA_PACKET = "packet"
    }
}
