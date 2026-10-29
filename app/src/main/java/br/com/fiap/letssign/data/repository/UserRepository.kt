package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.entity.UserEntity

/**
 * Interface para abstração da fonte de dados de usuários.
 */
interface UserRepository {
    suspend fun getUserByEmail(email: String): UserEntity?
    suspend fun getUserById(id: Long): UserEntity?
    suspend fun insertUser(user: UserEntity): Long
    suspend fun getUserCount(): Int
    suspend fun seedDatabaseIfEmpty()
}
