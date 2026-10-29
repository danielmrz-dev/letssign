package br.com.fiap.letssign.di

import android.content.Context
import br.com.fiap.letssign.core.database.DatabaseSeeder
import br.com.fiap.letssign.core.database.SeloDatabase
import br.com.fiap.letssign.core.pdf.PdfInspector
import br.com.fiap.letssign.core.pdf.PdfStamper
import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.storage.DocumentStorage
import br.com.fiap.letssign.feature.auth.data.RoomAuthRepository
import br.com.fiap.letssign.feature.auth.domain.AuthRepository
import br.com.fiap.letssign.feature.documents.data.RoomDocumentRepository
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.feature.documents.domain.SendDocumentUseCase
import br.com.fiap.letssign.feature.documents.domain.ValidateDocumentUseCase
import br.com.fiap.letssign.feature.signers.data.RoomSignerRepository
import br.com.fiap.letssign.feature.signers.domain.SignerRepository
import br.com.fiap.letssign.feature.signing.domain.SignDocumentUseCase

/**
 * Injeção de dependências manual. As telas dependem só das interfaces dos repositórios:
 * na Sprint 2 basta trocar RoomDocumentRepository por uma implementação via API REST aqui.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SeloDatabase = SeloDatabase.build(appContext)
    val passwordHasher = PasswordHasher()
    val storage = DocumentStorage(appContext)
    val pdfInspector = PdfInspector(appContext, storage)
    val session = SessionManager(appContext, database.userDao())
    val seeder = DatabaseSeeder(database.userDao(), passwordHasher)

    val authRepository: AuthRepository = RoomAuthRepository(database.userDao(), passwordHasher, session)
    val signerRepository: SignerRepository = RoomSignerRepository(database.userDao(), passwordHasher)
    val documentRepository: DocumentRepository = RoomDocumentRepository(database, storage, PdfStamper())

    val validateDocument = ValidateDocumentUseCase()
    val sendDocument = SendDocumentUseCase(validateDocument, documentRepository)
    val signDocument = SignDocumentUseCase(documentRepository)
}
