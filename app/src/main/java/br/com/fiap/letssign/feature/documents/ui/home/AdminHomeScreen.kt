package br.com.fiap.letssign.feature.documents.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.DocumentSummary
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.auth.domain.AuthRepository
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.DocumentStatusChip
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.GradientHeader
import br.com.fiap.letssign.ui.components.SeloWordmark
import br.com.fiap.letssign.ui.components.ThinProgress
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
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

/** As 4 abas da home do Admin, na ordem do design. */
enum class AdminTab(val label: String, val status: DocumentStatus) {
    SENT("Enviados", DocumentStatus.SENT),
    PARTIAL("Parcialmente assinados", DocumentStatus.PARTIALLY_SIGNED),
    COMPLETED("Concluídos", DocumentStatus.COMPLETED),
    DRAFTS("Rascunhos", DocumentStatus.DRAFT),
}

data class AdminHomeUiState(
    val user: User? = null,
    val tab: AdminTab = AdminTab.SENT,
    val documents: List<DocumentSummary> = emptyList(),
    val loading: Boolean = true,
) {
    fun count(tab: AdminTab) = documents.count { it.status == tab.status }
    val visible: List<DocumentSummary> get() = documents.filter { it.status == tab.status }
    val awaiting: Int get() = count(AdminTab.SENT) + count(AdminTab.PARTIAL)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdminHomeViewModel(
    private val session: SessionManager,
    private val documents: DocumentRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val tab = MutableStateFlow(AdminTab.SENT)
    private val loggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = loggedOut

    private val userFlow = flow { emit(session.requireUser()) }

    val state: StateFlow<AdminHomeUiState> = userFlow
        .flatMapLatest { user ->
            // Consulta filtrada pelo ownerId = usuário da sessão
            documents.observeOwnerDocuments(user.id).map { user to it }
        }
        .combine(tab) { (user, list), selected ->
            AdminHomeUiState(user = user, tab = selected, documents = list, loading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminHomeUiState())

    fun selectTab(value: AdminTab) {
        tab.value = value
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            loggedOut.value = true
        }
    }
}

/** Tela 03 · Home do Admin. */
@Composable
fun AdminHomeScreen(
    onNewDocument: () -> Unit,
    onOpenDocument: (DocumentSummary) -> Unit,
    onOpenSigners: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val viewModel = seloViewModel { AdminHomeViewModel(it.session, it.documentRepository, it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loggedOut by viewModel.isLoggedOut.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lavender50),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeloWordmark()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSigners) {
                        Icon(Icons.Outlined.Group, contentDescription = "Signatários", tint = Color.White)
                    }
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Olá, ${state.user?.firstName.orEmpty()}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when (state.awaiting) {
                        0 -> "Nenhum documento aguardando assinatura."
                        1 -> "1 documento aguarda assinatura."
                        else -> "${state.awaiting} documentos aguardam assinatura."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AdminTab.entries) { tab ->
                    TabChip(
                        label = tab.label,
                        count = state.count(tab),
                        selected = tab == state.tab,
                        onClick = { viewModel.selectTab(tab) },
                    )
                }
            }

            if (!state.loading && state.visible.isEmpty()) {
                EmptyState(
                    when (state.tab) {
                        AdminTab.SENT -> "Nenhum documento aguardando assinaturas. Toque em Novo documento para enviar um PDF."
                        AdminTab.PARTIAL -> "Nenhum documento parcialmente assinado no momento."
                        AdminTab.COMPLETED -> "Os documentos aparecem aqui quando todos os signatários assinarem."
                        AdminTab.DRAFTS -> "Nenhum rascunho. Os documentos em preparação ficam salvos aqui."
                    },
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.visible, key = { it.id }) { doc ->
                    DocumentCard(doc, onClick = { onOpenDocument(doc) })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewDocument,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            containerColor = Purple600,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            icon = { Icon(Icons.Outlined.UploadFile, contentDescription = null) },
            text = { Text("Novo documento", style = MaterialTheme.typography.titleMedium) },
        )
    }
}

@Composable
private fun TabChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Purple600 else Color.White)
            .border(1.dp, if (selected) Purple600 else Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) Color.White else Ink900,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            color = if (selected) Color.White.copy(alpha = 0.85f) else Ink600,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DocumentCard(doc: DocumentSummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    doc.title.ifBlank { "Documento sem título" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink900,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                DocumentStatusChip(doc.status)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row {
                    doc.signers.take(4).forEachIndexed { index, badge ->
                        Avatar(
                            name = badge.name,
                            colorIndex = badge.colorIndex,
                            size = 28.dp,
                            modifier = Modifier
                                .offset(x = (-6 * index).dp)
                                .border(2.dp, Color.White, CircleShape),
                        )
                    }
                }
                if (doc.signers.isEmpty()) {
                    Text("Sem signatários", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
                Spacer(Modifier.width(8.dp))
                ThinProgress(
                    progress = if (doc.totalCount == 0) 0f else doc.signedCount.toFloat() / doc.totalCount,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${doc.signedCount}/${doc.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink900,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when (doc.status) {
                    DocumentStatus.DRAFT -> "Rascunho atualizado em ${Formatters.dateTime(doc.updatedAt)}"
                    DocumentStatus.COMPLETED -> "Concluído em ${Formatters.dateTime(doc.completedAt ?: doc.updatedAt)}"
                    else -> "Enviado em ${Formatters.dateTime(doc.sentAt ?: doc.updatedAt)}" +
                        if (doc.versionNumber > 1) " · v${doc.versionNumber}" else ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = Ink600,
            )
        }
    }
}
