package br.com.fiap.letssign.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.fiap.letssign.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Modelo que representa os dados da sessão ativa.
 */
data class UserSession(
    val userId: String?,
    val role: UserRole?
) {
    val isValid: Boolean
        get() = !userId.isNullOrBlank() && role != null
}

/**
 * Encapsula o Preferences DataStore para persistência da sessão do usuário.
 */
class SessionManager(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }

    /**
     * Flow que expõe a sessão salva em tempo real garantindo o uso do ApplicationContext.
     */
    val sessionFlow: Flow<UserSession> = appContext.dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]
        val roleString = preferences[KEY_USER_ROLE]
        val role = roleString?.let {
            try {
                UserRole.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        UserSession(userId = userId, role = role)
    }

    /**
     * Salva as credenciais da sessão no DataStore.
     */
    suspend fun saveSession(userId: String, role: UserRole) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_ROLE] = role.name
        }
    }

    /**
     * Remove as credenciais salvas no DataStore.
     */
    suspend fun clearSession() {
        appContext.dataStore.edit { preferences ->
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_ROLE)
        }
    }
}
