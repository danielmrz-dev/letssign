package br.com.fiap.letssign.core.database

/** Linhas de consultas com JOIN (somente leitura). */
data class DocumentSummaryRow(
    val id: String,
    val title: String,
    val status: String,
    val versionNumber: Int,
    val updatedAt: Long,
    val sentAt: Long?,
    val completedAt: Long?,
    val totalSigners: Int,
    val signedSigners: Int,
)

data class SignerBadgeRow(
    val documentId: String,
    val fullName: String,
    val colorIndex: Int,
    val orderIndex: Int,
)

data class SignerRow(
    val linkId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val colorIndex: Int,
    val role: String,
    val orderIndex: Int,
    val required: Boolean,
    val status: String,
    val invitedAt: Long?,
    val viewedAt: Long?,
    val signedAt: Long?,
)

data class ReceivedRow(
    val documentId: String,
    val signerLinkId: String,
    val title: String,
    val ownerName: String,
    val sentAt: Long?,
    val documentStatus: String,
    val role: String,
    val status: String,
    val signedAt: Long?,
    val fieldCount: Int,
)
