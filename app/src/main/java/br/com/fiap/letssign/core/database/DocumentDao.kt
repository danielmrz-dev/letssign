package br.com.fiap.letssign.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // ---------- Documentos ----------
    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocument(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocument(id: String): Flow<DocumentEntity?>

    /** Home do Admin: filtra pelo dono (ownerId = usuário da sessão) e calcula o progresso da versão atual. */
    @Query(
        """
        SELECT d.id AS id, d.title AS title, d.status AS status, d.currentVersion AS versionNumber,
               d.updatedAt AS updatedAt, d.sentAt AS sentAt, d.completedAt AS completedAt,
               (SELECT COUNT(*) FROM document_signers s WHERE s.versionId = v.id) AS totalSigners,
               (SELECT COUNT(*) FROM document_signers s WHERE s.versionId = v.id AND s.status = 'SIGNED') AS signedSigners
        FROM documents d
        JOIN document_versions v ON v.documentId = d.id AND v.versionNumber = d.currentVersion
        WHERE d.ownerId = :ownerId
        ORDER BY d.updatedAt DESC
        """
    )
    fun observeOwnerSummaries(ownerId: String): Flow<List<DocumentSummaryRow>>

    @Query(
        """
        SELECT v.documentId AS documentId, u.fullName AS fullName, u.colorIndex AS colorIndex, s.orderIndex AS orderIndex
        FROM document_signers s
        JOIN users u ON u.id = s.userId
        JOIN document_versions v ON v.id = s.versionId
        JOIN documents d ON d.id = v.documentId AND v.versionNumber = d.currentVersion
        WHERE d.ownerId = :ownerId
        ORDER BY s.orderIndex
        """
    )
    fun observeOwnerSignerBadges(ownerId: String): Flow<List<SignerBadgeRow>>

    /** Home do Signatário: só documentos recebidos e já enviados (status diferente de DRAFT). */
    @Query(
        """
        SELECT d.id AS documentId, s.id AS signerLinkId, d.title AS title, o.fullName AS ownerName,
               d.sentAt AS sentAt, d.status AS documentStatus, s.role AS role, s.status AS status,
               s.signedAt AS signedAt,
               (SELECT COUNT(*) FROM signature_fields f WHERE f.signerLinkId = s.id) AS fieldCount
        FROM document_signers s
        JOIN document_versions v ON v.id = s.versionId
        JOIN documents d ON d.id = v.documentId AND v.versionNumber = d.currentVersion
        JOIN users o ON o.id = d.ownerId
        WHERE s.userId = :userId AND d.status != 'DRAFT'
        ORDER BY d.sentAt DESC
        """
    )
    fun observeReceived(userId: String): Flow<List<ReceivedRow>>

    // ---------- Versões ----------
    @Insert
    suspend fun insertVersion(version: DocumentVersionEntity)

    @Update
    suspend fun updateVersion(version: DocumentVersionEntity)

    @Query("SELECT * FROM document_versions WHERE documentId = :documentId AND versionNumber = :number")
    suspend fun getVersion(documentId: String, number: Int): DocumentVersionEntity?

    @Query("SELECT * FROM document_versions WHERE documentId = :documentId AND versionNumber = :number")
    fun observeVersion(documentId: String, number: Int): Flow<DocumentVersionEntity?>

    @Query("SELECT * FROM document_versions WHERE id = :id")
    suspend fun getVersionById(id: String): DocumentVersionEntity?

    // ---------- Signatários do documento ----------
    @Insert
    suspend fun insertSigner(signer: DocumentSignerEntity)

    @Update
    suspend fun updateSigner(signer: DocumentSignerEntity)

    @Query("DELETE FROM document_signers WHERE id = :id")
    suspend fun deleteSigner(id: String)

    @Query("SELECT * FROM document_signers WHERE id = :id")
    suspend fun getSignerLink(id: String): DocumentSignerEntity?

    @Query("SELECT * FROM document_signers WHERE versionId = :versionId ORDER BY orderIndex")
    suspend fun getSigners(versionId: String): List<DocumentSignerEntity>

    @Query("SELECT COUNT(*) FROM document_signers WHERE versionId = :versionId AND userId = :userId")
    suspend fun countSignerUser(versionId: String, userId: String): Int

    @Query("SELECT COUNT(*) FROM document_signers WHERE versionId = :versionId AND invitedAt IS NOT NULL")
    suspend fun countInvited(versionId: String): Int

    @Query(
        """
        SELECT s.id AS linkId, s.userId AS userId, u.fullName AS fullName, u.email AS email,
               u.colorIndex AS colorIndex, s.role AS role, s.orderIndex AS orderIndex, s.required AS required,
               s.status AS status, s.invitedAt AS invitedAt, s.viewedAt AS viewedAt, s.signedAt AS signedAt
        FROM document_signers s JOIN users u ON u.id = s.userId
        WHERE s.versionId = :versionId
        ORDER BY s.orderIndex
        """
    )
    fun observeSignerRows(versionId: String): Flow<List<SignerRow>>

    @Query(
        """
        SELECT s.id AS linkId, s.userId AS userId, u.fullName AS fullName, u.email AS email,
               u.colorIndex AS colorIndex, s.role AS role, s.orderIndex AS orderIndex, s.required AS required,
               s.status AS status, s.invitedAt AS invitedAt, s.viewedAt AS viewedAt, s.signedAt AS signedAt
        FROM document_signers s JOIN users u ON u.id = s.userId
        WHERE s.versionId = :versionId
        ORDER BY s.orderIndex
        """
    )
    suspend fun getSignerRows(versionId: String): List<SignerRow>

    // ---------- Campos de assinatura ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(field: SignatureFieldEntity)

    @Query("DELETE FROM signature_fields WHERE id = :id")
    suspend fun deleteField(id: String)

    @Query("DELETE FROM signature_fields WHERE versionId = :versionId AND pageIndex >= :pageCount")
    suspend fun deleteFieldsBeyondPage(versionId: String, pageCount: Int)

    @Query("SELECT * FROM signature_fields WHERE id = :id")
    suspend fun getField(id: String): SignatureFieldEntity?

    @Query("SELECT * FROM signature_fields WHERE versionId = :versionId ORDER BY pageIndex, yRatio")
    suspend fun getFields(versionId: String): List<SignatureFieldEntity>

    @Query("SELECT * FROM signature_fields WHERE versionId = :versionId ORDER BY pageIndex, yRatio")
    fun observeFields(versionId: String): Flow<List<SignatureFieldEntity>>

    @Query("SELECT * FROM signature_fields WHERE signerLinkId = :linkId ORDER BY pageIndex, yRatio")
    suspend fun getFieldsForSigner(linkId: String): List<SignatureFieldEntity>

    // ---------- Linha do tempo ----------
    @Insert
    suspend fun insertEvent(event: DocumentEventEntity)

    @Query("SELECT * FROM document_events WHERE documentId = :documentId ORDER BY createdAt ASC")
    fun observeEvents(documentId: String): Flow<List<DocumentEventEntity>>

    // ---------- Envio idempotente ----------
    @Query("SELECT * FROM send_requests WHERE idempotencyKey = :key")
    suspend fun getSendRequest(key: String): SendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSendRequest(request: SendRequestEntity)
}
