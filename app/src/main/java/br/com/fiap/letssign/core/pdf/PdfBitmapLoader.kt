package br.com.fiap.letssign.core.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Tamanho da página em pontos do PDF; aspectRatio = largura / altura. */
data class PdfPageSize(val width: Int, val height: Int) {
    val aspectRatio: Float get() = if (height == 0) 0.707f else width.toFloat() / height.toFloat()
}

/**
 * Renderiza as páginas do PDF em bitmaps com o PdfRenderer nativo.
 * O PdfRenderer não aceita acesso concorrente, então tudo passa por um Mutex.
 */
class PdfBitmapLoader(private val file: File, private val targetWidthPx: Int = 1200) {
    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var sizes: List<PdfPageSize>? = null
    private var closed = false
    private val cache = LruCache<Int, Bitmap>(6)

    suspend fun pageSizes(): List<PdfPageSize> = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureOpen()
            sizes.orEmpty()
        }
    }

    suspend fun render(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache.get(pageIndex)?.let { return@withLock it }
            val r = ensureOpen() ?: return@withLock null
            if (pageIndex !in 0 until r.pageCount) return@withLock null
            r.openPage(pageIndex).use { page ->
                val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                val width = targetWidthPx
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(pageIndex, bitmap)
                bitmap
            }
        }
    }

    private fun ensureOpen(): PdfRenderer? {
        if (closed) return null
        renderer?.let { return it }
        return runCatching {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(pfd)
            descriptor = pfd
            renderer = r
            sizes = (0 until r.pageCount).map { index ->
                r.openPage(index).use { page -> PdfPageSize(page.width, page.height) }
            }
            r
        }.getOrNull()
    }

    fun close() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            mutex.withLock {
                closed = true
                runCatching { renderer?.close() }
                runCatching { descriptor?.close() }
                renderer = null
                descriptor = null
                cache.evictAll()
            }
        }
    }
}
