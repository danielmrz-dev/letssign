package br.com.fiap.letssign.feature.documents.ui.wizard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.fiap.letssign.core.model.DocumentSigner
import br.com.fiap.letssign.core.model.SignerRole
import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.core.util.Validators
import br.com.fiap.letssign.ui.components.Avatar
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.InfoBanner
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.SecondaryButton
import br.com.fiap.letssign.ui.components.SeloTextField
import br.com.fiap.letssign.ui.components.dashedBorder
import br.com.fiap.letssign.ui.theme.ErrorBg
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Lavender200
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.Purple700
import br.com.fiap.letssign.ui.theme.SeloFonts
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg

// ===================================================================== Tela 06 · Dados

@Composable
fun StepDetails(
    state: WizardUiState,
    onTitleChange: (String) -> Unit,
    onPdfPicked: (Uri) -> Unit,
    onContinue: () -> Unit,
) {
    // Storage Access Framework filtrado por application/pdf
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPdfPicked(uri)
    }
    val openPicker = { picker.launch(arrayOf("application/pdf")) }
    val version = state.detail?.version

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            SeloTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = "Título do documento",
                placeholder = "Ex.: Contrato de locação comercial",
                error = state.titleError,
            )
            Text(
                "${state.title.length}/${Validators.TITLE_MAX}",
                style = MaterialTheme.typography.bodySmall,
                color = Ink600,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Arquivo PDF", style = MaterialTheme.typography.labelMedium, color = Ink900, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            when {
                state.importing -> Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Purple600, strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(14.dp))
                        Text("Validando o PDF…", style = MaterialTheme.typography.bodyMedium, color = Ink900)
                    }
                }
                version != null -> Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Lavender100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Purple600)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    version.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Ink900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "PDF · ${Formatters.plural(version.pageCount, "página", "páginas")} · ${Formatters.fileSize(version.fileSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Ink600,
                                )
                            }
                            TextButton(onClick = openPicker) {
                                Text("Trocar", color = Purple600, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SuccessBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Verified, contentDescription = null, tint = SuccessFg, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PDF válido · SHA-256",
                                color = SuccessFg,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Formatters.shortHash(version.sha256),
                                color = SuccessFg,
                                fontFamily = SeloFonts.Mono,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .dashedBorder(Lavender200, radius = 16.dp)
                        .clickable(onClick = openPicker)
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Purple600, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Selecionar PDF do celular", style = MaterialTheme.typography.titleSmall, color = Purple700)
                    Spacer(Modifier.height(4.dp))
                    Text("Somente arquivos PDF de até 20 MB", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
            }

            state.importError?.let {
                Spacer(Modifier.height(12.dp))
                InfoBanner(text = it, icon = Icons.Outlined.ErrorOutline, background = ErrorBg, contentColor = ErrorFg)
            }
        }
        BottomActionBar {
            PrimaryButton(
                text = "Continuar",
                onClick = onContinue,
                enabled = !state.importing,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ===================================================================== Tela 07 · Signatários

private sealed interface PickerMode {
    data object Add : PickerMode
    data class Replace(val linkId: String) : PickerMode
}

@Composable
fun StepSigners(
    state: WizardUiState,
    onAdd: (String) -> Unit,
    onReplace: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onRole: (String, SignerRole) -> Unit,
    onRegisterSigner: () -> Unit,
    onContinue: () -> Unit,
) {
    var picker by remember { mutableStateOf<PickerMode?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Quem precisa assinar? Classifique o papel de cada pessoa.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink900,
                )
            }
            items(state.signers, key = { it.linkId }) { signer ->
                SignerCard(
                    signer = signer,
                    onReplace = { picker = PickerMode.Replace(signer.linkId) },
                    onRemove = { onRemove(signer.linkId) },
                    onRole = { role -> onRole(signer.linkId, role) },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .dashedBorder(Lavender200)
                        .clickable { picker = PickerMode.Add }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Purple600)
                    Spacer(Modifier.width(10.dp))
                    Text("Adicionar signatário", color = Purple600, style = MaterialTheme.typography.titleSmall)
                }
            }
            if (state.signers.isEmpty()) {
                item {
                    Text(
                        "Adicione pelo menos 1 signatário para avançar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                    )
                }
            }
        }
        BottomActionBar {
            PrimaryButton(
                text = "Posicionar campos",
                onClick = onContinue,
                enabled = state.signers.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }

    picker?.let { mode ->
        val alreadyIn = state.signers.map { it.userId }.toSet()
        SignerPickerDialog(
            title = if (mode is PickerMode.Replace) "Substituir signatário" else "Adicionar signatário",
            candidates = state.availableSigners.filter { it.id !in alreadyIn },
            hasAnySigner = state.availableSigners.isNotEmpty(),
            onPick = { user ->
                when (mode) {
                    PickerMode.Add -> onAdd(user.id)
                    is PickerMode.Replace -> onReplace(mode.linkId, user.id)
                }
                picker = null
            },
            onRegister = {
                picker = null
                onRegisterSigner()
            },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun SignerCard(
    signer: DocumentSigner,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onRole: (SignerRole) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(signer.fullName, signer.colorIndex, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(signer.fullName, style = MaterialTheme.typography.titleMedium, color = Ink900)
                    Text(
                        signer.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onReplace) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "Substituir ${signer.fullName}", tint = Ink900)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.PersonRemove, contentDescription = "Remover ${signer.fullName}", tint = ErrorFg)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignerRole.entries.forEach { role ->
                    val selected = role == signer.role
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Lavender100 else Color.White)
                            .border(if (selected) 1.5.dp else 1.dp, if (selected) Purple600 else Line, RoundedCornerShape(10.dp))
                            .clickable { onRole(role) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            role.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Purple700 else Ink600,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignerPickerDialog(
    title: String,
    candidates: List<User>,
    hasAnySigner: Boolean,
    onPick: (User) -> Unit,
    onRegister: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = candidates.filter {
        query.isBlank() || it.fullName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Ink900)
                Spacer(Modifier.height(12.dp))
                if (candidates.isNotEmpty()) {
                    SeloTextField(value = query, onValueChange = { query = it }, label = "Buscar", placeholder = "Nome ou e-mail")
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(filtered, key = { it.id }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(user) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(user.fullName, user.colorIndex, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(user.fullName, style = MaterialTheme.typography.titleSmall, color = Ink900)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = Ink600)
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                when {
                                    !hasAnySigner -> "Nenhum signatário cadastrado ainda."
                                    candidates.isEmpty() -> "Todos os signatários cadastrados já estão neste documento."
                                    else -> "Nenhum resultado para \"$query\"."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink400,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Cadastrar novo",
                        onClick = onRegister,
                        icon = Icons.Outlined.PersonAdd,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss, modifier = Modifier.height(54.dp)) {
                        Text("Fechar", color = Ink600)
                    }
                }
            }
        }
    }
}
