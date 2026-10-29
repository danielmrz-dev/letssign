package br.com.fiap.letssign.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.database.DemoAccounts
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.auth.domain.AuthRepository
import br.com.fiap.letssign.ui.components.GradientHeader
import br.com.fiap.letssign.ui.components.InfoBanner
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.SecondaryButton
import br.com.fiap.letssign.ui.components.SeloTextField
import br.com.fiap.letssign.ui.components.SeloWordmark
import br.com.fiap.letssign.ui.theme.ErrorBg
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Purple700
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedUser: User? = null,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun fillDemo(admin: Boolean) = _state.update {
        if (admin) {
            it.copy(email = DemoAccounts.ADMIN_EMAIL, password = DemoAccounts.ADMIN_PASSWORD, error = null)
        } else {
            it.copy(email = DemoAccounts.SIGNER_EMAIL, password = DemoAccounts.SIGNER_PASSWORD, error = null)
        }
    }

    fun login() {
        val current = _state.value
        if (current.loading) return
        if (current.email.isBlank() || current.password.isEmpty()) {
            _state.update { it.copy(error = "Preencha e-mail e senha para entrar.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val user = runCatching { auth.login(current.email, current.password) }.getOrNull()
            _state.update {
                if (user == null) {
                    // Mensagem genérica: não diz qual dos campos está errado.
                    it.copy(loading = false, error = "E-mail ou senha incorretos. Confira os dados e tente de novo.")
                } else {
                    it.copy(loading = false, loggedUser = user)
                }
            }
        }
    }
}

/** Tela 02 · Login para os perfis Admin e Signatário. */
@Composable
fun LoginScreen(onLoggedIn: (User) -> Unit) {
    val viewModel = seloViewModel { LoginViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedUser) {
        state.loggedUser?.let(onLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            GradientHeader {
                Spacer(Modifier.height(12.dp))
                SeloWordmark(fontSize = 34.sp)
                Spacer(Modifier.height(28.dp))
                Text(
                    "Entre para preparar ou assinar seus documentos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Um só acesso para Admin e Signatário.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(36.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SeloTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmail,
                    label = "E-mail",
                    placeholder = "nome@empresa.com.br",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                SeloTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    label = "Senha",
                    placeholder = "Sua senha",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                                tint = Ink600,
                            )
                        }
                    },
                )
                state.error?.let {
                    InfoBanner(text = it, icon = Icons.Outlined.ErrorOutline, background = ErrorBg, contentColor = ErrorFg)
                }
                PrimaryButton(text = "Entrar", onClick = viewModel::login, loading = state.loading)

                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Lavender50)
                        .padding(16.dp),
                ) {
                    Text(
                        "ACESSO DE DEMONSTRAÇÃO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple700,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "Admin",
                            onClick = { viewModel.fillDemo(admin = true) },
                            icon = Icons.Outlined.AdminPanelSettings,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Signatário (Maria)",
                            onClick = { viewModel.fillDemo(admin = false) },
                            icon = Icons.Outlined.Person,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
                Spacer(Modifier.navigationBarsPadding().size(width = 1.dp, height = 8.dp))
            }
        }
    }
}
