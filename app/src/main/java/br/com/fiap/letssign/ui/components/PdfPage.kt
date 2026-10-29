package br.com.fiap.letssign.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.core.pdf.PdfBitmapLoader
import br.com.fiap.letssign.core.pdf.PdfPageSize
import br.com.fiap.letssign.ui.theme.Purple600
import java.io.File

/** Abre o PDF para renderização enquanto a tela estiver visível. */
@Composable
fun rememberPdfLoader(path: String?): PdfBitmapLoader? {
    val loader = remember(path) { path?.let { PdfBitmapLoader(File(it)) } }
    DisposableEffect(loader) {
        onDispose { loader?.close() }
    }
    return loader
}

@Composable
fun rememberPdfPageSizes(loader: PdfBitmapLoader?): List<PdfPageSize>? {
    val sizes by produceState<List<PdfPageSize>?>(initialValue = null, loader) {
        value = loader?.pageSizes()
    }
    return sizes
}

/**
 * Uma página do PDF com proporção real. O [overlay] recebe as dimensões da página na tela
 * para posicionar os campos a partir das coordenadas relativas (0..1).
 */
@Composable
fun PdfPage(
    loader: PdfBitmapLoader,
    pageIndex: Int,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxWithConstraintsScope.() -> Unit = {},
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, loader, pageIndex) {
        value = loader.render(pageIndex)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.3f, 3f))
            .shadow(6.dp)
            .background(Color.White),
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "Página ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
                color = Purple600,
                strokeWidth = 2.5.dp,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            overlay()
        }
    }
}
