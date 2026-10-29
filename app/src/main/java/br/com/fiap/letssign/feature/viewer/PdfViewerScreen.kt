package br.com.fiap.letssign.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.core.session.SessionManager
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.di.seloViewModel
import br.com.fiap.letssign.feature.documents.domain.DocumentRepository
import br.com.fiap.letssign.feature.tracking.ui.sharePdf
import br.com.fiap.letssign.ui.components.EmptyState
import br.com.fiap.letssign.ui.components.PdfPage
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.components.rememberPdfLoader
import br.com.fiap.letssign.ui.components.rememberPdfPageSizes
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.signerColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** Quem está abrindo o PDF: o dono (Admin, por documentId) ou o signatário (por vínculo). */
enum class ViewerKind {
    OWNER,
    SIGNER;

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.name == value } ?: OWNER
    }
}

data class ViewerUiState(
    val loading: Boolean = true,
    val title: String = "",
    val path: String? = null,
    val shareable: Boolean = false,
    val fields: List<SignatureField> = emptyList(),
    val signers: List<DocumentSigner> = emptyList(),
    val notFound: Boolean = false,
)

class PdfViewerViewModel(
    kind: ViewerKind,
    id: String,
    preview: Boolean,
    session: SessionManager,
    repository: DocumentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewerUiState())
    val state: StateFlow<ViewerUiState> = _state

    init {
        viewModelScope.launch {
            val user = session.requireUser()
            _state.value = when (kind) {
                ViewerKind.OWNER -> {
                    val detail = repository.observeDetail(id, user.id).first()
                    val version = detail?.version
                    if (detail == null || version == null) {
                        ViewerUiState(loading = false, notFound = true)
                    } else {
                        val completed = detail.status == DocumentStatus.COMPLETED
                        ViewerUiState(
                            loading = false,
                            title = detail.title,
                            path = if (preview) version.originalPath else version.signedPath ?: version.originalPath,
                            shareable = completed && !preview,
                            // Na pré-visualização da revisão os campos aparecem sobre as páginas
                            fields = if (preview) detail.fields else emptyList(),
                            signers = detail.signers,
                        )
                    }
                }
                ViewerKind.SIGNER -> {
                    val session2 = repository.loadSigningSession(id, user.id)
                    if (session2 == null) {
                        ViewerUiState(loading = false, notFound = true)
                    } else {
                        ViewerUiState(
                            loading = false,
                            title = session2.title,
                            path = session2.pdfPath,
                            shareable = session2.documentStatus == DocumentStatus.COMPLETED,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfViewerScreen(kind: ViewerKind, id: String, preview: Boolean, onBack: () -> Unit) {
    val viewModel = seloViewModel { PdfViewerViewModel(kind, id, preview, it.session, it.documentRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loader = rememberPdfLoader(state.path)
    val sizes = rememberPdfPageSizes(loader)
    val context = LocalContext.current
    val signersById = state.signers.associateBy { it.linkId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lavender100),
    ) {
        ScreenTopBar(
            title = state.title.ifBlank { "Documento" },
            subtitle = when {
                preview -> "Pré-visualização com os campos"
                sizes != null -> Formatters.plural(sizes.size, "página", "páginas")
                else -> null
            },
            onBack = onBack,
            trailing = {
                if (state.shareable) {
                    IconButton(onClick = { state.path?.let { sharePdf(context, File(it), state.title) } }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartilhar PDF", tint = Purple600)
                    }
                }
            },
        )
        when {
            state.notFound -> EmptyState("Documento não encontrado ou você não tem acesso a ele.")
            loader == null || sizes == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple600)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                itemsIndexed(sizes) { index, size ->
                    PdfPage(loader = loader, pageIndex = index, aspectRatio = size.aspectRatio) {
                        state.fields.filter { it.pageIndex == index }.forEach { field ->
                            val signer = signersById[field.signerLinkId]
                            val color = signerColor(signer?.colorIndex ?: 0)
                            Box(
                                modifier = Modifier
                                    .offset(x = maxWidth * field.xRatio, y = maxHeight * field.yRatio)
                                    .size(maxWidth * field.widthRatio, maxHeight * field.heightRatio)
                                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                    .border(1.5.dp, color, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    signer?.let { "${it.fullName.substringBefore(' ')} · ${it.role.label}" }.orEmpty(),
                                    color = color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item { Box(Modifier.fillMaxWidth().size(8.dp)) }
            }
        }
    }
}
