package br.com.fiap.letssign.feature.documents.domain

import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.DocumentSummary
import br.com.fiap.letssign.core.model.ImportedPdf
import br.com.fiap.letssign.core.model.ReceivedDocument
import br.com.fiap.letssign.core.model.SendOutcome
import br.com.fiap.letssign.core.model.SignOutcome
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.SigningSession
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Contrato de documentos. Hoje a implementação é local (Room + arquivos);
 * na Sprint 2 entra uma implementação remota (Retrofit) com o mesmo contrato.
 *
 * Todas as operações recebem o usuário da sessão (ownerId/userId) para garantir o isolamento:
 * um usuário nunca acessa dados de outro sem que o documento tenha sido enviado a ele.
 * Métodos de edição lançam [br.com.fiap.letssign.core.model.DomainException] quando o documento já foi enviado.
 */
interface DocumentRepository {
    fun observeOwnerDocuments(ownerId: String): Flow<List<DocumentSummary>>
    fun observeReceived(userId: String): Flow<List<ReceivedDocument>>
    fun observeDetail(documentId: String, ownerId: String): Flow<DocumentDetail?>

    // Rascunho (somente status DRAFT)
    suspend fun createDraft(ownerId: String, title: String, pdf: ImportedPdf): String
    suspend fun replaceDraftPdf(documentId: String, ownerId: String, pdf: ImportedPdf)
    suspend fun updateTitle(documentId: String, ownerId: String, title: String)
    suspend fun addSigner(documentId: String, ownerId: String, userId: String)
    suspend fun replaceSigner(documentId: String, ownerId: String, linkId: String, newUserId: String)
    suspend fun removeSigner(documentId: String, ownerId: String, linkId: String)
    suspend fun updateSignerRole(documentId: String, ownerId: String, linkId: String, role: SignerRole)
    suspend fun addField(documentId: String, ownerId: String, linkId: String, pageIndex: Int): SignatureField
    suspend fun updateField(documentId: String, ownerId: String, field: SignatureField)
    suspend fun deleteField(documentId: String, ownerId: String, fieldId: String)
    suspend fun updateInviteMessage(documentId: String, ownerId: String, message: String)
    suspend fun ensureIdempotencyKey(documentId: String, ownerId: String): String
    suspend fun deleteDraft(documentId: String, ownerId: String)

    // Envio e versões
    suspend fun send(documentId: String, ownerId: String, idempotencyKey: String, simulateFailure: Boolean): SendOutcome
    suspend fun createNewVersion(documentId: String, ownerId: String)

    // Assinatura (perfil Signatário)
    suspend fun loadSigningSession(linkId: String, userId: String): SigningSession?
    suspend fun markViewed(linkId: String, userId: String)
    suspend fun sign(linkId: String, userId: String, signaturePng: File): SignOutcome
}
