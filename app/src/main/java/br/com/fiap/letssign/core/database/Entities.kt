package br.com.fiap.letssign.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Admin e signatários ficam na mesma tabela; signatários são criados pelo Admin (createdBy). */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true), Index("createdBy")])
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val salt: String,
    val role: String,
    val colorIndex: Int,
    val createdBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "documents", indices = [Index("ownerId")])
data class DocumentEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val status: String,
    val currentVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val sentAt: Long?,
    val completedAt: Long?,
)

/** Cada envio fica preso a uma versão. Alterar depois do envio gera uma nova versão (novo fluxo). */
@Entity(
    tableName = "document_versions",
    indices = [Index(value = ["documentId", "versionNumber"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DocumentVersionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val versionNumber: Int,
    val originalPath: String,
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val sha256: String,
    val signedPath: String?,
    val signedSha256: String?,
    val inviteMessage: String,
    val idempotencyKey: String?,
    val createdAt: Long,
)

/** Vínculo signatário x versão. O índice único impede a mesma pessoa duas vezes no documento. */
@Entity(
    tableName = "document_signers",
    indices = [Index(value = ["versionId", "userId"], unique = true), Index("userId")],
    foreignKeys = [
        ForeignKey(
            entity = DocumentVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["versionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DocumentSignerEntity(
    @PrimaryKey val id: String,
    val versionId: String,
    val userId: String,
    val role: String,
    val orderIndex: Int,
    val required: Boolean,
    val status: String,
    val invitedAt: Long?,
    val viewedAt: Long?,
    val signedAt: Long?,
    val signatureImagePath: String?,
)

/** Coordenadas relativas à página (0..1): o campo fica no mesmo lugar em qualquer aparelho. */
@Entity(
    tableName = "signature_fields",
    indices = [Index("versionId"), Index("signerLinkId")],
    foreignKeys = [
        ForeignKey(
            entity = DocumentSignerEntity::class,
            parentColumns = ["id"],
            childColumns = ["signerLinkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SignatureFieldEntity(
    @PrimaryKey val id: String,
    val versionId: String,
    val signerLinkId: String,
    val pageIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
)

@Entity(
    tableName = "document_events",
    indices = [Index("documentId")],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DocumentEventEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val versionNumber: Int,
    val type: String,
    val actorId: String?,
    val description: String,
    val createdAt: Long,
)

/** Registro de envio por chave de idempotência: repetir o envio não duplica convites. */
@Entity(tableName = "send_requests")
data class SendRequestEntity(
    @PrimaryKey val idempotencyKey: String,
    val versionId: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val updatedAt: Long,
)

object SendStatus {
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
}
