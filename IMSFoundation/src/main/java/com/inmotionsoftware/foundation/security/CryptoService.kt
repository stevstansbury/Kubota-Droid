//
//  CryptoService.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoServiceException : Exception {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(cause: Throwable): super(cause)
    constructor(message: String, cause: Throwable): super(message, cause)
}

/*
 * References:
 * http://stackoverflow.com/questions/8622367/what-are-best-practices-for-using-aes-encryption-in-android
 */
object CryptoService {

    private const val PROVIDER = "BC"
    private const val SALT_LENGTH = 20
    private const val IV_LENGTH = 16
    private const val PBE_ITERATION_COUNT = 100

    private const val RANDOM_ALGORITHM = "SHA1PRNG"
    private const val HASH_ALGORITHM = "SHA-512"
    private const val PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC"
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val SECRET_KEY_ALGORITHM = "AES"

    @Throws(CryptoServiceException::class)
    fun encrypt(secret: SecretKey, cleartext: String): String {
        try {
            val iv = generateIv()
            val ivHex = HexEncoder.toHex(iv)
            val ivspec = IvParameterSpec(iv)

            val encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER)
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec)
            val encryptedText = encryptionCipher.doFinal(cleartext.toByteArray(charset("UTF-8")))
            val encryptedHex = HexEncoder.toHex(encryptedText)

            return ivHex + encryptedHex

        } catch (e: Throwable) {
            throw CryptoServiceException("Unable to encrypt", e)
        }
    }

    @Throws(CryptoServiceException::class)
    fun decrypt(secret: SecretKey, encrypted: String): String {
        try {
            val decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER)
            val ivHex = encrypted.substring(0, IV_LENGTH * 2)
            val encryptedHex = encrypted.substring(IV_LENGTH * 2)
            val ivspec = IvParameterSpec(HexEncoder.toBytes(ivHex))
            decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec)
            val decryptedText = decryptionCipher.doFinal(HexEncoder.toBytes(encryptedHex))
            return String(decryptedText)
        } catch (e: Throwable) {
            throw CryptoServiceException("Unable to decrypt", e)
        }
    }

    @Throws(CryptoServiceException::class)
    fun getSecretKey(password: String, salt: String): SecretKey {
        try {
            val pbeKeySpec = PBEKeySpec(password.toCharArray(), HexEncoder.toBytes(salt), PBE_ITERATION_COUNT, 256)
            val factory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER)
            val tmp = factory.generateSecret(pbeKeySpec)
            return SecretKeySpec(tmp.encoded, SECRET_KEY_ALGORITHM)
        } catch (e: Throwable) {
            throw CryptoServiceException("Unable to get secret key", e)
        }
    }

    @Throws(CryptoServiceException::class)
    fun getHash(password: String, salt: String): String {
        try {
            val input = password + salt
            val md = MessageDigest.getInstance(HASH_ALGORITHM, PROVIDER)
            val out = md.digest(input.toByteArray(charset("UTF-8")))
            return HexEncoder.toHex(out)
        } catch (e: Throwable) {
            throw CryptoServiceException("Unable to get hash", e)
        }
    }

    @Throws(CryptoServiceException::class)
    fun generateSalt(): String {
        try {
            val random = SecureRandom.getInstance(RANDOM_ALGORITHM)
            val salt = ByteArray(SALT_LENGTH)
            random.nextBytes(salt)
            return HexEncoder.toHex(salt)
        } catch (e: Throwable) {
            throw CryptoServiceException("Unable to generate salt", e)
        }
    }

    @Throws(CryptoServiceException::class)
    private fun generateIv(): ByteArray {
        try {
            val random = SecureRandom.getInstance(RANDOM_ALGORITHM)
            val iv = ByteArray(IV_LENGTH)
            random.nextBytes(iv)
            return iv
        } catch (e: NoSuchAlgorithmException) {
            throw CryptoServiceException(e)
        }
    }

}
