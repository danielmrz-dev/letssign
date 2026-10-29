package br.com.fiap.letssign.feature.signers.domain

import br.com.fiap.letssign.core.model.User
import br.com.fiap.letssign.core.util.Validators
import kotlinx.coroutines.flow.Flow

enum class SignerFormField { NAME, EMAIL, PHONE, PASSWORD }

data class SignerInput(
    val fullName: String,
    val email: String,
    val phoneDigits: String,
    val password: String,
)

sealed interface SaveSignerResult {
    data object Success : SaveSignerResult
    data class Invalid(val errors: Map<SignerFormField, String>) : SaveSignerResult
}

interface SignerRepository {
    fun observeSigners(adminId: String): Flow<List<User>>
    suspend fun getSigner(adminId: String, signerId: String): User?
    suspend fun create(adminId: String, input: SignerInput): SaveSignerResult
    suspend fun update(adminId: String, signerId: String, input: SignerInput): SaveSignerResult
}

/** Regras do cadastro de signatário (a unicidade do e-mail é checada no repositório). */
object SignerValidator {
    fun validate(input: SignerInput, passwordRequired: Boolean): Map<SignerFormField, String> = buildMap {
        Validators.fullName(input.fullName)?.let { put(SignerFormField.NAME, it) }
        Validators.email(input.email)?.let { put(SignerFormField.EMAIL, it) }
        Validators.phone(input.phoneDigits)?.let { put(SignerFormField.PHONE, it) }
        Validators.password(input.password, passwordRequired)?.let { put(SignerFormField.PASSWORD, it) }
    }
}
