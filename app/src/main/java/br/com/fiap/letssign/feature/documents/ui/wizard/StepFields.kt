package br.com.fiap.letssign.feature.documents.ui.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.SignatureField
import br.com.fiap.letssign.feature.documents.domain.FieldGeometry
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.PdfPage
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.SecondaryButton
import br.com.fiap.letssign.ui.components.SectionLabel
import br.com.fiap.letssign.ui.components.rememberPdfLoader
import br.com.fiap.letssign.ui.components.rememberPdfPageSizes
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.signerColor
import kotlin.math.roundToInt

// ===================================================================== Tela 08 · Campos

@Composable
fun StepFields(
    state: WizardUiState,
    onSelectSigner: (String) -> Unit,
    onSelectPage: (Int) -> Unit,
    onSelectField: (String?) -> Unit,
    onAddField: () -> Unit,
    onMoveField: (SignatureField) -> Unit,
    onDeleteField: (String) -> Unit,
    onReview: () -> Unit,
) {
    val version = state.detail?.version
    val loader = rememberPdfLoader(version?.originalPath)
    val sizes = rememberPdfPageSizes(loader)
    val signersById = state.signers.associateBy { it.linkId }
    val pageFields = state.fields.filter { it.pageIndex == state.currentPage }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Lavender50)
                .padding(top = 14.dp),
        ) {
            SectionLabel("Campo para", modifier = Modifier.padding(horizontal = 20.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.signers, key = { it.linkId }) { signer ->
                    SignerChip(
                        signer = signer,
                        count = state.fields.count { it.signerLinkId == signer.linkId },
                        selected = signer.linkId == state.selectedLinkId,
                        onClick = { onSelectSigner(signer.linkId) },
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items((0 until state.pageCount).toList()) { page ->
                        val selected = page == state.currentPage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Ink900 else Color.White)
                                .clickable { onSelectPage(page) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "Pág. ${page + 1}",
                                color = if (selected) Color.White else Ink900,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when (pageFields.size) {
                        0 -> "0 campos\nnesta página"
                        1 -> "1 campo\nnesta página"
                        else -> "${pageFields.size} campos\nnesta página"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Lavender100),
        ) {
            val pageSize = sizes?.getOrNull(state.currentPage)
            if (loader == null || pageSize == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Purple600)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    PdfPage(
                        loader = loader,
                        pageIndex = state.currentPage,
                        aspectRatio = pageSize.aspectRatio,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onSelectField(null) })
                        },
                    ) {
                        pageFields.forEach { field ->
                            val signer = signersById[field.signerLinkId]
                            EditableField(
                                field = field,
                                signer = signer,
                                selected = field.id == state.selectedFieldId,
                                onSelect = { onSelectField(field.id) },
                                onCommit = onMoveField,
                                onDelete = { onDeleteField(field.id) },
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Arraste o campo para posicionar. Toque nele para ver a alça de tamanho e o botão de excluir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                    )
                }
            }
        }

        BottomActionBar {
            SecondaryButton(
                text = "Adicionar campo",
                onClick = onAddField,
                icon = Icons.Filled.Add,
                enabled = state.selectedLinkId != null,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Revisar",
                onClick = onReview,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SignerChip(signer: DocumentSigner, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Lavender100 else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Purple600 else Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(signer.fullName, signer.colorIndex, size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "${signer.fullName.substringBefore(' ')} · ${signer.role.label}",
            style = MaterialTheme.typography.labelLarge,
            color = Ink900,
        )
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = Ink600)
    }
}

/** Deslocamento em andamento, relativo à posição salva [base]. */
private data class DragDelta(
    val base: SignatureField,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val dw: Float = 0f,
    val dh: Float = 0f,
)

/**
 * Campo de assinatura sobre a página. Durante o arraste a posição é limitada à página
 * (0..1 descontando largura e altura); ao soltar, o valor em proporção é salvo.
 */
@Composable
private fun BoxWithConstraintsScope.EditableField(
    field: SignatureField,
    signer: DocumentSigner?,
    selected: Boolean,
    onSelect: () -> Unit,
    onCommit: (SignatureField) -> Unit,
    onDelete: () -> Unit,
) {
    val pageW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val pageH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
    val density = LocalDensity.current
    val color = signerColor(signer?.colorIndex ?: 0)

    var delta by remember(field.id) { mutableStateOf<DragDelta?>(null) }
    val currentField by rememberUpdatedState(field)
    val currentCommit by rememberUpdatedState(onCommit)
    val currentSelect by rememberUpdatedState(onSelect)

    // Se o banco já devolveu a nova posição, o delta antigo deixa de valer.
    val active = delta?.takeIf { it.base == field }
    val shown = FieldGeometry.clamp(
        field.copy(
            xRatio = field.xRatio + (active?.dx ?: 0f),
            yRatio = field.yRatio + (active?.dy ?: 0f),
            widthRatio = field.widthRatio + (active?.dw ?: 0f),
            heightRatio = field.heightRatio + (active?.dh ?: 0f),
        ),
    )
    val widthDp = with(density) { (shown.widthRatio * pageW).toDp() }
    val heightDp = with(density) { (shown.heightRatio * pageH).toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset((shown.xRatio * pageW).roundToInt(), (shown.yRatio * pageH).roundToInt()) }
            .size(widthDp, heightDp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = if (selected) 0.22f else 0.14f))
                .border(if (selected) 2.dp else 1.5.dp, color, RoundedCornerShape(6.dp))
                .pointerInput(field.id) {
                    detectTapGestures(onTap = { currentSelect() })
                }
                .pointerInput(field.id, pageW, pageH) {
                    detectDragGestures(
                        onDragStart = {
                            currentSelect()
                            delta = DragDelta(base = currentField)
                        },
                        onDragEnd = {
                            val d = delta?.takeIf { it.base == currentField }
                            if (d != null) {
                                currentCommit(
                                    FieldGeometry.clamp(
                                        d.base.copy(xRatio = d.base.xRatio + d.dx, yRatio = d.base.yRatio + d.dy),
                                    ),
                                )
                            }
                        },
                        onDragCancel = { delta = null },
                    ) { change, amount ->
                        change.consume()
                        val d = delta ?: DragDelta(base = currentField)
                        delta = d.copy(dx = d.dx + amount.x / pageW, dy = d.dy + amount.y / pageH)
                    }
                }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                signer?.let { "${it.fullName.substringBefore(' ')} · ${it.role.label}" } ?: "Sem signatário",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            // Excluir
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, color, CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Excluir campo", tint = color, modifier = Modifier.size(14.dp))
            }
            // Redimensionar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .pointerInput(field.id, pageW, pageH) {
                        detectDragGestures(
                            onDragStart = { delta = DragDelta(base = currentField) },
                            onDragEnd = {
                                val d = delta?.takeIf { it.base == currentField }
                                if (d != null) {
                                    currentCommit(
                                        FieldGeometry.clamp(
                                            d.base.copy(
                                                widthRatio = (d.base.widthRatio + d.dw).coerceAtMost(1f - d.base.xRatio),
                                                heightRatio = (d.base.heightRatio + d.dh).coerceAtMost(1f - d.base.yRatio),
                                            ),
                                        ),
                                    )
                                }
                            },
                            onDragCancel = { delta = null },
                        ) { change, amount ->
                            change.consume()
                            val d = delta ?: DragDelta(base = currentField)
                            delta = d.copy(dw = d.dw + amount.x / pageW, dh = d.dh + amount.y / pageH)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.OpenInFull, contentDescription = "Redimensionar campo", tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}
