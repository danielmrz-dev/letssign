package br.com.fiap.letssign.core.model

import java.io.File

enum class UserRole { ADMIN, SIGNER }

enum class SignerRole(val label: String) {
    CONTRATANTE("Contratante"),
    CONTRATADO("Contratado"),
    TESTEMUNHA("Testemunha");

    companion object {
        fun from(value: String): SignerRole = entries.firstOrNull { it.name == value } ?: TESTEMUNHA

        /** Papel sugerido pela ordem de entrada: 1º contratante, 2º contratado, demais testemunhas. */
        fun defaultFor(position: Int): SignerRole = when (position) {
            0 -> CONTRATANTE
            1 -> CONTRATADO
            else -> TESTEMUNHA
        }
    }
}

enum class DocumentStatus(val label: String) {
    DRAFT("Rascunho"),
    SENT("Enviado"),
    PARTIALLY_SIGNED("Parcialmente assinado"),
    COMPLETED("Concluído");

    companion object {
        fun from(value: String): DocumentStatus = entries.firstOrNull { it.name == value } ?: DRAFT
    }
}

enum class SignerStatus(val label: String) {
    PENDING("Pendente"),
    SIGNED("Assinado");

    companion object {
        fun from(value: String): SignerStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

enum class EventType(val label: String) {
    CREATED("Documento criado"),
    SENT("Enviado para assinatura"),
    SEND_FAILED("Falha no envio"),
    VIEWED("Documento visualizado"),
    SIGNED("Assinatura realizada"),
    COMPLETED("Documento concluído"),
    NEW_VERSION("Nova versão criada");

    companion object {
        fun from(value: String): EventType = entries.firstOrNull { it.name == value } ?: CREATED
    }
}

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val colorIndex: Int,
) {
    val firstName: String get() = fullName.trim().split(" ").first()
}

/** Campo de assinatura salvo em proporção da página (0..1), independente da tela. */
data class SignatureField(
    val id: String,
    val signerLinkId: String,
    val pageIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
)

data class DocumentSigner(
    val linkId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val colorIndex: Int,
    val role: SignerRole,
    val order: Int,
    val required: Boolean,
    val status: SignerStatus,
    val invitedAt: Long?,
    val viewedAt: Long?,
    val signedAt: Long?,
)

data class DocumentVersion(
    val id: String,
    val documentId: String,
    val number: Int,
    val originalPath: String,
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val sha256: String,
    val signedPath: String?,
    val signedSha256: String?,
    val inviteMessage: String,
    val idempotencyKey: String?,
)

data class DocumentEvent(
    val id: String,
    val type: EventType,
    val description: String,
    val createdAt: Long,
)

data class DocumentDetail(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val title: String,
    val status: DocumentStatus,
    val createdAt: Long,
    val sentAt: Long?,
    val completedAt: Long?,
    val version: DocumentVersion?,
    val signers: List<DocumentSigner>,
    val fields: List<SignatureField>,
    val events: List<DocumentEvent>,
) {
    val signedCount: Int get() = signers.count { it.status == SignerStatus.SIGNED }
    fun fieldsFor(linkId: String): List<SignatureField> = fields.filter { it.signerLinkId == linkId }
}

data class SignerBadge(val name: String, val colorIndex: Int)

data class DocumentSummary(
    val id: String,
    val title: String,
    val status: DocumentStatus,
    val versionNumber: Int,
    val updatedAt: Long,
    val sentAt: Long?,
    val completedAt: Long?,
    val signers: List<SignerBadge>,
    val signedCount: Int,
    val totalCount: Int,
)

data class ReceivedDocument(
    val documentId: String,
    val signerLinkId: String,
    val title: String,
    val ownerName: String,
    val sentAt: Long?,
    val role: SignerRole,
    val status: SignerStatus,
    val signedAt: Long?,
    val fieldCount: Int,
    val documentStatus: DocumentStatus,
)

data class SigningSession(
    val documentId: String,
    val title: String,
    val ownerName: String,
    val versionNumber: Int,
    val pdfPath: String,
    val pageCount: Int,
    val me: DocumentSigner,
    val signers: List<DocumentSigner>,
    val fields: List<SignatureField>,
    val documentStatus: DocumentStatus,
)

/** PDF validado e copiado para a área temporária, pronto para virar a versão do documento. */
data class ImportedPdf(
    val file: File,
    val fileName: String,
    val size: Long,
    val pageCount: Int,
    val sha256: String,
)

/** Regra de negócio violada: a mensagem é mostrada para o usuário. */
class DomainException(message: String) : Exception(message)

sealed interface SendOutcome {
    data class Success(val invites: Int, val attempt: Int, val key: String, val alreadySent: Boolean) : SendOutcome
    data class Failure(val reason: String, val attempt: Int, val key: String) : SendOutcome
    data class Invalid(val issues: List<String>) : SendOutcome
}

sealed interface SignOutcome {
    data class Success(val completed: Boolean) : SignOutcome
    data class Failure(val message: String) : SignOutcome
}
