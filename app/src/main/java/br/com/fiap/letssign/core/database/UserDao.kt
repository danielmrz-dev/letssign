package br.com.fiap.letssign.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM users WHERE role = 'SIGNER' AND createdBy = :adminId")
    suspend fun countSigners(adminId: String): Int

    /** Isolamento: o Admin só enxerga os signatários que ele mesmo cadastrou. */
    @Query("SELECT * FROM users WHERE role = 'SIGNER' AND createdBy = :adminId ORDER BY fullName COLLATE NOCASE")
    fun observeSigners(adminId: String): Flow<List<UserEntity>>
}
