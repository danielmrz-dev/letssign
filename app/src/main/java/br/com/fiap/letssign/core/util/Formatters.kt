package br.com.fiap.letssign.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase(ptBr)
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase(ptBr)
        }
    }

    /** 27/10 às 10:20 */
    fun dateTime(millis: Long): String = SimpleDateFormat("dd/MM 'às' HH:mm", ptBr).format(Date(millis))

    /** 27/10/2026 às 10:20 */
    fun fullDateTime(millis: Long): String = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBr).format(Date(millis))

    fun fileSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(ptBr, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }

    /** Formata o telefone progressivamente: (11) 98765-4321 ou (11) 8765-4321. */
    fun phone(digitsInput: String): String {
        val d = digitsInput.filter { it.isDigit() }.take(11)
        if (d.isEmpty()) return ""
        if (d.length <= 2) return "(" + d
        val ddd = d.substring(0, 2)
        val rest = d.substring(2)
        val splitAt = if (d.length == 11) 5 else 4
        return if (rest.length <= splitAt) {
            "($ddd) $rest"
        } else {
            "($ddd) ${rest.substring(0, splitAt)}-${rest.substring(splitAt)}"
        }
    }

    fun shortHash(hash: String): String =
        if (hash.length <= 12) hash else hash.take(6) + "…" + hash.takeLast(4)

    fun plural(count: Int, singular: String, plural: String): String =
        "$count ${if (count == 1) singular else plural}"
}
