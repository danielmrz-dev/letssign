package br.com.fiap.letssign.core.security

import java.io.File
import java.security.MessageDigest

/** SHA-256 do arquivo, usado para comprovar que o PDF não mudou depois do envio. */
fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHex()
}
