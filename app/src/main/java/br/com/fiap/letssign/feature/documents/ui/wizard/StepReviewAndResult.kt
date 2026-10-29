package br.com.fiap.letssign.feature.documents.ui.wizard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.core.model.SendOutcome
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.InfoBanner
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.SectionCard
import br.com.fiap.letssign.ui.components.SectionLabel
import br.com.fiap.letssign.ui.theme.ErrorBg
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender50
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.Purple700
import br.com.fiap.letssign.ui.theme.SeloFonts
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg
import br.com.fiap.letssign.ui.theme.WarningBg
import br.com.fiap.letssign.ui.theme.WarningFg

// ===================================================================== Tela 09 · Revisão e envio

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepReview(
    state: WizardUiState,
    onInviteChange: (String) -> Unit,
    onToggleSimulation: () -> Unit,
    onPreview: () -> Unit,
    onSend: () -> Unit,
) {
    val detail = state.detail
    val version = detail?.version

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.issues.isEmpty()) {
                InfoBanner(text = "Tudo certo para enviar", icon = Icons.Outlined.CheckCircle, background = SuccessBg, contentColor = SuccessFg)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarningBg)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = WarningFg, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Resolva antes de enviar", color = WarningFg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    state.issues.forEach { issue ->
                        Text("• $issue", color = WarningFg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            SectionCard {
                SectionLabel("Documento final")
                Spacer(Modifier.height(8.dp))
                Text(state.title.ifBlank { "Sem título" }, style = MaterialTheme.typography.titleLarge, color = Ink900)
                if (version != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Purple600, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${version.fileName} · ${Formatters.fileSize(version.fileSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    state.fields.groupBy { it.pageIndex }.toSortedMap().forEach { (page, fields) ->
                        Text(
                            "Pág. ${page + 1}: ${Formatters.plural(fields.size, "campo", "campos")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink600,
                        )
                    }
                    TextButton(onClick = onPreview, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Purple600, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver documento com os campos", color = Purple600, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SectionCard {
                SectionLabel("Signatários")
                Spacer(Modifier.height(4.dp))
                state.signers.forEach { signer ->
                    val count = state.fields.count { it.signerLinkId == signer.linkId }
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(signer.fullName, signer.colorIndex, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(signer.fullName, style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(signer.role.label, style = MaterialTheme.typography.bodySmall, color = Ink600)
                        }
                        Text(
                            Formatters.plural(count, "campo", "campos"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (count == 0) ErrorFg else Ink900,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            SectionCard {
                SectionLabel("Mensagem do convite")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.inviteMessage,
                    onValueChange = onInviteChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple600,
                        unfocusedBorderColor = Line,
                        cursorColor = Purple600,
                    ),
                )
            }

            // Toque longo na chave liga o modo desenvolvedor (simula falha no próximo envio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = {}, onLongClick = onToggleSimulation)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = Ink400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chave de envio", style = MaterialTheme.typography.bodySmall, color = Ink600)
                Spacer(Modifier.width(8.dp))
                Text(
                    state.idempotencyKey.orEmpty(),
                    fontFamily = SeloFonts.Mono,
                    fontSize = 11.sp,
                    color = Ink600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.simulateFailure) {
                InfoBanner(
                    text = "Modo desenvolvedor: a próxima tentativa vai simular uma falha de envio.",
                    icon = Icons.Outlined.BugReport,
                    background = Lavender50,
                    contentColor = Purple700,
                )
            }
        }
        BottomActionBar {
            PrimaryButton(
                text = "Confirmar envio",
                onClick = onSend,
                icon = Icons.AutoMirrored.Filled.Send,
                enabled = state.issues.isEmpty() && state.idempotencyKey != null,
                loading = state.sending,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ===================================================================== Tela 10 · Resultado do envio

@Composable
fun StepResult(
    state: WizardUiState,
    onRetry: () -> Unit,
    onBackToReview: () -> Unit,
    onTrack: () -> Unit,
    onHome: () -> Unit,
) {
    val outcome = state.outcome
    val success = outcome as? SendOutcome.Success
    val failure = outcome as? SendOutcome.Failure

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(if (success != null) SuccessBg else ErrorBg),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (success != null) SuccessFg else ErrorFg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (success != null) Icons.Filled.Check else Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (success != null) "Documento enviado" else "Não foi possível enviar",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    success != null && success.alreadySent ->
                        "Este envio já tinha sido registrado. Nenhum convite foi duplicado."
                    success != null ->
                        "${Formatters.plural(success.invites, "convite registrado", "convites registrados")} para assinatura. " +
                            "Você acompanha cada assinatura em tempo real."
                    failure != null ->
                        "${failure.reason}\n\nVerifique a conexão e tente de novo. A nova tentativa usa a mesma chave, " +
                            "então nenhum convite será duplicado."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Ink600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            val key = success?.key ?: failure?.key ?: state.idempotencyKey.orEmpty()
            val attempt = success?.attempt ?: failure?.attempt ?: 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Lavender50)
                    .padding(14.dp),
            ) {
                Row {
                    Text("Chave de idempotência", style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.weight(1f))
                    Text("Tentativa $attempt", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
                Spacer(Modifier.height(4.dp))
                Text(key, fontFamily = SeloFonts.Mono, fontSize = 12.sp, color = Ink900)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (success != null) {
                PrimaryButton(text = "Acompanhar documento", onClick = onTrack)
                TextButton(onClick = onHome, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Voltar ao início", color = Purple600, fontWeight = FontWeight.SemiBold)
                }
            } else {
                PrimaryButton(text = "Tentar novamente", onClick = onRetry, loading = state.sending)
                TextButton(onClick = onBackToReview, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Voltar para a revisão", color = Purple600, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
