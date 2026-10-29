package br.com.fiap.letssign.ui.login

import br.com.fiap.letssign.data.model.UserRole

/**
 * Eventos disparados pelo LoginViewModel para ação na UI.
 */
sealed interface LoginEvent {
    data class NavigateToHome(val role: UserRole) : LoginEvent
}
