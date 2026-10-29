package br.com.fiap.letssign.feature.documents.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.ui.components.WizardHeader
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Purple600

/** Telas 06 a 10 · Assistente "Novo documento" em 5 passos, com rascunho salvo automaticamente. */
@Composable
fun DocumentWizardScreen(
    documentId: String?,
    onExit: () -> Unit,
    onOpenTracking: (String) -> Unit,
    onGoHome: () -> Unit,
    onPreview: (String) -> Unit,
    onRegisterSigner: () -> Unit,
) {
    val viewModel = seloViewModel {
        WizardViewModel(
            initialDocumentId = documentId,
            session = it.session,
            repository = it.documentRepository,
            inspector = it.pdfInspector,
            signerRepository = it.signerRepository,
            validate = it.validateDocument,
            sendDocument = it.sendDocument,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) onExit()
    }
    LaunchedEffect(state.openTrackingId) {
        val id = state.openTrackingId ?: return@LaunchedEffect
        viewModel.consumeTracking()
        onOpenTracking(id)
    }
    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.consumeMessage()
    }

    val requestClose: () -> Unit = {
        if (state.documentId == null) {
            onExit()
        } else {
            showExitDialog = true
        }
    }

    BackHandler {
        if (state.step == WizardStep.DETAILS) requestClose() else viewModel.back()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lavender50)
            .imePadding(),
    ) {
        if (state.step == WizardStep.RESULT) {
            StepResult(
                state = state,
                onRetry = viewModel::retrySend,
                onBackToReview = viewModel::backToReview,
                onTrack = { state.documentId?.let(onOpenTracking) },
                onHome = onGoHome,
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                WizardHeader(
                    step = state.step,
                    stepName = WizardStep.name(state.step),
                    draftSaved = state.documentId != null,
                    onBack = { if (state.step == WizardStep.DETAILS) requestClose() else viewModel.back() },
                    onClose = requestClose,
                )
                Box(Modifier.weight(1f)) {
                    when (state.step) {
                        WizardStep.DETAILS -> StepDetails(
                            state = state,
                            onTitleChange = viewModel::onTitleChange,
                            onPdfPicked = viewModel::pickPdf,
                            onContinue = viewModel::continueFromDetails,
                        )
                        WizardStep.SIGNERS -> StepSigners(
                            state = state,
                            onAdd = viewModel::addSigner,
                            onReplace = viewModel::replaceSigner,
                            onRemove = viewModel::removeSigner,
                            onRole = viewModel::setRole,
                            onRegisterSigner = onRegisterSigner,
                            onContinue = viewModel::continueFromSigners,
                        )
                        WizardStep.FIELDS -> StepFields(
                            state = state,
                            onSelectSigner = viewModel::selectSigner,
                            onSelectPage = viewModel::selectPage,
                            onSelectField = viewModel::selectField,
                            onAddField = viewModel::addField,
                            onMoveField = viewModel::moveField,
                            onDeleteField = viewModel::deleteField,
                            onReview = viewModel::toReview,
                        )
                        else -> StepReview(
                            state = state,
                            onInviteChange = viewModel::onInviteChange,
                            onToggleSimulation = viewModel::toggleSimulateFailure,
                            onPreview = { state.documentId?.let(onPreview) },
                            onSend = viewModel::confirmSend,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color.White,
            title = { Text("Sair do novo documento?") },
            text = { Text("O rascunho fica salvo em Rascunhos na tela inicial e você pode continuar depois.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) { Text("Sair e manter rascunho", color = Purple600) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.deleteDraftAndExit()
                }) { Text("Excluir rascunho", color = ErrorFg) }
            },
        )
    }
}
