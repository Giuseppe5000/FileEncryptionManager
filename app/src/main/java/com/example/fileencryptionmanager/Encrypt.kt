package com.example.fileencryptionmanager

import java.security.Key
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec

class Encrypt {
    var salt: ByteArray? = null
    var iv: ByteArray? = null
    var encryptedData: ByteArray? = null

    companion object {
        //This annotation tells Java classes to treat this method as if it was a static to [KotlinClass]
        @JvmStatic
        val SALT_BYTES = 8
        val PBK_ITERATIONS = 1000
        val ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding"
        val PBE_ALGORITHM = "PBEwithSHA256and128BITAES-CBC-BC"
        var encDataStatic: ByteArray? = null

        @JvmStatic
        fun encrypt(password: String, data: ByteArray): Encrypt {
            val encData = Encrypt()
            val rnd = SecureRandom()
            encData.salt = ByteArray(SALT_BYTES)
            encData.iv = ByteArray(16) // AES block size
            rnd.nextBytes(encData.salt)
            rnd.nextBytes(encData.iv)
            val keySpec = PBEKeySpec(password.toCharArray(), encData.salt, PBK_ITERATIONS)
            val secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM)
            val key: Key = secretKeyFactory.generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            val ivSpec = IvParameterSpec(encData.iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            encData.encryptedData = cipher.doFinal(data)
            return encData
        }

        @JvmStatic
        fun decrypt(password: String, salt: ByteArray?, iv: ByteArray?, encryptedData: ByteArray?): ByteArray {
            val keySpec = PBEKeySpec(password.toCharArray(), salt, PBK_ITERATIONS)
            val secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM)
            val key: Key = secretKeyFactory.generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            return cipher.doFinal(encryptedData)
        }
    }

}

