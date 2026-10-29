package br.com.fiap.letssign.feature.signers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.signers.domain.SaveSignerResult
import br.com.fiap.letssign.feature.signers.domain.SignerFormField
import br.com.fiap.letssign.feature.signers.domain.SignerInput
import br.com.fiap.letssign.feature.signers.domain.SignerRepository
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.components.SeloTextField
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Purple600
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignerFormUiState(
    val isEdit: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: TextFieldValue = TextFieldValue(""),
    val password: String = "",
    val errors: Map<SignerFormField, String> = emptyMap(),
    val saved: Boolean = false,
)

class SignerFormViewModel(
    private val signerId: String?,
    private val session: SessionManager,
    private val repository: SignerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SignerFormUiState(isEdit = signerId != null, loading = signerId != null))
    val state: StateFlow<SignerFormUiState> = _state

    init {
        if (signerId != null) {
            viewModelScope.launch {
                val admin = session.requireUser()
                val signer = repository.getSigner(admin.id, signerId)
                _state.update {
                    if (signer == null) {
                        it.copy(loading = false, errors = mapOf(SignerFormField.NAME to "Signatário não encontrado."))
                    } else {
                        val phone = Formatters.phone(signer.phone)
                        it.copy(
                            loading = false,
                            name = signer.fullName,
                            email = signer.email,
                            phone = TextFieldValue(phone, TextRange(phone.length)),
                        )
                    }
                }
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value, errors = it.errors - SignerFormField.NAME) }
    fun onEmail(value: String) = _state.update { it.copy(email = value, errors = it.errors - SignerFormField.EMAIL) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, errors = it.errors - SignerFormField.PASSWORD) }

    /** Máscara de telefone: guarda só os dígitos e reformata, mantendo o cursor no fim. */
    fun onPhone(value: TextFieldValue) {
        val formatted = Formatters.phone(value.text)
        _state.update {
            it.copy(phone = TextFieldValue(formatted, TextRange(formatted.length)), errors = it.errors - SignerFormField.PHONE)
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val admin = session.requireUser()
            val input = SignerInput(
                fullName = current.name,
                email = current.email,
                phoneDigits = current.phone.text.filter { it.isDigit() },
                password = current.password,
            )
            val result = if (signerId == null) repository.create(admin.id, input) else repository.update(admin.id, signerId, input)
            _state.update {
                when (result) {
                    SaveSignerResult.Success -> it.copy(saving = false, saved = true)
                    is SaveSignerResult.Invalid -> it.copy(saving = false, errors = result.errors)
                }
            }
        }
    }
}

/** Tela 05 · Formulário de signatário (a mesma tela cria e edita). */
@Composable
fun SignerFormScreen(signerId: String?, onDone: () -> Unit) {
    val viewModel = seloViewModel { SignerFormViewModel(signerId, it.session, it.signerRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding(),
    ) {
        ScreenTopBar(
            title = if (state.isEdit) "Editar signatário" else "Novo signatário",
            onBack = onDone,
            closeIcon = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SeloTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = "Nome completo",
                placeholder = "Nome e sobrenome",
                error = state.errors[SignerFormField.NAME],
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            SeloTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = "E-mail",
                placeholder = "nome@empresa.com.br",
                error = state.errors[SignerFormField.EMAIL],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            SeloTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = "Telefone com DDD",
                placeholder = "(11) 91234-5678",
                error = state.errors[SignerFormField.PHONE],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
            SeloTextField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = "Senha",
                placeholder = if (state.isEdit) "Deixe em branco para manter a atual" else "Mínimo de 8 caracteres",
                error = state.errors[SignerFormField.PASSWORD],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = PasswordVisualTransformation(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Lavender50)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Purple600, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "A senha é guardada como hash com salt, nunca em texto puro. O signatário usa estes dados para entrar no app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                )
            }
        }
        BottomActionBar {
            PrimaryButton(
                text = "Salvar signatário",
                onClick = viewModel::save,
                loading = state.saving,
                enabled = !state.loading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
