package br.com.fiap.letssign.feature.tracking.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.DocumentEvent
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.DomainException
import br.com.fiap.letssign.core.model.EventType
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.DocumentStatusChip
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.components.SectionCard
import br.com.fiap.letssign.ui.components.SectionLabel
import br.com.fiap.letssign.ui.components.SignerStatusChip
import br.com.fiap.letssign.ui.components.ThinProgress
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.SeloFonts
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class TrackingUiState(
    val loading: Boolean = true,
    val detail: DocumentDetail? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModel(
    private val documentId: String,
    private val session: SessionManager,
    private val repository: DocumentRepository,
) : ViewModel() {

    val state: StateFlow<TrackingUiState> = flow { emit(session.requireUser()) }
        .flatMapLatest { owner -> repository.observeDetail(documentId, owner.id) }
        .map { TrackingUiState(loading = false, detail = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingUiState())

    val message = MutableStateFlow<String?>(null)
    val newVersionCreated = MutableStateFlow(false)

    fun createNewVersion() {
        viewModelScope.launch {
            try {
                repository.createNewVersion(documentId, session.requireUser().id)
                newVersionCreated.value = true
            } catch (e: DomainException) {
                message.value = e.message
            }
        }
    }
}

/** Tela 11 · Detalhe e acompanhamento do documento. */
@Composable
fun TrackingScreen(
    documentId: String,
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
    onEditNewVersion: () -> Unit,
) {
    val viewModel = seloViewModel { TrackingViewModel(documentId, it.session, it.documentRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val created by viewModel.newVersionCreated.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmNewVersion by remember { mutableStateOf(false) }

    LaunchedEffect(created) { if (created) onEditNewVersion() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.message.value = null
        }
    }

    val detail = state.detail
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lavender50),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = "Acompanhamento",
                onBack = onBack,
                trailing = {
                    if (detail != null && detail.status != DocumentStatus.DRAFT) {
                        TextButton(onClick = { confirmNewVersion = true }) {
                            Icon(Icons.Outlined.FileCopy, contentDescription = null, tint = Purple600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nova versão", color = Purple600, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
            )
            when {
                state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple600)
                }
                detail == null -> Box(Modifier.weight(1f)) {
                    EmptyState("Documento não encontrado ou você não tem acesso a ele.")
                }
                else -> TrackingContent(detail, modifier = Modifier.weight(1f))
            }
            if (detail != null) {
                val completed = detail.status == DocumentStatus.COMPLETED
                BottomActionBar {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(
                                text = "Abrir PDF final",
                                onClick = onOpenPdf,
                                icon = Icons.Outlined.Description,
                                enabled = completed,
                                modifier = Modifier.weight(1f),
                            )
                            if (completed) {
                                IconButton(
                                    onClick = { detail.version?.signedPath?.let { sharePdf(context, File(it), detail.title) } },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Lavender100),
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Compartilhar PDF", tint = Purple600)
                                }
                            }
                        }
                        if (!completed) {
                            Text(
                                "Disponível quando todos os signatários assinarem.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink600,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }

    if (confirmNewVersion) {
        AlertDialog(
            onDismissRequest = { confirmNewVersion = false },
            containerColor = Color.White,
            title = { Text("Criar nova versão?") },
            text = {
                Text(
                    "A versão enviada fica preservada como registro, somente leitura. A nova versão começa como " +
                        "rascunho, com o mesmo PDF, signatários e campos, e precisa ser enviada de novo.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewVersion = false
                    viewModel.createNewVersion()
                }) { Text("Criar nova versão", color = Purple600) }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewVersion = false }) { Text("Cancelar", color = Ink600) }
            },
        )
    }
}

@Composable
private fun TrackingContent(detail: DocumentDetail, modifier: Modifier = Modifier) {
    val version = detail.version
    val total = detail.signers.size
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DocumentStatusChip(detail.status)
                Spacer(Modifier.width(10.dp))
                if (detail.status != DocumentStatus.DRAFT) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Ink600, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    "v${version?.number ?: 1}" + if (detail.status != DocumentStatus.DRAFT) " · somente leitura" else " · rascunho",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(detail.title, style = MaterialTheme.typography.titleLarge, color = Ink900)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThinProgress(
                    progress = if (total == 0) 0f else detail.signedCount.toFloat() / total,
                    modifier = Modifier.weight(1f),
                    color = if (detail.status == DocumentStatus.COMPLETED) SuccessFg else Purple600,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${detail.signedCount} de $total ${if (detail.signedCount == 1) "assinou" else "assinaram"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink900,
                )
            }
            if (version != null) {
                Spacer(Modifier.height(14.dp))
                HashBox("Integridade · SHA-256 do original", version.sha256)
                val signedHash = version.signedSha256
                if (signedHash != null) {
                    Spacer(Modifier.height(8.dp))
                    HashBox("Documento final · SHA-256", signedHash)
                }
            }
        }

        SectionCard {
            SectionLabel("Signatários")
            Spacer(Modifier.height(6.dp))
            detail.signers.forEach { signer ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(signer.fullName, signer.colorIndex, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(signer.fullName, style = MaterialTheme.typography.titleSmall, color = Ink900)
                        Text(signer.role.label, style = MaterialTheme.typography.bodySmall, color = Ink600)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SignerStatusChip(signer.status)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            when {
                                signer.status == SignerStatus.SIGNED && signer.signedAt != null -> Formatters.dateTime(signer.signedAt)
                                signer.viewedAt != null -> "Visualizou ${Formatters.dateTime(signer.viewedAt)}"
                                detail.status == DocumentStatus.DRAFT -> "Ainda não enviado"
                                else -> "Aguardando"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink600,
                        )
                    }
                }
            }
        }

        SectionCard {
            SectionLabel("Linha do tempo")
            Spacer(Modifier.height(10.dp))
            detail.events.forEachIndexed { index, event ->
                TimelineItem(event, isLast = index == detail.events.lastIndex)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HashBox(label: String, hash: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Lavender50)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Verified, contentDescription = null, tint = SuccessFg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink900, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Text(hash, fontFamily = SeloFonts.Mono, fontSize = 11.5.sp, color = Ink900, lineHeight = 16.sp)
    }
}

@Composable
private fun TimelineItem(event: DocumentEvent, isLast: Boolean) {
    val (icon, tint, bg) = eventStyle(event.type)
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(Line),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 4.dp, bottom = if (isLast) 0.dp else 8.dp)) {
            Text(event.type.label, style = MaterialTheme.typography.titleSmall, color = Ink900)
            Text(Formatters.dateTime(event.createdAt), style = MaterialTheme.typography.bodySmall, color = Ink600)
            if (event.description.isNotBlank()) {
                Text(event.description, style = MaterialTheme.typography.bodySmall, color = Ink400)
            }
        }
    }
}

private fun eventStyle(type: EventType): Triple<ImageVector, Color, Color> = when (type) {
    EventType.CREATED -> Triple(Icons.Outlined.UploadFile, Ink600, Lavender100)
    EventType.SENT -> Triple(Icons.AutoMirrored.Outlined.Send, Purple600, Lavender100)
    EventType.SEND_FAILED -> Triple(Icons.Outlined.ErrorOutline, ErrorFg, Color(0xFFFDE8EC))
    EventType.VIEWED -> Triple(Icons.Outlined.Visibility, Ink600, Lavender100)
    EventType.SIGNED -> Triple(Icons.Outlined.Draw, Purple600, Lavender100)
    EventType.COMPLETED -> Triple(Icons.Outlined.CheckCircle, SuccessFg, SuccessBg)
    EventType.NEW_VERSION -> Triple(Icons.Outlined.FileCopy, Ink600, Lavender100)
}

/** Compartilha o PDF final pelo FileProvider (diferencial). */
fun sharePdf(context: Context, file: File, title: String) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF assinado").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
