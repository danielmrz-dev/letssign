package br.com.fiap.letssign.feature.signing.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.SignOutcome
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.model.SigningSession
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.feature.signing.domain.SignDocumentUseCase
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.PdfPage
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.components.SecondaryButton
import br.com.fiap.letssign.ui.components.findFragmentActivity
import br.com.fiap.letssign.ui.components.rememberPdfLoader
import br.com.fiap.letssign.ui.components.rememberPdfPageSizes
import br.com.fiap.letssign.ui.components.requestSignatureConfirmation
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.Purple700
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SignUiState(
    val loading: Boolean = true,
    val session: SigningSession? = null,
    val signaturePath: String? = null,
    val signing: Boolean = false,
    val completedDocument: Boolean? = null,
    val error: String? = null,
) {
    val myFields: List<SignatureField>
        get() = session?.let { s -> s.fields.filter { it.signerLinkId == s.me.linkId } }
            .orEmpty()
            .sortedWith(compareBy({ it.pageIndex }, { it.yRatio }))
    val alreadySigned: Boolean get() = session?.me?.status == SignerStatus.SIGNED
}

class SignDocumentViewModel(
    private val linkId: String,
    private val session: SessionManager,
    private val repository: DocumentRepository,
    private val signDocument: SignDocumentUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SignUiState())
    val state: StateFlow<SignUiState> = _state

    init {
        viewModelScope.launch {
            val user = session.requireUser()
            val loaded = repository.loadSigningSession(linkId, user.id)
            _state.update { it.copy(loading = false, session = loaded) }
            if (loaded != null) repository.markViewed(linkId, user.id)
        }
    }

    fun setSignature(path: String?) {
        if (path != null) _state.update { it.copy(signaturePath = path, error = null) }
    }

    fun sign() {
        val path = _state.value.signaturePath ?: return
        if (_state.value.signing) return
        viewModelScope.launch {
            _state.update { it.copy(signing = true, error = null) }
            val user = session.requireUser()
            when (val outcome = signDocument(linkId, user.id, File(path))) {
                is SignOutcome.Success -> _state.update { it.copy(signing = false, completedDocument = outcome.completed) }
                is SignOutcome.Failure -> _state.update { it.copy(signing = false, error = outcome.message) }
            }
        }
    }

    fun consumeError() = _state.update { it.copy(error = null) }
}

