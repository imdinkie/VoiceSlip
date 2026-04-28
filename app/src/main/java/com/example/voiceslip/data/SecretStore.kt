package com.example.voiceslip.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("voiceslip_secrets", Context.MODE_PRIVATE)

    fun saveApiKey(provider: ProviderId, key: String) {
        val clean = key.trim()
        if (clean.isBlank()) {
            deleteApiKey(provider)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        val prefix = provider.prefPrefix()
        prefs.edit()
            .putString("${prefix}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${prefix}_value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun getApiKey(provider: ProviderId): String? {
        val prefix = provider.prefPrefix()
        val iv = prefs.getString("${prefix}_iv", null) ?: return null
        val value = prefs.getString("${prefix}_value", null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            String(cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun deleteApiKey(provider: ProviderId) {
        val prefix = provider.prefPrefix()
        prefs.edit().remove("${prefix}_iv").remove("${prefix}_value").apply()
    }

    fun saveMistralApiKey(key: String) {
        saveApiKey(ProviderId.MISTRAL, key)
    }

    fun getMistralApiKey(): String? {
        return getApiKey(ProviderId.MISTRAL)
    }

    fun deleteMistralApiKey() {
        deleteApiKey(ProviderId.MISTRAL)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "voiceslip_mistral_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

private fun ProviderId.prefPrefix(): String = when (this) {
    ProviderId.MISTRAL -> "mistral_key"
    ProviderId.GROQ -> "groq_key"
    ProviderId.OPENROUTER -> "openrouter_key"
}
