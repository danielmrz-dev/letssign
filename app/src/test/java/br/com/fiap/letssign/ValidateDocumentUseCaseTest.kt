package br.com.fiap.letssign

import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.feature.documents.domain.ValidateDocumentUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateDocumentUseCaseTest {

    private val validate = ValidateDocumentUseCase()

    private fun signer(link: String, user: String = "u-$link", name: String = "Maria Souza") = DocumentSigner(
        linkId = link,
        userId = user,
        fullName = name,
        email = "$link@teste.com",
        colorIndex = 0,
        role = SignerRole.CONTRATANTE,
        order = 0,
        required = true,
        status = SignerStatus.PENDING,
        invitedAt = null,
        viewedAt = null,
        signedAt = null,
    )

    private fun field(link: String, page: Int = 0, x: Float = 0.1f) =
        SignatureField("f-$link-$page-$x", link, page, x, 0.1f, 0.3f, 0.08f)

    private fun run(
        title: String = "Contrato",
        hasPdf: Boolean = true,
        pages: Int = 2,
        signers: List<DocumentSigner> = listOf(signer("a")),
        fields: List<SignatureField> = listOf(field("a")),
        invite: String = "Olá! Assine, por favor.",
    ) = validate.validate(title, hasPdf, pages, signers, fields, invite)

    @Test
    fun `documento completo nao tem pendencias`() {
        assertEquals(emptyList<String>(), run())
    }

    @Test
    fun `precisa de pelo menos um signatario`() {
        val issues = run(signers = emptyList(), fields = emptyList())
        assertTrue(issues.contains("Adicione pelo menos 1 signatário."))
    }

    @Test
    fun `signatario sem campo bloqueia o envio`() {
        val issues = run(signers = listOf(signer("a"), signer("b", name = "Carlos Mendes")))
        assertTrue(issues.contains("Carlos Mendes ainda não tem campo de assinatura."))
    }

    @Test
    fun `todo campo precisa estar associado a um participante`() {
        val issues = run(fields = listOf(field("a"), field("fantasma")))
        assertTrue(issues.contains("Há campo de assinatura sem participante associado."))
    }

    @Test
    fun `campo fora da pagina e detectado`() {
        val issues = run(fields = listOf(field("a", x = 0.9f)))
        assertTrue(issues.contains("Há campo fora dos limites da página 1."))
    }

    @Test
    fun `campo em pagina inexistente e detectado`() {
        val issues = run(fields = listOf(field("a", page = 5)))
        assertTrue(issues.contains("Há campo fora dos limites da página 6."))
    }

    @Test
    fun `titulo pdf e mensagem sao obrigatorios`() {
        val issues = run(title = " ", hasPdf = false, invite = "")
        assertTrue(issues.contains("Informe o título do documento."))
        assertTrue(issues.contains("Selecione o arquivo PDF."))
        assertTrue(issues.contains("Escreva a mensagem que acompanha o convite."))
    }

    @Test
    fun `mesmo signatario duas vezes e detectado`() {
        val issues = run(
            signers = listOf(signer("a", user = "u1"), signer("b", user = "u1")),
            fields = listOf(field("a"), field("b")),
        )
        assertTrue(issues.contains("Maria Souza foi adicionado mais de uma vez."))
    }
}
