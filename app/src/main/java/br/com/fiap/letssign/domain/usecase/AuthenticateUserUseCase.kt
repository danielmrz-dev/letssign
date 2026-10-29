package br.com.fiap.letssign.domain.usecase

import br.com.fiap.letssign.core.security.PasswordHasher
import br.com.fiap.letssign.core.security.PasswordHasherImpl
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.repository.UserRepository

/**
 * Encapsula o resultado de autenticação do usuário.
 */
sealed interface AuthResult {
    data class Success(val user: UserEntity) : AuthResult
    data object InvalidCredentials : AuthResult
}

/**
 * UseCase responsável pela autenticação do usuário com validação de senha por Hash e Salt.
 */
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher
) {

    suspend operator fun invoke(rawEmail: String, rawPassword: String): AuthResult {
        val sanitizedEmail = rawEmail.trim().lowercase()

        // Garante a existência do seed inicial
        userRepository.seedDatabaseIfEmpty()

        val user = userRepository.getUserByEmail(sanitizedEmail)

        if (user == null) {
            // Mitigação de timing attack: executa um cálculo de hash igual caso o e-mail não exista
            if (passwordHasher is PasswordHasherImpl) {
                passwordHasher.performDummyVerification(rawPassword)
            } else {
                passwordHasher.verifyPassword(rawPassword, "dummy_salt", "dummy_hash")
            }
            return AuthResult.InvalidCredentials
        }

        val isPasswordCorrect = passwordHasher.verifyPassword(
            password = rawPassword,
            saltBase64 = user.salt,
            expectedHashBase64 = user.passwordHash
        )

        return if (isPasswordCorrect) {
            AuthResult.Success(user)
        } else {
            AuthResult.InvalidCredentials
        }
    }
}
