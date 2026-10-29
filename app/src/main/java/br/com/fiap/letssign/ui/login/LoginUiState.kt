package br.com.fiap.letssign.ui.login

/**
 * Estado imutável da interface de Login.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isPasswordVisible: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotBlank()
}
