package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.ui.theme.Indigo700
import br.com.fiap.letssign.ui.theme.Navy900
import br.com.fiap.letssign.ui.theme.Purple600
import br.com.fiap.letssign.ui.theme.SeloFonts
import br.com.fiap.letssign.ui.theme.Violet400

val HeaderGradient = Brush.linearGradient(listOf(Indigo700, Color(0xFF5C2CC6)))
val SplashGradient = Brush.verticalGradient(listOf(Color(0xFF2B1EA0), Color(0xFF1A148A), Navy900))

/** Logotipo "selo." com o ponto em violeta. */
@Composable
fun SeloWordmark(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 28.sp,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append("selo") }
            withStyle(SpanStyle(color = Violet400)) { append(".") }
        },
        modifier = modifier,
        fontFamily = SeloFonts.Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = (-0.5).sp,
    )
}

/** Ícone do app: selo branco com o "x", o traço da assinatura e a linha pontilhada. */
@Composable
fun SeloAppMark(size: Dp = 88.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(16.dp, RoundedCornerShape(size * 0.24f))
            .clip(RoundedCornerShape(size * 0.24f))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.055f
            // "x"
            drawLine(Purple600, Offset(w * 0.05f, h * 0.58f), Offset(w * 0.2f, h * 0.73f), stroke, StrokeCap.Round)
            drawLine(Purple600, Offset(w * 0.2f, h * 0.58f), Offset(w * 0.05f, h * 0.73f), stroke, StrokeCap.Round)
            // assinatura
            val path = Path().apply {
                moveTo(w * 0.3f, h * 0.74f)
                cubicTo(w * 0.4f, h * 0.5f, w * 0.5f, h * 0.15f, w * 0.6f, h * 0.1f)
                cubicTo(w * 0.7f, h * 0.05f, w * 0.68f, h * 0.4f, w * 0.55f, h * 0.65f)
                cubicTo(w * 0.5f, h * 0.78f, w * 0.62f, h * 0.78f, w * 0.72f, h * 0.6f)
                cubicTo(w * 0.76f, h * 0.53f, w * 0.82f, h * 0.66f, w * 0.92f, h * 0.58f)
            }
            drawPath(path, Purple600, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // linha pontilhada
            for (i in 0 until 5) {
                val x = w * (0.18f + i * 0.16f)
                drawCircle(Purple600, radius = stroke * 0.55f, center = Offset(x, h * 0.92f))
            }
        }
    }
}

/** Quadrados translúcidos girados que aparecem no fundo dos cabeçalhos. */
@Composable
fun BoxScope.DecorativeShapes(large: Boolean = false) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = if (large) 70.dp else 46.dp, y = if (large) 110.dp else 16.dp)
            .size(if (large) 190.dp else 150.dp)
            .rotate(30f)
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp)),
    )
    if (large) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = (-150).dp)
                .size(140.dp)
                .rotate(-18f)
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp)),
        )
    }
}

/** Cabeçalho em degradê usado no login e nas homes. */
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderGradient),
    ) {
        DecorativeShapes()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
    }
}

@Composable
fun SplashBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGradient),
    ) {
        DecorativeShapes(large = true)
        content()
    }
}
