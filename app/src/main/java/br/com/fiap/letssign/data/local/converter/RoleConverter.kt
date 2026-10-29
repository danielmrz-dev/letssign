package br.com.fiap.letssign.data.local.converter

import androidx.room.TypeConverter
import br.com.fiap.letssign.data.model.UserRole

/**
 * TypeConverter do Room para conversão do enum UserRole de e para String.
 */
class RoleConverter {

    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(roleString: String): UserRole {
        return try {
            UserRole.valueOf(roleString)
        } catch (e: IllegalArgumentException) {
            UserRole.SIGNATARIO
        }
    }
}
