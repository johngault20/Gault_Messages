package org.fossify.messages.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background service responsible for bringing up the BiglyBT-based
 * Gault Network node and joining the DHT using the #GaultProtocol
 * namespace.
 *
 * Actual BiglyBT core/DHT wiring should be implemented inside
 * [initialiseGaultNetwork] once the SDK APIs are available in this
 * project.
 */
class GaultNetworkService : Service() {

    private suspend fun broadcastToDht(text: String, address: String) {
        // Once SDK is linked: dht.put(address.toGaultHash(), text.encrypt())
        Log.d(TAG, "Broadcasting P2P message to $address: $text")
    }

    private fun handleIncomingMessage(sender: String, body: String) {
        // This "shouts" to the app that a new P2P message has arrived
        val intent = Intent("RECEIVE_P2P_MSG").apply {
            putExtra("SENDER_ADDRESS", sender)
            putExtra("MESSAGE_BODY", body)
            setPackage(packageName) // Security: only our app hears this
        }
        sendBroadcast(intent)
        Log.i(TAG, "P2P Message received from $sender and broadcasted to UI")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            initialiseGaultNetwork()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "SEND_P2P_MSG") {
            val text = intent.getStringExtra("EXTRA_DATA") ?: ""
            val address = intent.getStringExtra("EXTRA_ADDRESS") ?: ""
            
            scope.launch {
                broadcastToDht(text, address)
            }
        }
        return START_STICKY
    }

    private suspend fun initialiseGaultNetwork() {
        try {
            Log.i(TAG, "Initializing BiglyBT core for $GAULT_NAMESPACE")
            
            // This is where the Gault Protocol stabilizes the local node
            // Once the SDK is ready, we hook into the DHT listener:
            /*
            dht.addListener(object : DhtListener {
                override fun onDataReceived(sender: String, data: String) {
                    handleIncomingMessage(sender, data)
                }
            })
            */
            
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize Gault Network", t)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "GaultNetworkService"
        const val GAULT_NAMESPACE: String = "#GaultProtocol"
    }
}

