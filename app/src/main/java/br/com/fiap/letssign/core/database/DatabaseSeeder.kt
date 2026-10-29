package br.com.fiap.letssign.core.database

import br.com.fiap.letssign.core.model.UserRole
import br.com.fiap.letssign.core.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/** Contas de demonstração (também listadas no README). */
object DemoAccounts {
    const val ADMIN_EMAIL = "admin@letssign.com"
    const val ADMIN_PASSWORD = "admin@2026"
    const val SIGNER_EMAIL = "maria@studionorte.com.br"
    const val SIGNER_PASSWORD = "selo@2026"
}

/** Cria o Admin padrão e alguns signatários no primeiro uso do app. */
class DatabaseSeeder(
    private val userDao: UserDao,
    private val hasher: PasswordHasher,
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (userDao.count() > 0) return@withLock
            val now = System.currentTimeMillis()
            val adminId = UUID.randomUUID().toString()
            userDao.insert(
                newUser(adminId, "Bruna Carvalho", DemoAccounts.ADMIN_EMAIL, "11912345678", DemoAccounts.ADMIN_PASSWORD, UserRole.ADMIN, 4, null, now),
            )
            val signers = listOf(
                Triple("Maria Souza", DemoAccounts.SIGNER_EMAIL, "11987654321"),
                Triple("João Pereira", "joao.pereira@gmail.com", "21996541122"),
                Triple("Ana Lima", "ana.lima@outlook.com", "31981112040"),
                Triple("Carlos Mendes", "carlos@mendesadv.com.br", "41997003311"),
            )
            signers.forEachIndexed { index, (name, email, phone) ->
                userDao.insert(
                    newUser(UUID.randomUUID().toString(), name, email, phone, DemoAccounts.SIGNER_PASSWORD, UserRole.SIGNER, index, adminId, now),
                )
            }
        }
    }

    private fun newUser(
        id: String,
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        colorIndex: Int,
        createdBy: String?,
        now: Long,
    ): UserEntity {
        val salt = hasher.newSalt()
        return UserEntity(
            id = id,
            fullName = name,
            email = email,
            phone = phone,
            passwordHash = hasher.hash(password, salt),
            salt = salt,
            role = role.name,
            colorIndex = colorIndex,
            createdBy = createdBy,
            createdAt = now,
            updatedAt = now,
        )
    }
}
