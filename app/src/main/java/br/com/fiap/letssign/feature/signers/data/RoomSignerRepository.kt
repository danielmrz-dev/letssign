package br.com.fiap.letssign.feature.signers.data

import br.com.fiap.letssign.core.database.UserDao
import br.com.fiap.letssign.core.database.UserEntity
import br.com.fiap.letssign.core.database.toDomain
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.model.UserRole
import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.core.util.Validators
import br.com.fiap.letssign.feature.signers.domain.SaveSignerResult
import br.com.fiap.letssign.feature.signers.domain.SignerFormField
import br.com.fiap.letssign.feature.signers.domain.SignerInput
import br.com.fiap.letssign.feature.signers.domain.SignerRepository
import br.com.fiap.letssign.feature.signers.domain.SignerValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomSignerRepository(
    private val userDao: UserDao,
    private val hasher: PasswordHasher,
) : SignerRepository {

    override fun observeSigners(adminId: String): Flow<List<User>> =
        userDao.observeSigners(adminId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSigner(adminId: String, signerId: String): User? {
        val entity = userDao.findById(signerId) ?: return null
        return if (entity.createdBy == adminId) entity.toDomain() else null
    }

    override suspend fun create(adminId: String, input: SignerInput): SaveSignerResult = withContext(Dispatchers.Default) {
        val errors = SignerValidator.validate(input, passwordRequired = true).toMutableMap()
        val email = Validators.normalizeEmail(input.email)
        if (SignerFormField.EMAIL !in errors && userDao.findByEmail(email) != null) {
            errors[SignerFormField.EMAIL] = "Já existe um cadastro com este e-mail."
        }
        if (errors.isNotEmpty()) return@withContext SaveSignerResult.Invalid(errors)

        val now = System.currentTimeMillis()
        val salt = hasher.newSalt()
        userDao.insert(
            UserEntity(
                id = UUID.randomUUID().toString(),
                fullName = input.fullName.trim().replace(Regex("\\s+"), " "),
                email = email,
                phone = input.phoneDigits.filter { it.isDigit() },
                passwordHash = hasher.hash(input.password, salt),
                salt = salt,
                role = UserRole.SIGNER.name,
                colorIndex = userDao.countSigners(adminId),
                createdBy = adminId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        SaveSignerResult.Success
    }

    override suspend fun update(adminId: String, signerId: String, input: SignerInput): SaveSignerResult =
        withContext(Dispatchers.Default) {
            val current = userDao.findById(signerId)
            if (current == null || current.createdBy != adminId) {
                return@withContext SaveSignerResult.Invalid(mapOf(SignerFormField.NAME to "Signatário não encontrado."))
            }
            val errors = SignerValidator.validate(input, passwordRequired = false).toMutableMap()
            val email = Validators.normalizeEmail(input.email)
            val owner = userDao.findByEmail(email)
            if (SignerFormField.EMAIL !in errors && owner != null && owner.id != signerId) {
                errors[SignerFormField.EMAIL] = "Já existe um cadastro com este e-mail."
            }
            if (errors.isNotEmpty()) return@withContext SaveSignerResult.Invalid(errors)

            val newSalt = if (input.password.isNotEmpty()) hasher.newSalt() else current.salt
            userDao.update(
                current.copy(
                    fullName = input.fullName.trim().replace(Regex("\\s+"), " "),
                    email = email,
                    phone = input.phoneDigits.filter { it.isDigit() },
                    salt = newSalt,
                    passwordHash = if (input.password.isNotEmpty()) hasher.hash(input.password, newSalt) else current.passwordHash,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            SaveSignerResult.Success
        }
}
