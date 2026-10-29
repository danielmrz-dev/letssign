package br.com.fiap.letssign.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.fiap.letssign.core.database.UserDao
import br.com.fiap.letssign.core.database.toDomain
import br.com.fiap.letssign.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** Guarda o usuário logado no DataStore: ao reabrir o app, a sessão continua. */
class SessionManager(
    context: Context,
    private val userDao: UserDao,
) {
    private val dataStore = context.applicationContext.sessionDataStore
    private val userIdKey = stringPreferencesKey("user_id")

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** Lê a sessão salva. Retorna null se não houver sessão válida. */
    suspend fun restore(): User? {
        val id = dataStore.data.first()[userIdKey] ?: return null
        val user = userDao.findById(id)?.toDomain()
        if (user == null) clear() else _currentUser.value = user
        return user
    }

    suspend fun start(user: User) {
        dataStore.edit { it[userIdKey] = user.id }
        _currentUser.value = user
    }

    suspend fun clear() {
        dataStore.edit { it.remove(userIdKey) }
        _currentUser.value = null
    }

    /** Usuário da sessão; toda consulta de dados é filtrada por ele. */
    suspend fun requireUser(): User =
        _currentUser.value ?: restore() ?: throw IllegalStateException("Sessão expirada. Entre novamente.")
}
