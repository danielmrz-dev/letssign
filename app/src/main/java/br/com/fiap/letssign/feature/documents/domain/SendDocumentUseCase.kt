package br.com.fiap.letssign.feature.documents.domain

import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.SendOutcome

/** Valida e dispara o envio. A chave de idempotência garante que repetir não duplica convites. */
class SendDocumentUseCase(
    private val validate: ValidateDocumentUseCase,
    private val repository: DocumentRepository,
) {
    suspend operator fun invoke(
        detail: DocumentDetail,
        ownerId: String,
        idempotencyKey: String,
        simulateFailure: Boolean = false,
    ): SendOutcome {
        val issues = validate(detail)
        if (issues.isNotEmpty()) return SendOutcome.Invalid(issues)
        return repository.send(detail.id, ownerId, idempotencyKey, simulateFailure)
    }
}
