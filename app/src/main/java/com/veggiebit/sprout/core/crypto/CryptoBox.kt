package com.veggiebit.sprout.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the cloud-engine API keys at rest with an Android Keystore-backed AES-256-GCM key —
 * the key material never leaves hardware-backed storage (on devices with StrongBox/TEE) and
 * isn't extractable even with root. `androidx.security:security-crypto` (EncryptedSharedPreferences)
 * is deliberately not used — it's deprecated with no replacement and this needs only a single
 * key, not a full preferences wrapper.
 *
 * Ciphertext is stored as `ENC1:<base64 iv>:<base64 ciphertext>`. [decrypt] returns null (never
 * throws) on any failure — corrupted data, a missing/invalidated key after a factory-reset-
 * adjacent OS event — so callers can fall back to treating the field as unset rather than crash.
 */
object CryptoBox {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "sprout_api_key_secret"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    const val PREFIX = "ENC1:"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // background service must encrypt/decrypt unattended
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Encrypts [plainText]; returns it unchanged if blank (nothing to protect). */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            "$PREFIX$ivB64:$cipherB64"
        } catch (_: Exception) {
            // If encryption is unavailable for some reason, fail safe by not persisting a
            // silently-broken value — caller keeps whatever it already had.
            plainText
        }
    }

    /** Decrypts a value previously produced by [encrypt]. Returns the input unchanged if it
     * isn't in the expected format (legacy plaintext, pre-migration), or null if it looks like
     * ciphertext but can't be decrypted (corrupted / key no longer available). */
    fun decrypt(stored: String): String? {
        if (stored.isBlank()) return stored
        if (!stored.startsWith(PREFIX)) return stored // legacy plaintext, not yet migrated

        return try {
            val payload = stored.removePrefix(PREFIX)
            val parts = payload.split(":", limit = 2)
            if (parts.size != 2) return null

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)
}
