package br.com.fiap.letssign.core.pdf

import android.graphics.BitmapFactory
import br.com.fiap.letssign.core.model.SignatureField
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File
import java.io.IOException

data class AuditSigner(val name: String, val email: String, val role: String, val signedAt: String)

data class AuditInfo(
    val title: String,
    val documentId: String,
    val versionNumber: Int,
    val originalSha256: String,
    val ownerName: String,
    val sentAt: String,
    val completedAt: String,
    val signers: List<AuditSigner>,
)

/**
 * Escreve no PDF com o PdfBox-Android. As coordenadas salvas (0..1, origem no canto superior
 * esquerdo) são convertidas para pontos do PDF, cujo eixo Y começa embaixo.
 * O arquivo original nunca é alterado: cada assinatura gera um novo arquivo.
 */
class PdfStamper {

    @Throws(IOException::class)
    fun stampSignature(input: File, output: File, signaturePng: File, fields: List<SignatureField>, label: String) {
        val bitmap = BitmapFactory.decodeFile(signaturePng.absolutePath)
            ?: throw IOException("Imagem da assinatura inválida.")
        PDDocument.load(input).use { document ->
            document.isAllSecurityToBeRemoved = true
            val image = LosslessFactory.createFromImage(document, bitmap)
            val font: PDFont = PDType1Font.HELVETICA
            fields.forEach { field ->
                if (field.pageIndex !in 0 until document.numberOfPages) return@forEach
                val page = document.getPage(field.pageIndex)
                val box = page.cropBox
                val pageW = box.width
                val pageH = box.height
                val x = box.lowerLeftX + field.xRatio * pageW
                val w = field.widthRatio * pageW
                val h = field.heightRatio * pageH
                val y = box.lowerLeftY + pageH - (field.yRatio + field.heightRatio) * pageH

                val labelH = (h * 0.26f).coerceIn(4f, 10f)
                val areaH = h - labelH
                val scale = minOf(w / image.width, areaH / image.height)
                val imageW = image.width * scale
                val imageH = image.height * scale

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { content ->
                    content.drawImage(image, x + (w - imageW) / 2f, y + labelH + (areaH - imageH) / 2f, imageW, imageH)

                    content.setStrokingColor(0.42f, 0.18f, 0.76f)
                    content.setLineWidth(0.6f)
                    content.moveTo(x, y + labelH)
                    content.lineTo(x + w, y + labelH)
                    content.stroke()

                    val fontSize = (labelH * 0.72f).coerceIn(3f, 7f)
                    content.beginText()
                    content.setFont(font, fontSize)
                    content.setNonStrokingColor(0.30f, 0.30f, 0.38f)
                    content.newLineAtOffset(x + 1f, y + (labelH - fontSize) / 2f + 0.6f)
                    content.showText(fitText(sanitize(label), font, fontSize, w - 2f))
                    content.endText()
                }
            }
            output.parentFile?.mkdirs()
            document.save(output)
        }
    }

    /** Página final de registro de assinaturas (comprovante de auditoria). */
    @Throws(IOException::class)
    fun appendAuditPage(input: File, output: File, info: AuditInfo) {
        PDDocument.load(input).use { document ->
            document.isAllSecurityToBeRemoved = true
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val regular: PDFont = PDType1Font.HELVETICA
            val bold: PDFont = PDType1Font.HELVETICA_BOLD
            val margin = 56f
            val maxWidth = PDRectangle.A4.width - margin * 2
            var cursorY = PDRectangle.A4.height - margin

            PDPageContentStream(document, page).use { content ->
                fun line(text: String, font: PDFont, size: Float, gap: Float = 6f, gray: Float = 0.1f) {
                    content.beginText()
                    content.setFont(font, size)
                    content.setNonStrokingColor(gray, gray, gray + 0.12f)
                    content.newLineAtOffset(margin, cursorY - size)
                    content.showText(fitText(sanitize(text), font, size, maxWidth))
                    content.endText()
                    cursorY -= size + gap
                }

                content.setNonStrokingColor(0.42f, 0.18f, 0.76f)
                content.addRect(margin, cursorY - 4f, 40f, 4f)
                content.fill()
                cursorY -= 18f

                line("Registro de assinaturas", bold, 18f, 4f)
                line("Gerado pelo selo. em parceria com a LetsSign", regular, 9f, 18f, 0.45f)

                line("Documento", bold, 11f, 3f)
                line(info.title, regular, 10f, 3f)
                line("ID ${info.documentId} · versão ${info.versionNumber}", regular, 9f, 12f, 0.4f)

                line("Integridade do arquivo original (SHA-256)", bold, 11f, 3f)
                line(info.originalSha256, regular, 8.5f, 12f, 0.35f)

                line("Enviado por ${info.ownerName} em ${info.sentAt}", regular, 10f, 3f)
                line("Concluído em ${info.completedAt}", regular, 10f, 16f)

                line("Signatários", bold, 11f, 6f)
                info.signers.forEachIndexed { index, signer ->
                    line("${index + 1}. ${signer.name} · ${signer.role}", bold, 10f, 2f)
                    line("${signer.email} · assinou em ${signer.signedAt}", regular, 9f, 10f, 0.4f)
                }

                cursorY -= 8f
                line(
                    "Cada assinatura foi confirmada no aparelho do signatário e aplicada nos campos indicados pelo remetente.",
                    regular,
                    8.5f,
                    4f,
                    0.45f,
                )
            }
            output.parentFile?.mkdirs()
            document.save(output)
        }
    }

    /** A fonte padrão (WinAnsi) só codifica Latin-1; caracteres fora disso são trocados. */
    private fun sanitize(text: String): String = buildString {
        text.forEach { c ->
            when {
                c == '—' || c == '–' -> append('-')
                c == '…' -> append("...")
                c.code in 32..126 || c.code in 160..255 -> append(c)
                else -> append(' ')
            }
        }
    }

    private fun fitText(text: String, font: PDFont, size: Float, maxWidth: Float): String {
        fun width(s: String) = runCatching { font.getStringWidth(s) / 1000f * size }.getOrDefault(0f)
        if (width(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && width(text.substring(0, end) + "...") > maxWidth) end--
        return text.substring(0, end) + "..."
    }
}
