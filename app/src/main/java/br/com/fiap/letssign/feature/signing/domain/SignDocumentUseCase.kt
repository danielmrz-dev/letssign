package br.com.fiap.letssign.feature.signing.domain

import br.com.fiap.letssign.core.model.SignOutcome
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import java.io.File

/**
 * Aplica a assinatura em todos os campos do signatário, salva o PDF assinado e atualiza o status.
 * O documento só passa a "Concluído" quando todos os signatários obrigatórios assinam.
 */
class SignDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(linkId: String, userId: String, signaturePng: File): SignOutcome {
        if (!signaturePng.exists() || signaturePng.length() == 0L) {
            return SignOutcome.Failure("Desenhe sua assinatura antes de concluir.")
        }
        return repository.sign(linkId, userId, signaturePng)
    }
}
