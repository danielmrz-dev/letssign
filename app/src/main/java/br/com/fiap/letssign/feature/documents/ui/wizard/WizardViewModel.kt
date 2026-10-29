package br.com.fiap.letssign.feature.documents.ui.wizard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.DocumentDetail
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.DomainException
import br.com.fiap.letssign.core.model.SendOutcome
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.pdf.PdfImportResult
import br.com.fiap.letssign.core.pdf.PdfInspector
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Validators
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.feature.documents.domain.SendDocumentUseCase
import br.com.fiap.letssign.feature.documents.domain.ValidateDocumentUseCase
import br.com.fiap.letssign.feature.signers.domain.SignerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Passos do assistente: 0 Dados · 1 Signatários · 2 Campos · 3 Revisão · 4 Envio (resultado). */
object WizardStep {
    const val DETAILS = 0
    const val SIGNERS = 1
    const val FIELDS = 2
    const val REVIEW = 3
    const val RESULT = 4

    fun name(step: Int) = when (step) {
        DETAILS -> "Dados"
        SIGNERS -> "Signatários"
        FIELDS -> "Campos"
        REVIEW -> "Revisão"
        else -> "Envio"
    }
}

data class WizardUiState(
    val step: Int = WizardStep.DETAILS,
    val documentId: String? = null,
    val loadingDraft: Boolean = false,
    val detail: DocumentDetail? = null,
    val title: String = "",
    val titleError: String? = null,
    val importing: Boolean = false,
    val importError: String? = null,
    val availableSigners: List<User> = emptyList(),
    val selectedLinkId: String? = null,
    val currentPage: Int = 0,
    val selectedFieldId: String? = null,
    val inviteMessage: String = "",
    val idempotencyKey: String? = null,
    val simulateFailure: Boolean = false,
    val sending: Boolean = false,
    val outcome: SendOutcome? = null,
    val issues: List<String> = emptyList(),
    val message: String? = null,
    val openTrackingId: String? = null,
    val exitRequested: Boolean = false,
) {
    val pageCount: Int get() = detail?.version?.pageCount ?: 0
    val signers get() = detail?.signers.orEmpty()
    val fields get() = detail?.fields.orEmpty()
    val canEdit: Boolean get() = detail == null || detail.status == DocumentStatus.DRAFT
}

