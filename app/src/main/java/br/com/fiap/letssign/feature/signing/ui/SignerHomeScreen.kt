package br.com.fiap.letssign.feature.signing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.ReceivedDocument
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.auth.domain.AuthRepository
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.GradientHeader
import br.com.fiap.letssign.ui.components.Pill
import br.com.fiap.letssign.ui.components.SeloWordmark
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.Purple700
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SignerHomeUiState(
    val user: User? = null,
    val showSigned: Boolean = false,
    val documents: List<ReceivedDocument> = emptyList(),
    val loading: Boolean = true,
) {
    val pending get() = documents.filter { it.status == SignerStatus.PENDING }
    val signed get() = documents.filter { it.status == SignerStatus.SIGNED }
    val visible get() = if (showSigned) signed else pending
}

@OptIn(ExperimentalCoroutinesApi::class)
class SignerHomeViewModel(
    session: SessionManager,
    repository: DocumentRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val signedTab = MutableStateFlow(false)
    val loggedOut = MutableStateFlow(false)

    // Isolamento: só documentos enviados a este usuário (vínculo signatário x versão).
    val state: StateFlow<SignerHomeUiState> = flow { emit(session.requireUser()) }
        .flatMapLatest { user -> repository.observeReceived(user.id).map { user to it } }
        .combine(signedTab) { (user, list), selected ->
            SignerHomeUiState(user = user, showSigned = selected, documents = list, loading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignerHomeUiState())

    fun showSigned(value: Boolean) {
        signedTab.value = value
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            loggedOut.value = true
        }
    }
}

/** Tela 12 · Home do Signatário com as abas Pendentes e Assinados. */
@Composable
fun SignerHomeScreen(onOpen: (ReceivedDocument) -> Unit, onLoggedOut: () -> Unit) {
    val viewModel = seloViewModel { SignerHomeViewModel(it.session, it.documentRepository, it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lavender50),
    ) {
        GradientHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloWordmark()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = viewModel::logout) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair", tint = Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Olá, ${state.user?.firstName.orEmpty()}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                when (val count = state.pending.size) {
                    0 -> "Nenhum documento pendente de assinatura."
                    1 -> "Você tem 1 documento para assinar."
                    else -> "Você tem $count documentos para assinar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth().background(Color.White)) {
            UnderlineTab("Pendentes · ${state.pending.size}", selected = !state.showSigned, modifier = Modifier.weight(1f)) {
                viewModel.showSigned(false)
            }
            UnderlineTab("Assinados · ${state.signed.size}", selected = state.showSigned, modifier = Modifier.weight(1f)) {
                viewModel.showSigned(true)
            }
        }
        HorizontalDivider(color = Line)

        if (!state.loading && state.visible.isEmpty()) {
            EmptyState(
                if (state.showSigned) {
                    "Os documentos que você assinar aparecem aqui."
                } else {
                    "Tudo em dia! Quando alguém enviar um documento para você assinar, ele aparece aqui."
                },
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.visible, key = { it.signerLinkId }) { doc ->
                ReceivedCard(doc, onClick = { onOpen(doc) })
            }
        }
    }
}

@Composable
private fun UnderlineTab(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) Purple600 else Ink600,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) Purple600 else Color.Transparent),
        )
    }
}

@Composable
private fun ReceivedCard(doc: ReceivedDocument, onClick: () -> Unit) {
    val signed = doc.status == SignerStatus.SIGNED
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (signed) SuccessBg else Lavender100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (signed) Icons.Outlined.TaskAlt else Icons.Outlined.Draw,
                    contentDescription = null,
                    tint = if (signed) SuccessFg else Purple600,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, color = Ink900, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Enviado por ${doc.ownerName}" + (doc.sentAt?.let { " em ${Formatters.dateTime(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("Você: ${doc.role.label}", Lavender100, Purple700)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (signed) {
                            "Assinado em ${doc.signedAt?.let { Formatters.dateTime(it) }.orEmpty()}"
                        } else {
                            Formatters.plural(doc.fieldCount, "campo para assinar", "campos para assinar")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                    )
                }
            }
        }
    }
}
