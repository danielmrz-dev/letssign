package br.com.fiap.letssign.feature.auth.domain

import br.com.fiap.letssign.core.model.User

/** Contrato de autenticação. Na Sprint 2 a implementação local dá lugar ao login via API (JWT). */
interface AuthRepository {
    /** Retorna o usuário autenticado ou null quando e-mail ou senha não conferem. */
    suspend fun login(email: String, password: String): User?
    suspend fun logout()
}
