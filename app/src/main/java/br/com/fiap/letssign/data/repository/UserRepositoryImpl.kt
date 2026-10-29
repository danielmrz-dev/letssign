package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.model.UserRole

/**
 * Implementação do repositório utilizando o Room UserDao local e o PasswordHasher.
 */
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher
) : UserRepository {

    override suspend fun getUserByEmail(email: String): UserEntity? {
        seedDatabaseIfEmpty()
        return userDao.getByEmail(email)
    }

    override suspend fun getUserById(id: Long): UserEntity? {
        seedDatabaseIfEmpty()
        return userDao.getById(id)
    }

    override suspend fun insertUser(user: UserEntity): Long {
        return userDao.insert(user)
    }

    override suspend fun getUserCount(): Int {
        return userDao.count()
    }

    override suspend fun seedDatabaseIfEmpty() {
        if (userDao.count() == 0) {
            // Admin inicial: Bruna Carvalho (admin@letssign.com / admin1234)
            val adminPassword = passwordHasher.hashPassword("admin1234")
            val adminUser = UserEntity(
                fullName = "Bruna Carvalho",
                email = "admin@letssign.com",
                passwordHash = adminPassword.hash,
                salt = adminPassword.salt,
                role = UserRole.ADMIN
            )
            userDao.insert(adminUser)

            // Signatário de demonstração: Maria Souza (maria@studionorte.com.br / maria1234)
            val signerPassword = passwordHasher.hashPassword("maria1234")
            val signerUser = UserEntity(
                fullName = "Maria Souza",
                email = "maria@studionorte.com.br",
                phone = "(11) 98765-4321",
                passwordHash = signerPassword.hash,
                salt = signerPassword.salt,
                role = UserRole.SIGNATARIO
            )
            userDao.insert(signerUser)
        }
    }
}
