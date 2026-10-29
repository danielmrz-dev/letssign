package br.com.fiap.letssign.core.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de senha com PBKDF2 + salt aleatório. A senha nunca é salva em texto puro.
 * Formato salvo: "<algoritmo>:<iterações>:<hash em hex>".
 */
class PasswordHasher(private val iterations: Int = 40_000) {

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(password: String, salt: String): String {
        val algorithm = preferredAlgorithm()
        val hash = derive(password, salt, algorithm, iterations)
        return "$algorithm:$iterations:${hash.toHex()}"
    }

    fun verify(password: String, salt: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3) return false
        val algorithm = parts[0]
        val rounds = parts[1].toIntOrNull() ?: return false
        val expected = parts[2].hexToBytes() ?: return false
        val actual = runCatching { derive(password, salt, algorithm, rounds) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: String, salt: String, algorithm: String, rounds: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt.hexToBytes() ?: salt.toByteArray(), rounds, 256)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** PBKDF2WithHmacSHA256 existe a partir do Android 8; antes disso usamos a variante SHA1. */
    private fun preferredAlgorithm(): String = try {
        SecretKeyFactory.getInstance(SHA256)
        SHA256
    } catch (e: NoSuchAlgorithmException) {
        SHA1
    }

    private companion object {
        const val SHA256 = "PBKDF2WithHmacSHA256"
        const val SHA1 = "PBKDF2WithHmacSHA1"
    }
}

fun ByteArray.toHex(): String {
    val chars = "0123456789abcdef"
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(chars[v ushr 4]).append(chars[v and 0x0F])
    }
    return sb.toString()
}

fun String.hexToBytes(): ByteArray? {
    if (length % 2 != 0) return null
    val out = ByteArray(length / 2)
    for (i in out.indices) {
        val hi = Character.digit(this[i * 2], 16)
        val lo = Character.digit(this[i * 2 + 1], 16)
        if (hi < 0 || lo < 0) return null
        out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
}
