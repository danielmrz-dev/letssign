package br.com.fiap.letssign.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        DocumentEntity::class,
        DocumentVersionEntity::class,
        DocumentSignerEntity::class,
        SignatureFieldEntity::class,
        DocumentEventEntity::class,
        SendRequestEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SeloDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao

    companion object {
        fun build(context: Context): SeloDatabase =
            Room.databaseBuilder(context, SeloDatabase::class.java, "selo.db").build()
    }
}
