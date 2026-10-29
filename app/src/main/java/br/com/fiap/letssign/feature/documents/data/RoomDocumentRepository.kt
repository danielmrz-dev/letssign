package br.com.fiap.letssign.feature.documents.data

import androidx.room.withTransaction
import br.com.fiap.letssign.core.database.DocumentEntity
import br.com.fiap.letssign.core.database.DocumentEventEntity
import br.com.fiap.letssign.core.database.DocumentSignerEntity
import br.com.fiap.letssign.core.database.DocumentVersionEntity
import br.com.fiap.letssign.core.database.SendRequestEntity
import br.com.fiap.letssign.core.database.SendStatus
import br.com.fiap.letssign.core.database.SeloDatabase
import br.com.fiap.letssign.core.database.SignatureFieldEntity
import br.com.fiap.letssign.core.database.toDomain
import br.com.fiap.letssign.core.database.toEntity
import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.DocumentSummary
import br.com.fiap.letssign.core.model.DomainException
import br.com.fiap.letssign.core.model.EventType
import br.com.fiap.letssign.core.model.ImportedPdf
import br.com.fiap.letssign.core.model.ReceivedDocument
import br.com.fiap.letssign.core.model.SendOutcome
import br.com.fiap.letssign.core.model.SignOutcome
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerBadge
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.model.SigningSession
import br.com.fiap.letssign.core.model.UserRole
import br.com.fiap.letssign.core.pdf.AuditInfo
import br.com.fiap.letssign.core.pdf.AuditSigner
import br.com.fiap.letssign.core.pdf.PdfStamper
import br.com.fiap.letssign.core.security.sha256Hex
import br.com.fiap.letssign.core.storage.DocumentStorage
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.feature.documents.domain.FieldGeometry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RoomDocumentRepository(
    private val db: SeloDatabase,
    private val storage: DocumentStorage,
    private val stamper: PdfStamper,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : DocumentRepository {

    private val dao = db.documentDao()
    private val userDao = db.userDao()

    // ------------------------------------------------------------------ Consultas

    override fun observeOwnerDocuments(ownerId: String): Flow<List<DocumentSummary>> =
        combine(dao.observeOwnerSummaries(ownerId), dao.observeOwnerSignerBadges(ownerId)) { rows, badges ->
            val byDocument = badges.groupBy { it.documentId }
            rows.map { row ->
                DocumentSummary(
                    id = row.id,
                    title = row.title,
                    status = DocumentStatus.from(row.status),
                    versionNumber = row.versionNumber,
                    updatedAt = row.updatedAt,
                    sentAt = row.sentAt,
                    completedAt = row.completedAt,
                    signers = byDocument[row.id].orEmpty().map { SignerBadge(it.fullName, it.colorIndex) },
                    signedCount = row.signedSigners,
                    totalCount = row.totalSigners,
                )
            }
        }

    override fun observeReceived(userId: String): Flow<List<ReceivedDocument>> =
        dao.observeReceived(userId).map { rows ->
            rows.map { row ->
                ReceivedDocument(
                    documentId = row.documentId,
                    signerLinkId = row.signerLinkId,
                    title = row.title,
                    ownerName = row.ownerName,
                    sentAt = row.sentAt,
                    role = SignerRole.from(row.role),
                    status = SignerStatus.from(row.status),
                    signedAt = row.signedAt,
                    fieldCount = row.fieldCount,
                    documentStatus = DocumentStatus.from(row.documentStatus),
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDetail(documentId: String, ownerId: String): Flow<DocumentDetail?> =
        dao.observeDocument(documentId).flatMapLatest { document ->
            if (document == null || document.ownerId != ownerId) {
                flowOf(null)
            } else {
                dao.observeVersion(document.id, document.currentVersion).flatMapLatest { version ->
                    if (version == null) {
                        userDao.observeById(document.ownerId).map { owner ->
                            document.toDetail(owner?.fullName.orEmpty(), null, emptyList(), emptyList(), emptyList())
                        }
                    } else {
                        combine(
                            dao.observeSignerRows(version.id),
                            dao.observeFields(version.id),
                            dao.observeEvents(document.id),
                            userDao.observeById(document.ownerId),
                        ) { signers, fields, events, owner ->
                            document.toDetail(owner?.fullName.orEmpty(), version, signers, fields, events)
                        }
                    }
                }
            }
        }

    private fun DocumentEntity.toDetail(
        ownerName: String,
        version: DocumentVersionEntity?,
        signers: List<br.com.fiap.letssign.core.database.SignerRow>,
        fields: List<SignatureFieldEntity>,
        events: List<DocumentEventEntity>,
    ) = DocumentDetail(
        id = id,
        ownerId = ownerId,
        ownerName = ownerName,
        title = title,
        status = DocumentStatus.from(status),
        createdAt = createdAt,
        sentAt = sentAt,
        completedAt = completedAt,
        version = version?.toDomain(),
        signers = signers.map { it.toDomain() },
        fields = fields.map { it.toDomain() },
        events = events.map { it.toDomain() },
    )

    // ------------------------------------------------------------------ Rascunho

    override suspend fun createDraft(ownerId: String, title: String, pdf: ImportedPdf): String =
        withContext(Dispatchers.IO) {
            val documentId = newId()
            val now = clock()
            val target = storage.versionFile(documentId, 1)
            target.parentFile?.mkdirs()
            pdf.file.copyTo(target, overwrite = true)
            pdf.file.delete()
            db.withTransaction {
                dao.insertDocument(
                    DocumentEntity(
                        id = documentId,
                        ownerId = ownerId,
                        title = title.trim().take(TITLE_LIMIT),
                        status = DocumentStatus.DRAFT.name,
                        currentVersion = 1,
                        createdAt = now,
                        updatedAt = now,
                        sentAt = null,
                        completedAt = null,
                    ),
                )
                dao.insertVersion(
                    DocumentVersionEntity(
                        id = newId(),
                        documentId = documentId,
                        versionNumber = 1,
                        originalPath = target.absolutePath,
                        fileName = pdf.fileName,
                        fileSize = pdf.size,
                        pageCount = pdf.pageCount,
                        sha256 = pdf.sha256,
                        signedPath = null,
                        signedSha256 = null,
                        inviteMessage = DEFAULT_INVITE,
                        idempotencyKey = null,
                        createdAt = now,
                    ),
                )
                dao.insertEvent(event(documentId, 1, EventType.CREATED, ownerId, "A partir do arquivo ${pdf.fileName}", now))
            }
            documentId
        }

    override suspend fun replaceDraftPdf(documentId: String, ownerId: String, pdf: ImportedPdf) =
        withContext(Dispatchers.IO) {
            val (document, version) = requireDraft(documentId, ownerId)
            val target = storage.versionFile(document.id, version.versionNumber)
            target.parentFile?.mkdirs()
            pdf.file.copyTo(target, overwrite = true)
            pdf.file.delete()
            db.withTransaction {
                dao.updateVersion(
                    version.copy(
                        originalPath = target.absolutePath,
                        fileName = pdf.fileName,
                        fileSize = pdf.size,
                        pageCount = pdf.pageCount,
                        sha256 = pdf.sha256,
                    ),
                )
                dao.deleteFieldsBeyondPage(version.id, pdf.pageCount)
                touch(document)
            }
        }

    override suspend fun updateTitle(documentId: String, ownerId: String, title: String) {
        val (document, _) = requireDraft(documentId, ownerId)
        dao.updateDocument(document.copy(title = title.trim().take(TITLE_LIMIT), updatedAt = clock()))
    }

    override suspend fun addSigner(documentId: String, ownerId: String, userId: String) {
        db.withTransaction {
            val (document, version) = requireDraft(documentId, ownerId)
            requireOwnSigner(ownerId, userId)
            if (dao.countSignerUser(version.id, userId) > 0) {
                throw DomainException("Esse signatário já está no documento.")
            }
            val current = dao.getSigners(version.id)
            dao.insertSigner(
                DocumentSignerEntity(
                    id = newId(),
                    versionId = version.id,
                    userId = userId,
                    role = SignerRole.defaultFor(current.size).name,
                    orderIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                    required = true,
                    status = SignerStatus.PENDING.name,
                    invitedAt = null,
                    viewedAt = null,
                    signedAt = null,
                    signatureImagePath = null,
                ),
            )
            touch(document)
        }
    }

    /** Substitui a pessoa mantendo o vínculo: os campos já posicionados passam para o substituto. */
    override suspend fun replaceSigner(documentId: String, ownerId: String, linkId: String, newUserId: String) {
        db.withTransaction {
            val (document, version) = requireDraft(documentId, ownerId)
            requireOwnSigner(ownerId, newUserId)
            val link = dao.getSignerLink(linkId)?.takeIf { it.versionId == version.id }
                ?: throw DomainException("Signatário não encontrado no documento.")
            if (link.userId == newUserId) return@withTransaction
            if (dao.countSignerUser(version.id, newUserId) > 0) {
                throw DomainException("Esse signatário já está no documento.")
            }
            dao.updateSigner(link.copy(userId = newUserId))
            touch(document)
        }
    }

    override suspend fun removeSigner(documentId: String, ownerId: String, linkId: String) {
        db.withTransaction {
            val (document, version) = requireDraft(documentId, ownerId)
            val link = dao.getSignerLink(linkId)?.takeIf { it.versionId == version.id } ?: return@withTransaction
            dao.deleteSigner(link.id) // os campos do signatário saem junto (ON DELETE CASCADE)
            touch(document)
        }
    }

    override suspend fun updateSignerRole(documentId: String, ownerId: String, linkId: String, role: SignerRole) {
        val (document, version) = requireDraft(documentId, ownerId)
        val link = dao.getSignerLink(linkId)?.takeIf { it.versionId == version.id } ?: return
        dao.updateSigner(link.copy(role = role.name))
        touch(document)
    }

    override suspend fun addField(documentId: String, ownerId: String, linkId: String, pageIndex: Int): SignatureField =
        db.withTransaction {
            val (document, version) = requireDraft(documentId, ownerId)
            dao.getSignerLink(linkId)?.takeIf { it.versionId == version.id }
                ?: throw DomainException("Selecione um signatário antes de adicionar o campo.")
            if (pageIndex !in 0 until version.pageCount) throw DomainException("Página inválida.")
            val onPage = dao.getFields(version.id).count { it.pageIndex == pageIndex }
            val (x, y) = FieldGeometry.newFieldPosition(onPage)
            val field = FieldGeometry.clamp(
                SignatureField(
                    id = newId(),
                    signerLinkId = linkId,
                    pageIndex = pageIndex,
                    xRatio = x,
                    yRatio = y,
                    widthRatio = FieldGeometry.DEFAULT_WIDTH,
                    heightRatio = FieldGeometry.DEFAULT_HEIGHT,
                ),
            )
            dao.upsertField(field.toEntity(version.id))
            touch(document)
            field
        }

    override suspend fun updateField(documentId: String, ownerId: String, field: SignatureField) {
        val (document, version) = requireDraft(documentId, ownerId)
        val existing = dao.getField(field.id)?.takeIf { it.versionId == version.id }
            ?: throw DomainException("Campo não encontrado.")
        val clamped = FieldGeometry.clamp(field.copy(signerLinkId = existing.signerLinkId))
        dao.upsertField(clamped.toEntity(version.id))
        touch(document)
    }

    override suspend fun deleteField(documentId: String, ownerId: String, fieldId: String) {
        val (document, version) = requireDraft(documentId, ownerId)
        val existing = dao.getField(fieldId)?.takeIf { it.versionId == version.id } ?: return
        dao.deleteField(existing.id)
        touch(document)
    }

    override suspend fun updateInviteMessage(documentId: String, ownerId: String, message: String) {
        val (document, version) = requireDraft(documentId, ownerId)
        dao.updateVersion(version.copy(inviteMessage = message.take(INVITE_LIMIT)))
        touch(document)
    }

    /** A chave é criada ao entrar na revisão e reaproveitada até o envio dar certo. */
    override suspend fun ensureIdempotencyKey(documentId: String, ownerId: String): String {
        val (_, version) = requireDraft(documentId, ownerId)
        version.idempotencyKey?.let { return it }
        val key = UUID.randomUUID().toString()
        dao.updateVersion(version.copy(idempotencyKey = key))
        return key
    }

    override suspend fun deleteDraft(documentId: String, ownerId: String) = withContext(Dispatchers.IO) {
        val (document, _) = requireDraft(documentId, ownerId)
        dao.deleteDocument(document.id)
        storage.deleteDocument(document.id)
    }

    // ------------------------------------------------------------------ Envio

    /**
     * Envio idempotente: os convites e o registro do envio são gravados numa única transação.
     * Em falha, a transação é desfeita (rollback) e a nova tentativa usa a mesma chave.
     * Se a chave já tiver um envio concluído, nada é gravado de novo.
     */
    override suspend fun send(
        documentId: String,
        ownerId: String,
        idempotencyKey: String,
        simulateFailure: Boolean,
    ): SendOutcome = withContext(Dispatchers.IO) {
        val existing = dao.getSendRequest(idempotencyKey)
        if (existing != null && existing.status == SendStatus.SUCCESS) {
            return@withContext SendOutcome.Success(
                invites = dao.countInvited(existing.versionId),
                attempt = existing.attempts,
                key = idempotencyKey,
                alreadySent = true,
            )
        }
        val attempt = (existing?.attempts ?: 0) + 1
        var invites = 0
        var versionId: String? = existing?.versionId
        try {
            db.withTransaction {
                val (document, version) = requireDraft(documentId, ownerId)
                versionId = version.id
                val signers = dao.getSigners(version.id)
                if (signers.isEmpty()) throw DomainException("Adicione pelo menos 1 signatário.")
                val now = clock()
                signers.forEach { dao.updateSigner(it.copy(status = SignerStatus.PENDING.name, invitedAt = now)) }
                dao.updateDocument(document.copy(status = DocumentStatus.SENT.name, sentAt = now, updatedAt = now))
                dao.insertEvent(
                    event(
                        document.id,
                        version.versionNumber,
                        EventType.SENT,
                        ownerId,
                        "${Formatters.plural(signers.size, "convite registrado", "convites registrados")} · tentativa $attempt",
                        now,
                    ),
                )
                if (simulateFailure) throw SimulatedSendFailure()
                dao.upsertSendRequest(SendRequestEntity(idempotencyKey, version.id, SendStatus.SUCCESS, attempt, null, now))
                invites = signers.size
            }
            SendOutcome.Success(invites, attempt, idempotencyKey, alreadySent = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SimulatedSendFailure) {
            recordFailure(documentId, versionId, idempotencyKey, attempt, SIMULATED_FAILURE)
        } catch (e: DomainException) {
            recordFailure(documentId, versionId, idempotencyKey, attempt, e.message ?: GENERIC_FAILURE)
        } catch (e: Exception) {
            recordFailure(documentId, versionId, idempotencyKey, attempt, GENERIC_FAILURE)
        }
    }

    private suspend fun recordFailure(
        documentId: String,
        versionId: String?,
        key: String,
        attempt: Int,
        reason: String,
    ): SendOutcome.Failure {
        runCatching {
            val now = clock()
            if (versionId != null) {
                dao.upsertSendRequest(SendRequestEntity(key, versionId, SendStatus.FAILED, attempt, reason, now))
            }
            val document = dao.getDocument(documentId)
            if (document != null) {
                dao.insertEvent(event(document.id, document.currentVersion, EventType.SEND_FAILED, document.ownerId, "Tentativa $attempt: $reason", now))
            }
        }
        return SendOutcome.Failure(reason, attempt, key)
    }

    /** O documento enviado é preservado; a nova versão copia PDF, signatários e campos e volta a ser rascunho. */
    override suspend fun createNewVersion(documentId: String, ownerId: String) = withContext(Dispatchers.IO) {
        val document = dao.getDocument(documentId)?.takeIf { it.ownerId == ownerId }
            ?: throw DomainException("Documento não encontrado.")
        if (DocumentStatus.from(document.status) == DocumentStatus.DRAFT) {
            throw DomainException("Este documento ainda é um rascunho e pode ser editado.")
        }
        val previous = dao.getVersion(document.id, document.currentVersion)
            ?: throw DomainException("Versão atual não encontrada.")
        val nextNumber = previous.versionNumber + 1
        val target = storage.versionFile(document.id, nextNumber)
        File(previous.originalPath).copyTo(target, overwrite = true)
        val now = clock()
        db.withTransaction {
            val newVersionId = newId()
            dao.insertVersion(
                previous.copy(
                    id = newVersionId,
                    versionNumber = nextNumber,
                    originalPath = target.absolutePath,
                    signedPath = null,
                    signedSha256 = null,
                    idempotencyKey = null,
                    createdAt = now,
                ),
            )
            val linkMap = mutableMapOf<String, String>()
            dao.getSigners(previous.id).forEach { signer ->
                val newLinkId = newId()
                linkMap[signer.id] = newLinkId
                dao.insertSigner(
                    signer.copy(
                        id = newLinkId,
                        versionId = newVersionId,
                        status = SignerStatus.PENDING.name,
                        invitedAt = null,
                        viewedAt = null,
                        signedAt = null,
                        signatureImagePath = null,
                    ),
                )
            }
            dao.getFields(previous.id).forEach { field ->
                val newLink = linkMap[field.signerLinkId] ?: return@forEach
                dao.upsertField(field.copy(id = newId(), versionId = newVersionId, signerLinkId = newLink))
            }
            dao.updateDocument(
                document.copy(
                    status = DocumentStatus.DRAFT.name,
                    currentVersion = nextNumber,
                    sentAt = null,
                    completedAt = null,
                    updatedAt = now,
                ),
            )
            dao.insertEvent(
                event(document.id, nextNumber, EventType.NEW_VERSION, ownerId, "Versão $nextNumber criada; a versão ${previous.versionNumber} foi preservada", now),
            )
        }
    }

    // ------------------------------------------------------------------ Assinatura

    override suspend fun loadSigningSession(linkId: String, userId: String): SigningSession? =
        withContext(Dispatchers.IO) {
            val link = dao.getSignerLink(linkId)?.takeIf { it.userId == userId } ?: return@withContext null
            val version = dao.getVersionById(link.versionId) ?: return@withContext null
            val document = dao.getDocument(version.documentId) ?: return@withContext null
            val status = DocumentStatus.from(document.status)
            if (status == DocumentStatus.DRAFT || document.currentVersion != version.versionNumber) return@withContext null
            val signers = dao.getSignerRows(version.id).map { it.toDomain() }
            val me = signers.firstOrNull { it.linkId == linkId } ?: return@withContext null
            SigningSession(
                documentId = document.id,
                title = document.title,
                ownerName = userDao.findById(document.ownerId)?.fullName.orEmpty(),
                versionNumber = version.versionNumber,
                pdfPath = version.signedPath ?: version.originalPath,
                pageCount = version.pageCount,
                me = me,
                signers = signers,
                fields = dao.getFields(version.id).map { it.toDomain() },
                documentStatus = status,
            )
        }

    override suspend fun markViewed(linkId: String, userId: String) {
        withContext(Dispatchers.IO) {
            val link = dao.getSignerLink(linkId)?.takeIf { it.userId == userId } ?: return@withContext
            if (link.viewedAt != null) return@withContext
            val version = dao.getVersionById(link.versionId) ?: return@withContext
            val user = userDao.findById(userId) ?: return@withContext
            val now = clock()
            db.withTransaction {
                dao.updateSigner(link.copy(viewedAt = now))
                dao.insertEvent(event(version.documentId, version.versionNumber, EventType.VIEWED, userId, "Aberto por ${user.fullName}", now))
            }
        }
    }

    override suspend fun sign(linkId: String, userId: String, signaturePng: File): SignOutcome =
        withContext(Dispatchers.IO) {
            try {
                val link = dao.getSignerLink(linkId)?.takeIf { it.userId == userId }
                    ?: return@withContext SignOutcome.Failure("Documento não encontrado.")
                if (SignerStatus.from(link.status) == SignerStatus.SIGNED) {
                    return@withContext SignOutcome.Failure("Você já assinou este documento.")
                }
                val version = dao.getVersionById(link.versionId)
                    ?: return@withContext SignOutcome.Failure("Documento não encontrado.")
                val document = dao.getDocument(version.documentId)
                    ?: return@withContext SignOutcome.Failure("Documento não encontrado.")
                val status = DocumentStatus.from(document.status)
                if (document.currentVersion != version.versionNumber ||
                    (status != DocumentStatus.SENT && status != DocumentStatus.PARTIALLY_SIGNED)
                ) {
                    return@withContext SignOutcome.Failure("Este documento não está disponível para assinatura.")
                }
                val user = userDao.findById(userId) ?: return@withContext SignOutcome.Failure("Usuário não encontrado.")
                val myFields = dao.getFieldsForSigner(linkId).map { it.toDomain() }
                if (myFields.isEmpty()) return@withContext SignOutcome.Failure("Não há campos de assinatura para você.")

                val signers = dao.getSigners(version.id)
                val alreadySigned = signers.count { SignerStatus.from(it.status) == SignerStatus.SIGNED }
                val now = clock()

                // 1. Guarda a imagem da assinatura junto do documento
                val signatureFile = storage.signatureFile(document.id, linkId)
                signatureFile.parentFile?.mkdirs()
                signaturePng.copyTo(signatureFile, overwrite = true)

                // 2. Carimba a assinatura sobre o último PDF (o original nunca é alterado)
                val input = File(version.signedPath ?: version.originalPath)
                val stamped = storage.signedFile(document.id, version.versionNumber, alreadySigned + 1)
                val role = SignerRole.from(link.role)
                val label = "${user.fullName} · ${role.label} · ${Formatters.fullDateTime(now)} · ${version.sha256.take(8)}"
                stamper.stampSignature(input, stamped, signatureFile, myFields, label)

                // 3. Se era o último obrigatório, acrescenta a página de registro de assinaturas
                val remainingRequired = signers.count {
                    it.required && it.id != linkId && SignerStatus.from(it.status) != SignerStatus.SIGNED
                }
                val completed = remainingRequired == 0
                var finalFile = stamped
                if (completed) {
                    val rows = dao.getSignerRows(version.id)
                    val auditSigners = rows.map { row ->
                        val signedAt = if (row.linkId == linkId) now else row.signedAt
                        AuditSigner(
                            name = row.fullName,
                            email = row.email,
                            role = SignerRole.from(row.role).label,
                            signedAt = signedAt?.let { Formatters.fullDateTime(it) } ?: "não assinou (opcional)",
                        )
                    }
                    finalFile = storage.finalFile(document.id, version.versionNumber)
                    stamper.appendAuditPage(
                        stamped,
                        finalFile,
                        AuditInfo(
                            title = document.title,
                            documentId = document.id,
                            versionNumber = version.versionNumber,
                            originalSha256 = version.sha256,
                            ownerName = userDao.findById(document.ownerId)?.fullName.orEmpty(),
                            sentAt = document.sentAt?.let { Formatters.fullDateTime(it) } ?: "-",
                            completedAt = Formatters.fullDateTime(now),
                            signers = auditSigners,
                        ),
                    )
                }
                val finalHash = if (completed) finalFile.sha256Hex() else null

                // 4. Atualiza o status do registro numa única transação
                db.withTransaction {
                    dao.updateSigner(
                        link.copy(
                            status = SignerStatus.SIGNED.name,
                            signedAt = now,
                            viewedAt = link.viewedAt ?: now,
                            signatureImagePath = signatureFile.absolutePath,
                        ),
                    )
                    dao.updateVersion(version.copy(signedPath = finalFile.absolutePath, signedSha256 = finalHash))
                    dao.insertEvent(
                        event(document.id, version.versionNumber, EventType.SIGNED, userId, "${user.fullName} assinou como ${role.label.lowercase()}", now),
                    )
                    if (completed) {
                        dao.updateDocument(document.copy(status = DocumentStatus.COMPLETED.name, completedAt = now, updatedAt = now))
                        dao.insertEvent(
                            event(document.id, version.versionNumber, EventType.COMPLETED, null, "Todos os signatários obrigatórios assinaram", now),
                        )
                    } else {
                        dao.updateDocument(document.copy(status = DocumentStatus.PARTIALLY_SIGNED.name, updatedAt = now))
                    }
                }
                SignOutcome.Success(completed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SignOutcome.Failure("Não foi possível salvar a assinatura. Tente novamente.")
            }
        }

    // ------------------------------------------------------------------ Apoio

    /** Garante que o documento é do usuário da sessão e ainda está em rascunho (editável). */
    private suspend fun requireDraft(documentId: String, ownerId: String): Pair<DocumentEntity, DocumentVersionEntity> {
        val document = dao.getDocument(documentId)?.takeIf { it.ownerId == ownerId }
            ?: throw DomainException("Documento não encontrado.")
        if (DocumentStatus.from(document.status) != DocumentStatus.DRAFT) {
            throw DomainException("Documento enviado é somente leitura. Crie uma nova versão para alterar.")
        }
        val version = dao.getVersion(document.id, document.currentVersion)
            ?: throw DomainException("Versão do documento não encontrada.")
        return document to version
    }

    private suspend fun requireOwnSigner(ownerId: String, userId: String) {
        val user = userDao.findById(userId)
        if (user == null || user.role != UserRole.SIGNER.name || user.createdBy != ownerId) {
            throw DomainException("Signatário não encontrado.")
        }
    }

    private suspend fun touch(document: DocumentEntity) {
        val fresh = dao.getDocument(document.id) ?: return
        dao.updateDocument(fresh.copy(updatedAt = clock()))
    }

    private fun event(
        documentId: String,
        versionNumber: Int,
        type: EventType,
        actorId: String?,
        description: String,
        at: Long,
    ) = DocumentEventEntity(newId(), documentId, versionNumber, type.name, actorId, description, at)

    private fun newId() = UUID.randomUUID().toString()

    private class SimulatedSendFailure : Exception()

    companion object {
        const val DEFAULT_INVITE =
            "Olá! Você recebeu um documento para assinar pelo selo. Abra o app, confira o conteúdo e assine nos campos indicados."
        private const val TITLE_LIMIT = 120
        private const val INVITE_LIMIT = 500
        private const val SIMULATED_FAILURE = "A conexão caiu antes de confirmar o envio (falha simulada no modo desenvolvedor)."
        private const val GENERIC_FAILURE = "Não foi possível registrar o envio agora."
    }
}
