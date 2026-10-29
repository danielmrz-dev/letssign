package br.com.fiap.letssign.core.storage

import android.content.Context
import java.io.File

/**
 * Os PDFs ficam no sistema de arquivos do app (opção 1 do enunciado):
 * filesDir/documents/<docId>/v<versão>.pdf. O Room guarda nome, caminho, data, hash e status.
 */
class DocumentStorage(context: Context) {
    private val root = File(context.filesDir, "documents")
    val importDir = File(context.cacheDir, "import")
    val signatureCacheDir = File(context.cacheDir, "signatures")

    fun documentDir(documentId: String) = File(root, documentId)

    fun versionFile(documentId: String, version: Int) = File(documentDir(documentId), "v$version.pdf")

    fun signedFile(documentId: String, version: Int, step: Int) =
        File(documentDir(documentId), "v${version}_assinado_$step.pdf")

    fun finalFile(documentId: String, version: Int) = File(documentDir(documentId), "v${version}_final.pdf")

    fun signatureFile(documentId: String, linkId: String) = File(documentDir(documentId), "assinatura_$linkId.png")

    fun deleteDocument(documentId: String) {
        documentDir(documentId).deleteRecursively()
    }
}
