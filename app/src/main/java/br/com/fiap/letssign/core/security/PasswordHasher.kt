package br.com.fiap.letssign.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Encapsula o resultado de hashing de uma senha com o hash e o salt codificados em Base64.
 */
data class HashedPasswordResult(
    val hash: String,
    val salt: String
)

/**
 * Interface para gerenciar o hashing seguro de senhas.
 */
interface PasswordHasher {
    fun hashPassword(password: String): HashedPasswordResult
    fun hashPasswordWithSalt(password: String, saltBase64: String): String
    fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean
}

/**
 * Implementação usando PBKDF2WithHmacSHA256 com salt aleatório de 16 bytes e 120.000 iterações.
 */
class PasswordHasherImpl : PasswordHasher {

    companion object {
        private const val PRIMARY_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val ITERATIONS = 120000
        private const val KEY_LENGTH = 256
        private const val SALT_SIZE_BYTES = 16

        // Salt e hash fictícios para simulação em tempo constante quando o e-mail não for encontrado
        private const val DUMMY_SALT_BASE64 = "dHVtbXlfc2FsdF9mYWtlXzEyMw=="
        private const val DUMMY_HASH_BASE64 = "ZHVtbXlfaGFzaF9mYWtlXzEyMw=="
    }

    private val secureRandom = SecureRandom()

    override fun hashPassword(password: String): HashedPasswordResult {
        val saltBytes = ByteArray(SALT_SIZE_BYTES)
        secureRandom.nextBytes(saltBytes)
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

        val hashBase64 = hashPasswordWithSalt(password, saltBase64)
        return HashedPasswordResult(hash = hashBase64, salt = saltBase64)
    }

    override fun hashPasswordWithSalt(password: String, saltBase64: String): String {
        val saltBytes = try {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            ByteArray(SALT_SIZE_BYTES)
        }

        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = try {
            SecretKeyFactory.getInstance(PRIMARY_ALGORITHM)
        } catch (e: Exception) {
            SecretKeyFactory.getInstance(FALLBACK_ALGORITHM)
        }
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    override fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val calculatedHashBase64 = hashPasswordWithSalt(password, saltBase64)
        val calculatedBytes = calculatedHashBase64.toByteArray(Charsets.UTF_8)
        val expectedBytes = expectedHashBase64.toByteArray(Charsets.UTF_8)

        // Comparação em tempo constante para proteção contra timing attacks
        return MessageDigest.isEqual(calculatedBytes, expectedBytes)
    }

    /**
     * Executa o hash de senha com o salt fictício para equalizar o tempo de resposta.
     */
    fun performDummyVerification(password: String) {
        verifyPassword(password, DUMMY_SALT_BASE64, DUMMY_HASH_BASE64)
    }
}
