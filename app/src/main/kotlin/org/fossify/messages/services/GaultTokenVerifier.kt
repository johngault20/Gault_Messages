package org.fossify.messages.services

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Signed-token security for "Clear Tone" unlocks.
 *
 * The OGN hub should sign `payload` using its private key.
 * Satellites verify the signature here before unlocking.
 *
 * Populate [OGN_HUB_PUBLIC_KEY_B64_X509] with the hub's X.509 DER public key
 * (Base64, without PEM headers).
 */
object GaultTokenVerifier {
    private const val TAG = "GaultTokenVerifier"

    // IMPORTANT: set this to the Official Gault Network hub public key (X.509 DER, base64)
    private const val OGN_HUB_PUBLIC_KEY_B64_X509: String = ""

    // Match the hub signing algorithm. ECDSA is a good default for Android.
    private const val SIGNATURE_ALG = "SHA256withECDSA"

    fun verifyClearTone(payload: String, signatureB64: String): Boolean {
        if (OGN_HUB_PUBLIC_KEY_B64_X509.isBlank()) {
            // Fail-closed until a real hub key is configured.
            Log.w(TAG, "Hub public key not configured; refusing unlock")
            return false
        }

        return try {
            val pub = decodePublicKey(OGN_HUB_PUBLIC_KEY_B64_X509)
            val sigBytes = Base64.decode(signatureB64, Base64.DEFAULT)

            val verifier = Signature.getInstance(SIGNATURE_ALG)
            verifier.initVerify(pub)
            verifier.update(payload.toByteArray(Charsets.UTF_8))
            verifier.verify(sigBytes)
        } catch (t: Throwable) {
            false
        }
    }

    private fun decodePublicKey(keyB64X509: String): PublicKey {
        val keyBytes = Base64.decode(keyB64X509, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }
}

