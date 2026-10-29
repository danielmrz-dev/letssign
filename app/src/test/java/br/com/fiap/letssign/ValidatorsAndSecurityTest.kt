package br.com.fiap.letssign

import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.core.util.Validators
import br.com.fiap.letssign.feature.signers.domain.SignerFormField
import br.com.fiap.letssign.feature.signers.domain.SignerInput
import br.com.fiap.letssign.feature.signers.domain.SignerValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsAndSecurityTest {

    @Test
    fun `nome precisa de nome e sobrenome`() {
        assertEquals("Informe nome e sobrenome.", Validators.fullName("Maria"))
        assertNull(Validators.fullName("Maria Souza"))
    }

    @Test
    fun `email invalido e recusado`() {
        assertTrue(Validators.email("maria@") != null)
        assertNull(Validators.email("maria@studionorte.com.br"))
    }

    @Test
    fun `telefone exige DDD`() {
        assertTrue(Validators.phone("98765-4321") != null)
        assertNull(Validators.phone("(11) 98765-4321"))
        assertNull(Validators.phone("1187654321"))
    }

    @Test
    fun `senha minima de 8 caracteres e opcional na edicao`() {
        assertTrue(Validators.password("1234567", required = true) != null)
        assertNull(Validators.password("", required = false))
        assertNull(Validators.password("12345678", required = true))
    }

    @Test
    fun `validador do cadastro devolve todos os erros`() {
        val errors = SignerValidator.validate(SignerInput("Ana", "x", "11", ""), passwordRequired = true)
        assertEquals(setOf(SignerFormField.NAME, SignerFormField.EMAIL, SignerFormField.PHONE, SignerFormField.PASSWORD), errors.keys)
    }

    @Test
    fun `mascara de telefone`() {
        assertEquals("(11) 98765-4321", Formatters.phone("11987654321"))
        assertEquals("(11) 8765-4321", Formatters.phone("1187654321"))
        assertEquals("(11) 987", Formatters.phone("11987"))
    }

    @Test
    fun `senha nunca fica em texto puro e o hash confere`() {
        val hasher = PasswordHasher(iterations = 1_000)
        val salt = hasher.newSalt()
        val stored = hasher.hash("selo@2026", salt)
        assertFalse(stored.contains("selo@2026"))
        assertTrue(hasher.verify("selo@2026", salt, stored))
        assertFalse(hasher.verify("outra-senha", salt, stored))
    }

    @Test
    fun `salts diferentes geram hashes diferentes`() {
        val hasher = PasswordHasher(iterations = 1_000)
        assertNotEquals(hasher.hash("selo@2026", hasher.newSalt()), hasher.hash("selo@2026", hasher.newSalt()))
    }
}
