package br.com.fiap.letssign.feature.signers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.signers.domain.SignerRepository
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Purple600
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class SignersUiState(
    val query: String = "",
    val all: List<User> = emptyList(),
    val loading: Boolean = true,
) {
    val filtered: List<User>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return all
            val qDigits = q.filter { it.isDigit() }
            return all.filter {
                it.fullName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    (qDigits.isNotEmpty() && it.phone.contains(qDigits))
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SignersViewModel(session: SessionManager, repository: SignerRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<SignersUiState> = flow { emit(session.requireUser()) }
        .flatMapLatest { admin -> repository.observeSigners(admin.id) }
        .combine(query) { list, q -> SignersUiState(query = q, all = list, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignersUiState())

    fun onQuery(value: String) {
        query.value = value
    }
}

/** Tela 04 · Lista e busca de signatários (somente Admin). */
@Composable
fun SignersScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val viewModel = seloViewModel { SignersViewModel(it.session, it.signerRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = "Signatários",
                onBack = onBack,
                trailing = {
                    Text(
                        Formatters.plural(state.all.size, "cadastrado", "cadastrados"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink600,
                    )
                },
            )
            TextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                placeholder = { Text("Buscar por nome, e-mail ou telefone", color = Ink400) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink600) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Lavender100,
                    unfocusedContainerColor = Lavender100,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Purple600,
                ),
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                items(state.filtered, key = { it.id }) { signer ->
                    SignerRow(signer = signer, onEdit = { onEdit(signer.id) })
                }
                item {
                    if (!state.loading && state.all.isEmpty()) {
                        EmptyState("Nenhum signatário cadastrado ainda. Toque em Adicionar para cadastrar quem vai assinar.")
                    } else if (!state.loading && state.filtered.isEmpty()) {
                        EmptyState("Nenhum signatário encontrado para \"${state.query}\". Confira a busca ou cadastre um novo.")
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            containerColor = Purple600,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
            text = { Text("Adicionar", style = MaterialTheme.typography.titleMedium) },
        )
    }
}

@Composable
private fun SignerRow(signer: User, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(signer.fullName, signer.colorIndex, size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(signer.fullName, style = MaterialTheme.typography.titleMedium, color = Ink900)
            Text(signer.email, style = MaterialTheme.typography.bodyMedium, color = Ink600, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Formatters.phone(signer.phone), style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editar ${signer.fullName}", tint = Ink600)
        }
    }
}
