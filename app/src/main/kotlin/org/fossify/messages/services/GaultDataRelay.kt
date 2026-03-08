package org.fossify.messages.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GaultDataRelay (Satellite Role)
 *
 * - Collects app-provided public (non-personal) data
 * - Forwards it to the Official Gault Network (OGN) hub via local socket
 * - Waits for a "Clear Tone" (subscription verified) from the hub
 * - If verified, flips MASTER_UNLOCKED for Movie Hub / Command Center gating
 *
 * Nearby mesh relay is exposed via [GaultSovereignLink] and can be implemented
 * per-flavor (gplay) or via reflection without hard dependency.
 */
class GaultDataRelay : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private val running = AtomicBoolean(false)
    private var socketThread: Thread? = null

    private val harvestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_DATA_HARVEST) return

            val type = intent.getStringExtra(EXTRA_TYPE) ?: return
            val content = intent.getStringExtra(EXTRA_CONTENT) ?: return

            // Satellite apps are responsible for ensuring this is public/non-personal.
            enqueueToHub(type, content)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(harvestReceiver, IntentFilter(ACTION_DATA_HARVEST))
        startSocketLoop()
    }

    override fun onDestroy() {
        unregisterReceiver(harvestReceiver)
        running.set(false)
        socketThread?.interrupt()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == org.fossify.messages.gault.GaultSovereignLink.ACTION_SEND_MESH_PACKET) {
            // Mesh relay hook point (Nearby). Implement in a per-flavor component.
            // We keep the entrypoint centralized even if the actual relay differs by build variant.
            val packet = intent.getByteArrayExtra(org.fossify.messages.gault.GaultSovereignLink.EXTRA_MESH_PACKET)
            if (packet != null) {
                // TODO: Nearby relay implementation (gplay) should send this packet P2P.
                Log.d(TAG, "Mesh packet queued (${packet.size} bytes)")
            }
        }

        // Keep relay alive for hub handshake + mesh relay.
        return START_STICKY
    }

    private fun startSocketLoop() {
        if (!running.compareAndSet(false, true)) return

        socketThread = Thread({
            while (running.get()) {
                try {
                    LocalSocket().use { socket ->
                        socket.connect(
                            LocalSocketAddress(
                                OGN_LOCAL_SOCKET_NAME,
                                LocalSocketAddress.Namespace.ABSTRACT
                            )
                        )

                        BufferedWriter(OutputStreamWriter(socket.outputStream)).use { out ->
                            BufferedReader(InputStreamReader(socket.inputStream)).use { input ->
                                Log.d(TAG, "Connected to OGN hub socket: $OGN_LOCAL_SOCKET_NAME")

                                // Read hub messages line-by-line (JSONL).
                                var line: String?
                                while (running.get() && input.readLine().also { line = it } != null) {
                                    handleHubLine(line!!.trim())
                                }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    // Hub may not be running yet. Back off and retry.
                    try {
                        Thread.sleep(RECONNECT_BACKOFF_MS)
                    } catch (_: InterruptedException) {
                        // allow shutdown
                    }
                }
            }
        }, "GaultDataRelaySocket").apply { start() }
    }

    private fun enqueueToHub(type: String, content: String) {
        // Best-effort: write a single message by connecting briefly.
        Thread({
            try {
                val msg = JSONObject().apply {
                    put("op", "DATA")
                    put("app", packageName)
                    put("type", type)
                    put("content", content)
                    put("ts", System.currentTimeMillis())
                }.toString()

                LocalSocket().use { socket ->
                    socket.connect(
                        LocalSocketAddress(
                            OGN_LOCAL_SOCKET_NAME,
                            LocalSocketAddress.Namespace.ABSTRACT
                        )
                    )
                    BufferedWriter(OutputStreamWriter(socket.outputStream)).use { out ->
                        out.write(msg)
                        out.write("\n")
                        out.flush()
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed forwarding data to OGN hub: ${t.message}")
            }
        }, "GaultDataRelaySend").start()
    }

    private fun handleHubLine(line: String) {
        if (line.isBlank()) return

        // Allow the hub to send simple sentinel messages.
        if (line == CLEAR_TONE_SENTINEL) {
            setMasterUnlocked()
            return
        }

        // Preferred: JSON with signed "Clear Tone"
        try {
            val obj = JSONObject(line)
            val op = obj.optString("op")
            if (op != "CLEAR_TONE") return

            val payload = obj.optString("payload")
            val signatureB64 = obj.optString("sig")
            if (payload.isBlank() || signatureB64.isBlank()) {
                Log.w(TAG, "Clear Tone missing payload/signature; ignoring")
                return
            }

            if (!GaultTokenVerifier.verifyClearTone(payload, signatureB64)) {
                Log.w(TAG, "Clear Tone signature invalid; ignoring")
                return
            }

            setMasterUnlocked()
        } catch (_: Throwable) {
            // ignore malformed lines
        }
    }

    private fun setMasterUnlocked() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MASTER_UNLOCKED, true)
            .apply()

        sendBroadcast(Intent(ACTION_CLEAR_TONE_VERIFIED).setPackage(packageName))
        Log.d(TAG, "Clear Tone verified -> MASTER_UNLOCKED = true")
    }

    companion object {
        private const val TAG = "GaultDataRelay"

        // Broadcast contract for satellite apps
        const val ACTION_DATA_HARVEST = "com.gault.network.DATA_HARVEST"
        const val EXTRA_TYPE = "type"
        const val EXTRA_CONTENT = "content"

        // Hub -> satellite unlock signal
        const val ACTION_CLEAR_TONE_VERIFIED = "com.gault.network.CLEAR_TONE_VERIFIED"

        // Local socket hub endpoint (Official Gault Network)
        const val OGN_LOCAL_SOCKET_NAME = "gault_ogn_hub"
        private const val RECONNECT_BACKOFF_MS = 2_000L

        // Simple sentinel supported for early bring-up
        private const val CLEAR_TONE_SENTINEL = "CLEAR_TONE"

        // UI gatekeeper storage
        const val PREFS = "GaultNetworkPrefs"
        const val KEY_MASTER_UNLOCKED = "MASTER_UNLOCKED"
    }
}

