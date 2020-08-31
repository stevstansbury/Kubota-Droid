//
//  KeyStoreService.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.security

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.spec.IvParameterSpec

@TargetApi(Build.VERSION_CODES.M)
object KeyStoreService {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    private val keyStore: KeyStore

    init {
        this.keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        this.keyStore.load(null)
    }

    open class CryptoData(val encryption: ByteArray, val iv: ByteArray)

    fun encrypt(alias: String, stringToEncrypt: String): CryptoData {
        val cipher = this.getCipherForEncryption(alias)
        val iv = cipher.iv
        val encryption = cipher.doFinal(stringToEncrypt.toByteArray(charset = Charsets.UTF_8))

        return CryptoData(encryption = encryption, iv = iv)
    }

    fun getCipherForEncryption(alias: String): Cipher {
        val secretKey = this.findOrCreateKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    fun decrypt(alias: String, encryptedText: String, ivString: String): String {
        val iv = ivString.toByteArray(charset = Charsets.UTF_8)
        val encryption = encryptedText.toByteArray(charset = Charsets.UTF_8)

        return this.decrypt(alias, encryption, iv)
    }

    fun decrypt(alias: String, cryptoData: CryptoData): String
            = this.decrypt(alias, cryptoData.encryption, cryptoData.iv)

    private fun decrypt(alias: String, encryption: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, this.getKey(alias), IvParameterSpec(iv))

        return String(cipher.doFinal(encryption), Charsets.UTF_8)
    }

    @Throws(KeyStoreException::class, CertificateException::class, NoSuchAlgorithmException::class, IOException::class)
    private fun keyExists(keyName: String): Boolean {
        val aliases = this.keyStore.aliases()

        while (aliases.hasMoreElements()) {
            if (keyName == aliases.nextElement()) {
                return true
            }
        }
        return false
    }

    @Throws(NoSuchProviderException::class
            , NoSuchAlgorithmException::class
            , InvalidAlgorithmParameterException::class
            , UnrecoverableKeyException::class
            , CertificateException::class
            , KeyStoreException::class
            , IOException::class)
    private fun findOrCreateKey(keyName: String): SecretKey {
        return if (keyExists(keyName)) {
            getKey(keyName)
        } else {
            generateSecretKeyForEncryption(keyName)
        }
    }

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, UnrecoverableKeyException::class)
    private fun getKey(keyName: String): SecretKey {
        return this.keyStore.getKey(keyName, null) as SecretKey
    }

    private fun generateSecretKeyForEncryption(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        val builder = KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

                // Require the user to authenticate with a fingerprint to authorize every use of the key.
                // It is set to false for now until individual manufacturers like Samsung and Asus, patches
                // their OS to support the native Android Api for fingerprint authentication.
                .setUserAuthenticationRequired(false)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
