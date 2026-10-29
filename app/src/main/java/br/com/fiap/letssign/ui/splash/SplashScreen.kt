package br.com.fiap.letssign.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.LetsSignTheme
import br.com.fiap.letssign.ui.theme.SplashDotPurple
import br.com.fiap.letssign.ui.theme.SplashGradientBottom
import br.com.fiap.letssign.ui.theme.SplashGradientMiddle
import br.com.fiap.letssign.ui.theme.SplashGradientTop
import br.com.fiap.letssign.ui.theme.SplashIconPurple
import br.com.fiap.letssign.ui.theme.SplashLightLilac
import br.com.fiap.letssign.ui.theme.SplashShapeOverlay
import kotlinx.coroutines.launch

/**
 * Composable stateful da Splash Screen. Observa o estado do ViewModel e aciona os callbacks de navegação.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdminHome: () -> Unit,
    onNavigateToSignerHome: () -> Unit
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.AdminHome -> onNavigateToAdminHome()
            SplashDestination.SignerHome -> onNavigateToSignerHome()
            SplashDestination.Loading -> { /* Carregando sessão */ }
        }
    }

    SplashScreenContent()
}

/**
 * Layout e visual stateless da Splash Screen (Edge-to-edge com gradiente, elementos vetoriais e animação).
 */
@Composable
fun SplashScreenContent() {
    // Animação de entrada (Fade-in + Scale de 0.92 -> 1.0 em 600ms)
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    // Fundo com gradiente vertical e recortes contidos
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashGradientTop,
                        SplashGradientMiddle,
                        SplashGradientBottom
                    )
                )
            )
            .clipToBounds()
    ) {
        // Quadrado 1 translúcido no canto superior direito (~30° rotação)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .rotate(30f)
                .size(220.dp)
                .background(
                    color = SplashShapeOverlay,
                    shape = RoundedCornerShape(36.dp)
                )
        )

        // Quadrado 2 translúcido no canto inferior esquerdo (~ -20° rotação)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 50.dp)
                .rotate(-20f)
                .size(240.dp)
                .background(
                    color = SplashShapeOverlay,
                    shape = RoundedCornerShape(44.dp)
                )
        )

        // Bloco central com animação de entrada
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone do App: Quadrado branco 98dp com cantos 28dp e assinatura em vetor
            SeloAppIcon()

            Spacer(modifier = Modifier.height(16.dp))

            // Wordmark: "selo" em branco e ponto "." em roxo vivo
            val annotatedWordmark = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.White)) {
                    append(stringResource(R.string.splash_wordmark_main))
                }
                withStyle(style = SpanStyle(color = SplashDotPurple)) {
                    append(stringResource(R.string.splash_wordmark_dot))
                }
            }

            Text(
                text = annotatedWordmark,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline do aplicativo
            Text(
                text = stringResource(R.string.splash_tagline),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        // Rodapé alinhado ao fundo respeitando insets da barra de navegação
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.splash_checking_session),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.splash_partnership),
                color = SplashLightLilac.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Ícone estilizado do selo. com vetor de assinatura desenhado em Canvas.
 */
@Composable
private fun SeloAppIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(98.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            val strokeWidthPx = 3.5.dp.toPx()
            val strokeCap = StrokeCap.Round
            val strokeJoin = StrokeJoin.Round

            // 1. "x" estilizado
            val xCenter = size.width * 0.18f
            val yCenter = size.height * 0.42f
            val xOffset = size.width * 0.07f

            drawLine(
                color = SplashIconPurple,
                start = Offset(xCenter - xOffset, yCenter - xOffset),
                end = Offset(xCenter + xOffset, yCenter + xOffset),
                strokeWidth = strokeWidthPx,
                cap = strokeCap
            )
            drawLine(
                color = SplashIconPurple,
                start = Offset(xCenter - xOffset, yCenter + xOffset),
                end = Offset(xCenter + xOffset, yCenter - xOffset),
                strokeWidth = strokeWidthPx,
                cap = strokeCap
            )

            // 2. Traço cursivo
            val signaturePath = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.45f)
                cubicTo(
                    size.width * 0.38f, size.height * 0.12f,
                    size.width * 0.48f, size.height * 0.12f,
                    size.width * 0.52f, size.height * 0.52f
                )
                cubicTo(
                    size.width * 0.56f, size.height * 0.78f,
                    size.width * 0.65f, size.height * 0.20f,
                    size.width * 0.72f, size.height * 0.35f
                )
                cubicTo(
                    size.width * 0.78f, size.height * 0.48f,
                    size.width * 0.84f, size.height * 0.40f,
                    size.width * 0.90f, size.height * 0.45f
                )
            }

            drawPath(
                path = signaturePath,
                color = SplashIconPurple,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = strokeCap,
                    join = strokeJoin
                )
            )

            // 3. Linha pontilhada
            val dotY = size.height * 0.82f
            val dashEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                phase = 0f
            )

            drawLine(
                color = SplashIconPurple,
                start = Offset(size.width * 0.10f, dotY),
                end = Offset(size.width * 0.90f, dotY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = dashEffect,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    LetsSignTheme {
        SplashScreenContent()
    }
}
