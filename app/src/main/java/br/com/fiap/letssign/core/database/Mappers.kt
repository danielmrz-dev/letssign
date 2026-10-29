package br.com.fiap.letssign.core.database

import br.com.fiap.letssign.core.model.DocumentEvent
import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.DocumentVersion
import br.com.fiap.letssign.core.model.EventType
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.model.UserRole

fun UserEntity.toDomain() = User(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    role = if (role == UserRole.ADMIN.name) UserRole.ADMIN else UserRole.SIGNER,
    colorIndex = colorIndex,
)

fun SignatureFieldEntity.toDomain() = SignatureField(
    id = id,
    signerLinkId = signerLinkId,
    pageIndex = pageIndex,
    xRatio = xRatio,
    yRatio = yRatio,
    widthRatio = widthRatio,
    heightRatio = heightRatio,
)

fun SignatureField.toEntity(versionId: String) = SignatureFieldEntity(
    id = id,
    versionId = versionId,
    signerLinkId = signerLinkId,
    pageIndex = pageIndex,
    xRatio = xRatio,
    yRatio = yRatio,
    widthRatio = widthRatio,
    heightRatio = heightRatio,
)

fun SignerRow.toDomain() = DocumentSigner(
    linkId = linkId,
    userId = userId,
    fullName = fullName,
    email = email,
    colorIndex = colorIndex,
    role = SignerRole.from(role),
    order = orderIndex,
    required = required,
    status = SignerStatus.from(status),
    invitedAt = invitedAt,
    viewedAt = viewedAt,
    signedAt = signedAt,
)

fun DocumentVersionEntity.toDomain() = DocumentVersion(
    id = id,
    documentId = documentId,
    number = versionNumber,
    originalPath = originalPath,
    fileName = fileName,
    fileSize = fileSize,
    pageCount = pageCount,
    sha256 = sha256,
    signedPath = signedPath,
    signedSha256 = signedSha256,
    inviteMessage = inviteMessage,
    idempotencyKey = idempotencyKey,
)

fun DocumentEventEntity.toDomain() = DocumentEvent(
    id = id,
    type = EventType.from(type),
    description = description,
    createdAt = createdAt,
)