/** Tela 13 · Assinar documento: só os campos do signatário ficam ativos. */
@Composable
fun SignDocumentScreen(
    linkId: String,
    signaturePath: String?,
    onBack: () -> Unit,
    onCaptureSignature: () -> Unit,
) {
    val viewModel = seloViewModel { SignDocumentViewModel(linkId, it.session, it.documentRepository, it.signDocument) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var nextFieldIndex by remember { mutableIntStateOf(0) }
    var manualConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(signaturePath) { viewModel.setSignature(signaturePath) }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    val signingSession = state.session
    val loader = rememberPdfLoader(signingSession?.pdfPath)
    val sizes = rememberPdfPageSizes(loader)
    val myFields = state.myFields

    // Abre direto na página do primeiro campo do signatário
    LaunchedEffect(sizes, myFields.firstOrNull()?.id) {
        val first = myFields.firstOrNull() ?: return@LaunchedEffect
        if (sizes != null) listState.scrollToItem(first.pageIndex)
    }

    val signatureBitmap by produceState<Bitmap?>(initialValue = null, state.signaturePath) {
        value = state.signaturePath?.let { path -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) } }
    }
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    val confirmAndSign: () -> Unit = {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            manualConfirm = true
        } else {
            requestSignatureConfirmation(
                activity = activity,
                onConfirmed = viewModel::sign,
                onUnavailable = { manualConfirm = true },
                onError = { message -> scope.launch { snackbar.showSnackbar(message) } },
            )
        }
    }

    if (state.completedDocument != null) {
        SignSuccess(completed = state.completedDocument == true, onDone = onBack)
        return
    }

    Box(Modifier.fillMaxSize().background(Lavender100)) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = signingSession?.title ?: "Documento",
                subtitle = signingSession?.let { "Página $currentPage de ${it.pageCount}" },
                onBack = onBack,
            )
            when {
                state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple600)
                }
                signingSession == null -> Box(Modifier.weight(1f)) {
                    EmptyState("Documento não disponível para assinatura ou você não tem acesso a ele.")
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1EAFF))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Purple700, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when {
                                state.alreadySigned -> "Você já assinou este documento"
                                state.signaturePath != null -> "Assinatura aplicada em todos os seus campos"
                                else -> "Toque no seu campo para assinar"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple700,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${if (state.signaturePath != null) myFields.size else 0}/${myFields.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Purple700,
                        )
                    }
                    if (loader == null || sizes == null) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Purple600)
                        }
                    } else {
                        val signersById = signingSession.signers.associateBy { it.linkId }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(sizes) { index, size ->
                                PdfPage(loader = loader, pageIndex = index, aspectRatio = size.aspectRatio) {
                                    signingSession.fields.filter { it.pageIndex == index }.forEach { field ->
                                        val owner = signersById[field.signerLinkId]
                                        val mine = field.signerLinkId == signingSession.me.linkId
                                        val fieldModifier = Modifier
                                            .offset(x = maxWidth * field.xRatio, y = maxHeight * field.yRatio)
                                            .size(maxWidth * field.widthRatio, maxHeight * field.heightRatio)
                                        when {
                                            mine && !state.alreadySigned -> MyField(
                                                modifier = fieldModifier,
                                                signature = signatureBitmap,
                                                onClick = onCaptureSignature,
                                            )
                                            !mine && owner?.status != SignerStatus.SIGNED -> LockedField(
                                                modifier = fieldModifier,
                                                name = owner?.fullName?.substringBefore(' ').orEmpty(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (signingSession != null && !state.alreadySigned) {
                BottomActionBar {
                    SecondaryButton(
                        text = "Próximo campo",
                        icon = Icons.Filled.ArrowDownward,
                        enabled = myFields.isNotEmpty(),
                        onClick = {
                            if (myFields.isNotEmpty()) {
                                val target = myFields[nextFieldIndex % myFields.size]
                                nextFieldIndex = (nextFieldIndex + 1) % myFields.size
                                scope.launch { listState.animateScrollToItem(target.pageIndex) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = "Concluir",
                        onClick = confirmAndSign,
                        enabled = state.signaturePath != null,
                        loading = state.signing,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (manualConfirm) {
        AlertDialog(
            onDismissRequest = { manualConfirm = false },
            containerColor = Color.White,
            title = { Text("Confirmar assinatura") },
            text = {
                Text(
                    "Este aparelho não tem biometria nem PIN configurados. Confirme que você leu o documento " +
                        "e deseja assinar.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    manualConfirm = false
                    viewModel.sign()
                }) { Text("Assinar", color = Purple600) }
            },
            dismissButton = {
                TextButton(onClick = { manualConfirm = false }) { Text("Cancelar", color = Ink600) }
            },
        )
    }
}

@Composable
private fun MyField(modifier: Modifier, signature: Bitmap?, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Purple600.copy(alpha = if (signature == null) 0.12f else 0.05f))
            .border(2.dp, Purple600, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (signature != null) {
            Image(
                bitmap = signature.asImageBitmap(),
                contentDescription = "Sua assinatura",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Draw, contentDescription = null, tint = Purple600, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Toque para assinar",
                    color = Purple600,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LockedField(modifier: Modifier, name: String) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x14000000))
            .border(1.dp, Ink400, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Ink400, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("Campo de $name", color = Ink600, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SignSuccess(completed: Boolean, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(SuccessBg),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuccessFg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Documento assinado", style = MaterialTheme.typography.headlineMedium, color = Ink900)
        Spacer(Modifier.height(10.dp))
        Text(
            if (completed) {
                "Você foi o último a assinar: o documento está concluído e o PDF final já tem o registro de assinaturas."
            } else {
                "Sua assinatura foi salva e o status foi atualizado. Agora faltam os demais signatários."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Ink600,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Voltar aos documentos", onClick = onDone)
    }
}
