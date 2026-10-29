package br.com.fiap.letssign.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.session.SessionManager
import br.com.fiap.letssign.domain.usecase.AuthResult
import br.com.fiap.letssign.domain.usecase.AuthenticateUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel que controla o estado da tela de Login e a execução da autenticação.
 */
class LoginViewModel(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                loginError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                loginError = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update {
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }

    fun onFillDemoCredentials(email: String, password: String) {
        _uiState.update {
            it.copy(
                email = email,
                password = password,
                emailError = null,
                passwordError = null,
                loginError = null
            )
        }
    }

    fun onLoginClicked() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        // Validação local de formato prévia
        var hasFormatError = false
        var emailErrorMsgKey: String? = null
        var passwordErrorMsgKey: String? = null

        val trimmedEmail = currentState.email.trim()
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            hasFormatError = true
            emailErrorMsgKey = "invalid_email_format"
        }

        if (currentState.password.isEmpty()) {
            hasFormatError = true
            passwordErrorMsgKey = "empty_password"
        }

        if (hasFormatError) {
            _uiState.update {
                it.copy(
                    emailError = emailErrorMsgKey,
                    passwordError = passwordErrorMsgKey
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                loginError = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = authenticateUserUseCase(
                rawEmail = currentState.email,
                rawPassword = currentState.password
            )

            when (result) {
                is AuthResult.Success -> {
                    val user = result.user
                    // Salva a sessão ativa no DataStore
                    sessionManager.saveSession(user.id.toString(), user.role)

                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.NavigateToHome(user.role))
                }

                is AuthResult.InvalidCredentials -> {
                    // Erro genérico para proteger a identidade dos e-mails
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginError = "invalid_credentials"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Factory para instanciação do LoginViewModel.
 */
class LoginViewModelFactory(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authenticateUserUseCase, sessionManager) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
