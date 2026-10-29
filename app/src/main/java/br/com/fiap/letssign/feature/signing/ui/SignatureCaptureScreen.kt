package br.com.fiap.letssign.feature.signing.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.di.LocalAppContainer
import br.com.fiap.letssign.ui.components.BottomActionBar
import br.com.fiap.letssign.ui.components.PrimaryButton
import br.com.fiap.letssign.ui.components.ScreenTopBar
import br.com.fiap.letssign.ui.components.SecondaryButton
import br.com.fiap.letssign.ui.theme.Ink400
import br.com.fiap.letssign.ui.theme.Ink600
import br.com.fiap.letssign.ui.theme.Ink900
import br.com.fiap.letssign.ui.theme.Lavender200
import br.com.fiap.letssign.ui.theme.Lavender50
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val InkColor = Color(0xFF1B1464)

/** Tela 14 · Desenho da assinatura com o dedo, exportado como PNG de fundo transparente. */
@Composable
fun SignatureCaptureScreen(linkId: String, onCancel: () -> Unit, onConfirmed: (String) -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val signerName = LocalAppContainer.current.session.currentUser.value?.fullName.orEmpty()
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    val strokeWidthPx = with(density) { 3.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        ScreenTopBar(title = "Sua assinatura", onBack = onCancel, closeIcon = true)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
        ) {
            Text(
                "Desenhe com o dedo dentro da área. Ela será aplicada em todos os seus campos deste documento.",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink900,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Lavender50)
                    .border(1.dp, Lavender200, RoundedCornerShape(20.dp)),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { point -> strokes.add(listOf(point, point + Offset(0.5f, 0.5f))) })
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { point -> current = listOf(point) },
                                onDragEnd = {
                                    if (current.isNotEmpty()) strokes.add(current)
                                    current = emptyList()
                                },
                                onDragCancel = {
                                    if (current.isNotEmpty()) strokes.add(current)
                                    current = emptyList()
                                },
                            ) { change, _ ->
                                change.consume()
                                current = current + change.position
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    // linha de base tracejada, "x" e nome do signatário (como no design)
                    val baseY = h * 0.72f
                    drawLine(
                        color = Ink400,
                        start = Offset(w * 0.08f, baseY),
                        end = Offset(w * 0.92f, baseY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                    val xSize = 10f
                    val xOrigin = Offset(w * 0.08f + xSize, baseY - 22f)
                    drawLine(Ink400, xOrigin - Offset(xSize, xSize), xOrigin + Offset(xSize, xSize), 2.5f, StrokeCap.Round)
                    drawLine(Ink400, xOrigin + Offset(-xSize, xSize), xOrigin + Offset(xSize, -xSize), 2.5f, StrokeCap.Round)

                    (strokes + listOf(current)).forEach { points ->
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path,
                                InkColor,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                Text(
                    signerName,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomStart)
                        .padding(start = 28.dp, bottom = 34.dp),
                    color = Ink600,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = Ink600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salva como PNG com fundo transparente", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
        }
        BottomActionBar {
            SecondaryButton(
                text = "Limpar",
                onClick = {
                    strokes.clear()
                    current = emptyList()
                },
                color = Ink900,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Confirmar",
                enabled = strokes.isNotEmpty(),
                loading = saving,
                onClick = {
                    saving = true
                    val snapshot = strokes.toList()
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            exportSignature(context, linkId, snapshot, strokeWidthPx)
                        }
                        saving = false
                        if (file != null) onConfirmed(file.absolutePath)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Recorta a área desenhada e salva em PNG com fundo transparente. */
private fun exportSignature(context: Context, linkId: String, strokes: List<List<Offset>>, strokeWidth: Float): File? {
    val points = strokes.flatten()
    if (points.isEmpty()) return null
    val pad = strokeWidth * 3
    val minX = points.minOf { it.x } - pad
    val minY = points.minOf { it.y } - pad
    val maxX = points.maxOf { it.x } + pad
    val maxY = points.maxOf { it.y } + pad
    val width = (maxX - minX).toInt().coerceAtLeast(1)
    val height = (maxY - minY).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = InkColor.toArgb()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.isEmpty()) return@forEach
        val path = android.graphics.Path()
        path.moveTo(stroke.first().x - minX, stroke.first().y - minY)
        stroke.drop(1).forEach { path.lineTo(it.x - minX, it.y - minY) }
        canvas.drawPath(path, paint)
    }

    val dir = File(context.cacheDir, "signatures").apply { mkdirs() }
    val file = File(dir, "assinatura_${linkId}_${System.currentTimeMillis()}.png")
    return runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }.getOrNull()
}