@OptIn(ExperimentalCoroutinesApi::class)
class WizardViewModel(
    initialDocumentId: String?,
    private val session: SessionManager,
    private val repository: DocumentRepository,
    private val inspector: PdfInspector,
    private val signerRepository: SignerRepository,
    private val validate: ValidateDocumentUseCase,
    private val sendDocument: SendDocumentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WizardUiState(documentId = initialDocumentId, loadingDraft = initialDocumentId != null),
    )
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    private val documentId = MutableStateFlow(initialDocumentId)
    private var ownerId: String? = null
    private var titleJob: Job? = null
    private var inviteJob: Job? = null
    private var titleTouched = false
    private var inviteTouched = false

    init {
        viewModelScope.launch {
            val owner = owner()
            launch {
                signerRepository.observeSigners(owner).collect { list ->
                    _state.update { it.copy(availableSigners = list) }
                }
            }
            documentId
                .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeDetail(id, owner) }
                .collect { detail -> onDetail(detail) }
        }
        recomputeIssues()
    }

    private suspend fun owner(): String = ownerId ?: session.requireUser().id.also { ownerId = it }

    private fun onDetail(detail: DocumentDetail?) {
        _state.update { current ->
            var next = current.copy(detail = detail, loadingDraft = false)
            if (detail != null) {
                if (!titleTouched) next = next.copy(title = detail.title)
                if (!inviteTouched) next = next.copy(inviteMessage = detail.version?.inviteMessage.orEmpty())
                val links = detail.signers.map { it.linkId }
                if (next.selectedLinkId == null || next.selectedLinkId !in links) {
                    next = next.copy(selectedLinkId = links.firstOrNull())
                }
                if (next.selectedFieldId != null && detail.fields.none { it.id == next.selectedFieldId }) {
                    next = next.copy(selectedFieldId = null)
                }
                if (next.currentPage >= (detail.version?.pageCount ?: 1)) next = next.copy(currentPage = 0)
                // Documento já enviado não pode ser editado: vai para o acompanhamento.
                val notEditable = detail.status != DocumentStatus.DRAFT
                if (notEditable && !next.sending && next.outcome == null && next.step < WizardStep.RESULT) {
                    next = next.copy(openTrackingId = detail.id)
                }
            }
            next.copy(issues = issuesFor(next))
        }
    }

    private fun issuesFor(s: WizardUiState): List<String> {
        val detail = s.detail
        return if (detail == null) {
            validate.validate(s.title, hasPdf = false, pageCount = 0, signers = emptyList(), fields = emptyList(), inviteMessage = s.inviteMessage)
        } else {
            validate(detail, title = s.title, inviteMessage = s.inviteMessage)
        }
    }

    private fun recomputeIssues() = _state.update { it.copy(issues = issuesFor(it)) }

    /** Executa uma ação de edição e transforma regra violada em mensagem para o usuário. */
    private fun runAction(block: suspend (documentId: String, ownerId: String) -> Unit) {
        val id = documentId.value ?: return
        viewModelScope.launch {
            try {
                block(id, owner())
            } catch (e: DomainException) {
                _state.update { it.copy(message = e.message) }
            }
        }
    }

    private fun goTo(step: Int) = _state.update { it.copy(step = step, message = null) }

    // ------------------------------------------------------------- Passo 1 · Dados

    fun onTitleChange(value: String) {
        titleTouched = true
        val text = value.take(Validators.TITLE_MAX)
        _state.update { it.copy(title = text, titleError = null) }
        recomputeIssues()
        val id = documentId.value ?: return
        titleJob?.cancel()
        titleJob = viewModelScope.launch {
            delay(400)
            runCatching { repository.updateTitle(id, owner(), text) }
        }
    }

    fun pickPdf(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importing = true, importError = null) }
            when (val result = inspector.import(uri)) {
                is PdfImportResult.Error -> _state.update { it.copy(importing = false, importError = result.message) }
                is PdfImportResult.Success -> try {
                    val owner = owner()
                    val id = documentId.value
                    if (id == null) {
                        val suggested = _state.value.title.ifBlank {
                            result.pdf.fileName.substringBeforeLast('.').replace('_', ' ')
                        }.take(Validators.TITLE_MAX)
                        titleTouched = true
                        _state.update { it.copy(title = suggested) }
                        val newId = repository.createDraft(owner, suggested, result.pdf)
                        documentId.value = newId
                        _state.update { it.copy(documentId = newId, importing = false) }
                    } else {
                        repository.replaceDraftPdf(id, owner, result.pdf)
                        _state.update { it.copy(importing = false) }
                    }
                } catch (e: DomainException) {
                    _state.update { it.copy(importing = false, importError = e.message) }
                }
            }
        }
    }

    fun continueFromDetails() {
        val current = _state.value
        val titleError = Validators.title(current.title)
        if (titleError != null) {
            _state.update { it.copy(titleError = titleError) }
            return
        }
        if (current.detail?.version == null) {
            _state.update { it.copy(importError = "Selecione o arquivo PDF para continuar.") }
            return
        }
        flushTitle()
        goTo(WizardStep.SIGNERS)
    }

    private fun flushTitle() {
        val id = documentId.value ?: return
        titleJob?.cancel()
        val text = _state.value.title
        viewModelScope.launch { runCatching { repository.updateTitle(id, owner(), text) } }
    }

    // ------------------------------------------------------------- Passo 2 · Signatários

    fun addSigner(userId: String) = runAction { id, owner -> repository.addSigner(id, owner, userId) }

    fun replaceSigner(linkId: String, userId: String) =
        runAction { id, owner -> repository.replaceSigner(id, owner, linkId, userId) }

    fun removeSigner(linkId: String) = runAction { id, owner -> repository.removeSigner(id, owner, linkId) }

    fun setRole(linkId: String, role: SignerRole) =
        runAction { id, owner -> repository.updateSignerRole(id, owner, linkId, role) }

    fun continueFromSigners() {
        if (_state.value.signers.isEmpty()) {
            _state.update { it.copy(message = "Adicione pelo menos 1 signatário para avançar.") }
            return
        }
        goTo(WizardStep.FIELDS)
    }

    // ------------------------------------------------------------- Passo 3 · Campos

    fun selectSigner(linkId: String) = _state.update { it.copy(selectedLinkId = linkId) }

    fun selectPage(page: Int) = _state.update { it.copy(currentPage = page, selectedFieldId = null) }

    fun selectField(fieldId: String?) = _state.update { s ->
        val field = s.fields.firstOrNull { it.id == fieldId }
        s.copy(selectedFieldId = fieldId, selectedLinkId = field?.signerLinkId ?: s.selectedLinkId)
    }

    /** Campo só é criado com um signatário selecionado. */
    fun addField() {
        val current = _state.value
        val linkId = current.selectedLinkId
        if (linkId == null) {
            _state.update { it.copy(message = "Selecione para quem é o campo antes de adicioná-lo.") }
            return
        }
        runAction { id, owner ->
            val field = repository.addField(id, owner, linkId, current.currentPage)
            _state.update { it.copy(selectedFieldId = field.id) }
        }
    }

    fun moveField(field: SignatureField) = runAction { id, owner -> repository.updateField(id, owner, field) }

    fun deleteField(fieldId: String) = runAction { id, owner ->
        repository.deleteField(id, owner, fieldId)
        _state.update { it.copy(selectedFieldId = null) }
    }

    /** Ao entrar na revisão é gerada (ou reaproveitada) a chave de idempotência do envio. */
    fun toReview() = runAction { id, owner ->
        val key = repository.ensureIdempotencyKey(id, owner)
        _state.update { it.copy(idempotencyKey = key, step = WizardStep.REVIEW, selectedFieldId = null) }
    }

    // ------------------------------------------------------------- Passo 4 · Revisão e envio

    fun onInviteChange(value: String) {
        inviteTouched = true
        val text = value.take(Validators.INVITE_MAX)
        _state.update { it.copy(inviteMessage = text) }
        recomputeIssues()
        val id = documentId.value ?: return
        inviteJob?.cancel()
        inviteJob = viewModelScope.launch {
            delay(400)
            runCatching { repository.updateInviteMessage(id, owner(), text) }
        }
    }

    /** Modo desenvolvedor (toque longo na chave de envio): a próxima tentativa simula uma falha. */
    fun toggleSimulateFailure() = _state.update { it.copy(simulateFailure = !it.simulateFailure) }

    fun confirmSend() {
        val current = _state.value
        val id = documentId.value ?: return
        val detail = current.detail ?: return
        val key = current.idempotencyKey ?: return
        if (current.sending || current.issues.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, message = null) }
            val owner = owner()
            titleJob?.cancel()
            inviteJob?.cancel()
            runCatching {
                repository.updateTitle(id, owner, current.title)
                repository.updateInviteMessage(id, owner, current.inviteMessage)
            }
            val fresh = detail.copy(
                title = current.title.trim(),
                version = detail.version?.copy(inviteMessage = current.inviteMessage),
            )
            val outcome = sendDocument(fresh, owner, key, current.simulateFailure)
            _state.update {
                when (outcome) {
                    is SendOutcome.Invalid -> it.copy(
                        sending = false,
                        issues = outcome.issues,
                        message = outcome.issues.firstOrNull(),
                    )
                    else -> it.copy(
                        sending = false,
                        outcome = outcome,
                        step = WizardStep.RESULT,
                        simulateFailure = false,
                    )
                }
            }
        }
    }

    /** Nova tentativa com a mesma chave: não duplica convites. */
    fun retrySend() = confirmSend()

    // ------------------------------------------------------------- Navegação

    fun back() {
        val current = _state.value
        when (current.step) {
            WizardStep.DETAILS -> _state.update { it.copy(exitRequested = true) }
            WizardStep.RESULT -> if (current.outcome is SendOutcome.Success) {
                _state.update { it.copy(exitRequested = true) }
            } else {
                _state.update { it.copy(step = WizardStep.REVIEW, outcome = null) }
            }
            else -> goTo(current.step - 1)
        }
    }

    fun backToReview() = _state.update { it.copy(step = WizardStep.REVIEW, outcome = null) }

    fun deleteDraftAndExit() {
        val id = documentId.value
        viewModelScope.launch {
            if (id != null) runCatching { repository.deleteDraft(id, owner()) }
            _state.update { it.copy(exitRequested = true) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun consumeTracking() = _state.update { it.copy(openTrackingId = null) }
}
