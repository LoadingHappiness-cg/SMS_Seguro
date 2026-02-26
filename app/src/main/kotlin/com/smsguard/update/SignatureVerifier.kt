package com.smsguard.update

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object SignatureVerifier {
    // Hardcoded Public Key (Example Ed25519 X.509 encoded public key)
    // In production, replace this with your actual public key
    private const val PUBLIC_KEY_BASE64 = "MCowBQYDK2VwAyEAGb9ECWmEzf6PssS6OdN7f9L2zYv6T6X9X9X9X9X9X9U="

    fun verify(data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(keyBytes)
            val kf = KeyFactory.getInstance("Ed25519")
            val publicKey: PublicKey = kf.generatePublic(spec)

            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
