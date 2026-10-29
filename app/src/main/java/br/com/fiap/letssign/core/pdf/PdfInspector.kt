package br.com.fiap.letssign.core.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import br.com.fiap.letssign.core.model.ImportedPdf
import br.com.fiap.letssign.core.security.sha256Hex
import br.com.fiap.letssign.core.storage.DocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

sealed interface PdfImportResult {
    data class Success(val pdf: ImportedPdf) : PdfImportResult
    data class Error(val message: String) : PdfImportResult
}

/**
 * Importa o PDF escolhido pelo Storage Access Framework e valida antes de aceitar:
 * tipo, tamanho (até 20 MB), cabeçalho, senha, arquivo corrompido e quantidade de páginas.
 */
class PdfInspector(
    private val context: Context,
    private val storage: DocumentStorage,
) {
    suspend fun import(uri: Uri): PdfImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        if (mime != null && mime != PDF_MIME) {
            return@withContext PdfImportResult.Error("O arquivo escolhido não é um PDF. Selecione um arquivo .pdf.")
        }

        var displayName = "documento.pdf"
        var declaredSize = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) declaredSize = cursor.getLong(sizeIndex)
                }
            }
        }
        if (declaredSize > MAX_BYTES) return@withContext tooLarge()

        storage.importDir.mkdirs()
        val temp = File(storage.importDir, "${UUID.randomUUID()}.pdf")
        try {
            val copied = resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_BYTES) return@use -1L
                        output.write(buffer, 0, read)
                    }
                    total
                }
            } ?: return@withContext failure(temp, "Não foi possível ler o arquivo selecionado.")

            if (copied < 0) {
                temp.delete()
                return@withContext tooLarge()
            }
            if (copied == 0L) return@withContext failure(temp, "O arquivo está vazio.")
            if (!hasPdfHeader(temp)) return@withContext failure(temp, CORRUPTED)

            val pageCount = try {
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer -> renderer.pageCount }
                }
            } catch (e: SecurityException) {
                return@withContext failure(temp, "Este PDF está protegido por senha. Remova a senha e tente de novo.")
            } catch (e: IOException) {
                return@withContext failure(temp, CORRUPTED)
            } catch (e: Exception) {
                return@withContext failure(temp, "Não foi possível processar este PDF. Tente outro arquivo.")
            }
            if (pageCount <= 0) return@withContext failure(temp, "O PDF não tem páginas.")

            val name = if (displayName.lowercase().endsWith(".pdf")) displayName else "$displayName.pdf"
            PdfImportResult.Success(
                ImportedPdf(
                    file = temp,
                    fileName = name,
                    size = copied,
                    pageCount = pageCount,
                    sha256 = temp.sha256Hex(),
                ),
            )
        } catch (e: IOException) {
            failure(temp, "Não foi possível ler o arquivo selecionado.")
        } catch (e: SecurityException) {
            failure(temp, "Sem permissão para abrir este arquivo. Escolha-o de novo.")
        }
    }

    private fun hasPdfHeader(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(1024)
        val read = input.read(header)
        read > 4 && String(header, 0, read, Charsets.ISO_8859_1).contains("%PDF-")
    }

    private fun tooLarge() = PdfImportResult.Error("O PDF tem mais de 20 MB. Escolha um arquivo menor.")

    private fun failure(temp: File, message: String?): PdfImportResult.Error {
        temp.delete()
        return PdfImportResult.Error(message ?: CORRUPTED)
    }

    companion object {
        const val PDF_MIME = "application/pdf"
        const val MAX_BYTES = 20L * 1024L * 1024L
        private const val CORRUPTED = "Este arquivo está corrompido ou não é um PDF válido."
    }
}
