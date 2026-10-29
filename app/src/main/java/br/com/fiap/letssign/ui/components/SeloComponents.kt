package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.core.model.DocumentStatus
import br.com.fiap.letssign.core.model.SignerStatus
import br.com.fiap.letssign.core.util.Formatters
import br.com.fiap.letssign.ui.theme.DisabledPurple
import br.com.fiap.letssign.ui.theme.ErrorFg
import br.com.fiap.letssign.ui.theme.InfoBg
import br.com.fiap.letssign.ui.theme.InfoFg
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender100
import br.com.fiap.letssign.ui.theme.Line
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.Purple700
import br.com.fiap.letssign.ui.theme.SuccessBg
import br.com.fiap.letssign.ui.theme.SuccessFg
import br.com.fiap.letssign.ui.theme.WarningBg
import br.com.fiap.letssign.ui.theme.WarningFg
import br.com.fiap.letssign.ui.theme.signerColor

private val ButtonShape = RoundedCornerShape(14.dp)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Purple600,
            contentColor = Color.White,
            disabledContainerColor = DisabledPurple,
            disabledContentColor = Color.White,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = Purple600,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, if (enabled) color else DisabledPurple),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = DisabledPurple),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Purple600,
    unfocusedBorderColor = Line,
    errorBorderColor = ErrorFg,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    cursorColor = Purple600,
    focusedTextColor = Ink900,
    unfocusedTextColor = Ink900,
)

@Composable
private fun FieldLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = Ink900, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun FieldFooter(error: String?, helper: String?) {
    when {
        error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorFg, modifier = Modifier.padding(top = 4.dp))
        helper != null -> Text(helper, style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SeloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    helper: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = Ink400) } },
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
        FieldFooter(error, helper)
    }
}

@Composable
fun SeloTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    helper: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = Ink400) } },
            isError = error != null,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
        FieldFooter(error, helper)
    }
}

/** Avatar circular com as iniciais e a cor do signatário. */
@Composable
fun Avatar(name: String, colorIndex: Int, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(signerColor(colorIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Formatters.initials(name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}

@Composable
fun Pill(text: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun DocumentStatusChip(status: DocumentStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        DocumentStatus.DRAFT -> Lavender100 to Ink600
        DocumentStatus.SENT -> InfoBg to InfoFg
        DocumentStatus.PARTIALLY_SIGNED -> Lavender100 to Purple700
        DocumentStatus.COMPLETED -> SuccessBg to SuccessFg
    }
    Pill(status.label, bg, fg, modifier)
}

@Composable
fun SignerStatusChip(status: SignerStatus, modifier: Modifier = Modifier) {
    when (status) {
        SignerStatus.PENDING -> Pill(status.label, WarningBg, WarningFg, modifier)
        SignerStatus.SIGNED -> Pill(status.label, SuccessBg, SuccessFg, modifier)
    }
}

/** Barra de progresso fina (assinaturas concluídas / total). */
@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier, color: Color = Purple600) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Lavender100),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = Ink600,
    )
}

@Composable
fun InfoBanner(
    text: String,
    icon: ImageVector,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/** Barra superior branca das telas internas (seta de voltar ou X para fechar). */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    closeIcon: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = if (closeIcon) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (closeIcon) "Fechar" else "Voltar",
                    tint = Ink900,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
            }
            trailing?.invoke(this)
        }
        HorizontalDivider(color = Line)
    }
}

/** Cabeçalho do assistente "Novo documento" com os 5 segmentos de progresso. */
@Composable
fun WizardHeader(
    step: Int,
    stepName: String,
    draftSaved: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Ink900)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Novo documento", style = MaterialTheme.typography.titleLarge, color = Ink900)
                Text("Passo ${step + 1} de 5 · $stepName", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Ink900)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (index <= step) Purple600 else Line),
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (draftSaved) {
                Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = SuccessFg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Rascunho salvo automaticamente", style = MaterialTheme.typography.bodySmall, color = Ink600)
            } else {
                Text("O rascunho é salvo assim que o PDF for escolhido", style = MaterialTheme.typography.bodySmall, color = Ink400)
            }
        }
        HorizontalDivider(color = Line)
    }
}

/** Área fixa no rodapé com os botões de ação. */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        HorizontalDivider(color = Line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Ink600,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** Borda tracejada (botões "Adicionar signatário" e "Selecionar PDF"). */
fun Modifier.dashedBorder(color: Color, radius: Dp = 14.dp, strokeWidth: Dp = 1.5.dp): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
    )
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
}

@Composable
fun BorderedIconBox(icon: ImageVector, tint: Color, background: Color, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(0.dp, Color.Transparent, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
