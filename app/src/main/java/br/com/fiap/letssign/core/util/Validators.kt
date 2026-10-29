package br.com.fiap.letssign.core.util

/** Validações de entrada. Cada função devolve a mensagem de erro ou null quando está tudo certo. */
object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val TITLE_MAX = 120
    const val NAME_MAX = 120
    const val PASSWORD_MIN = 8
    const val INVITE_MAX = 500

    fun normalizeEmail(value: String): String = value.trim().lowercase()

    fun fullName(value: String): String? {
        val words = value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            value.isBlank() -> "Informe o nome completo."
            words.size < 2 -> "Informe nome e sobrenome."
            value.trim().length > NAME_MAX -> "O nome pode ter até $NAME_MAX caracteres."
            else -> null
        }
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Informe o e-mail."
        !emailRegex.matches(value.trim()) -> "Informe um e-mail válido, como nome@empresa.com.br."
        else -> null
    }

    /** Telefone brasileiro com DDD: 10 ou 11 dígitos. */
    fun phone(value: String): String? {
        val digits = value.filter { it.isDigit() }
        val ddd = digits.take(2).toIntOrNull() ?: 0
        return when {
            digits.isEmpty() -> "Informe o telefone com DDD."
            digits.length !in 10..11 || ddd < 11 -> "Telefone incompleto. Use o formato (11) 91234-5678."
            else -> null
        }
    }

    fun password(value: String, required: Boolean): String? = when {
        value.isEmpty() && !required -> null
        value.isEmpty() -> "Crie uma senha."
        value.length < PASSWORD_MIN -> "A senha precisa ter no mínimo $PASSWORD_MIN caracteres."
        else -> null
    }

    fun title(value: String): String? = when {
        value.isBlank() -> "Informe o título do documento."
        value.trim().length > TITLE_MAX -> "O título pode ter até $TITLE_MAX caracteres."
        else -> null
    }
}
