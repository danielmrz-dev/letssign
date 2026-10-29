package br.com.fiap.letssign

import br.com.fiap.letssign.core.security.PasswordHasherImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSecurityUnitTest {

    private val passwordHasher = PasswordHasherImpl()

    @Test
    fun passwordHasher_generatesDifferentSaltAndHashForSamePassword() {
        val result1 = passwordHasher.hashPassword("admin1234")
        val result2 = passwordHasher.hashPassword("admin1234")

        assertNotNull(result1.hash)
        assertNotNull(result1.salt)
        assertNotEquals(result1.salt, result2.salt)
        assertNotEquals(result1.hash, result2.hash)
    }

    @Test
    fun passwordHasher_verifiesCorrectPasswordSuccessfully() {
        val hashed = passwordHasher.hashPassword("maria1234")
        val isValid = passwordHasher.verifyPassword("maria1234", hashed.salt, hashed.hash)
        assertTrue(isValid)
    }

    @Test
    fun passwordHasher_rejectsIncorrectPassword() {
        val hashed = passwordHasher.hashPassword("maria1234")
        val isValid = passwordHasher.verifyPassword("senha_errada", hashed.salt, hashed.hash)
        assertFalse(isValid)
    }
}
