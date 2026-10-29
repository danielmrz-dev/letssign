package br.com.fiap.letssign.feature.documents.domain

import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.util.Validators

/**
 * Valida os dados obrigatórios antes do envio. A lista vazia significa "tudo certo para enviar".
 * Regras: título, PDF, pelo menos 1 signatário, papel definido, todo campo ligado a um participante,
 * todo signatário com campo, campos dentro da página e mensagem do convite.
 */
class ValidateDocumentUseCase {

    operator fun invoke(detail: DocumentDetail, title: String = detail.title, inviteMessage: String? = null): List<String> =
        validate(
            title = title,
            hasPdf = detail.version != null,
            pageCount = detail.version?.pageCount ?: 0,
            signers = detail.signers,
            fields = detail.fields,
            inviteMessage = inviteMessage ?: detail.version?.inviteMessage.orEmpty(),
        )

    fun validate(
        title: String,
        hasPdf: Boolean,
        pageCount: Int,
        signers: List<DocumentSigner>,
        fields: List<SignatureField>,
        inviteMessage: String,
    ): List<String> {
        val issues = mutableListOf<String>()
        Validators.title(title)?.let { issues += it }
        if (!hasPdf) issues += "Selecione o arquivo PDF."
        if (signers.isEmpty()) issues += "Adicione pelo menos 1 signatário."

        signers.groupBy { it.userId }.values.filter { it.size > 1 }.forEach { duplicated ->
            issues += "${duplicated.first().fullName} foi adicionado mais de uma vez."
        }

        val linkIds = signers.map { it.linkId }.toSet()
        if (fields.any { it.signerLinkId !in linkIds }) {
            issues += "Há campo de assinatura sem participante associado."
        }
        fields.filter { !FieldGeometry.isInsidePage(it) || it.pageIndex !in 0 until pageCount }
            .map { it.pageIndex + 1 }
            .distinct()
            .forEach { page -> issues += "Há campo fora dos limites da página $page." }

        signers.filter { signer -> fields.none { it.signerLinkId == signer.linkId } }
            .forEach { issues += "${it.fullName} ainda não tem campo de assinatura." }

        when {
            inviteMessage.isBlank() -> issues += "Escreva a mensagem que acompanha o convite."
            inviteMessage.length > Validators.INVITE_MAX -> issues += "A mensagem do convite pode ter até ${Validators.INVITE_MAX} caracteres."
        }
        return issues.distinct()
    }
}
