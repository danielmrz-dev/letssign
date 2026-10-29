package br.com.fiap.letssign.feature.auth.data

import br.com.fiap.letssign.core.database.UserDao
import br.com.fiap.letssign.core.database.toDomain
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Validators
import br.com.fiap.letssign.feature.auth.domain.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomAuthRepository(
    private val userDao: UserDao,
    private val hasher: PasswordHasher,
    private val session: SessionManager,
) : AuthRepository {

    override suspend fun login(email: String, password: String): User? = withContext(Dispatchers.Default) {
        val entity = userDao.findByEmail(Validators.normalizeEmail(email))
        if (entity == null) {
            // Calcula um hash mesmo assim para não revelar, pelo tempo de resposta, se o e-mail existe.
            hasher.hash(password, hasher.newSalt())
            return@withContext null
        }
        if (!hasher.verify(password, entity.salt, entity.passwordHash)) return@withContext null
        val user = entity.toDomain()
        session.start(user)
        user
    }

    override suspend fun logout() {
        session.clear()
    }
}
