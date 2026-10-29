package br.com.fiap.letssign.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.model.UserRole
import br.com.fiap.letssign.data.session.SessionManager
import br.com.fiap.letssign.data.session.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Destinos de navegação possíveis calculados na Splash Screen.
 */
sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Login : SplashDestination
    data object AdminHome : SplashDestination
    data object SignerHome : SplashDestination
}

/**
 * ViewModel que gerencia a verificação de sessão e o tempo mínimo de 1,2s na tela de Splash.
 */
class SplashViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // Tenta obter a sessão salva; trata qualquer exceção como ausência de sessão
            val session = try {
                sessionManager.sessionFlow.first()
            } catch (e: Exception) {
                UserSession(userId = null, role = null)
            }

            // Garante o tempo mínimo de exibição de 1.200 ms (1,2s)
            val elapsedTime = System.currentTimeMillis() - startTime
            val minSplashDurationMs = 1200L
            val remainingDelayMs = minSplashDurationMs - elapsedTime
            if (remainingDelayMs > 0) {
                delay(remainingDelayMs)
            }

            // Redireciona com base na validade da sessão e perfil do usuário
            _destination.value = when {
                session.isValid && session.role == UserRole.ADMIN -> SplashDestination.AdminHome
                session.isValid && session.role == UserRole.SIGNATARIO -> SplashDestination.SignerHome
                else -> SplashDestination.Login
            }
        }
    }
}

/**
 * Factory simples para injeção manual do SessionManager no SplashViewModel.
 */
class SplashViewModelFactory(
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
