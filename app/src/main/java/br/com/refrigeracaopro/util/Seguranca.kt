package br.com.refrigeracaopro.util

import java.security.MessageDigest
import java.security.SecureRandom

/** Hash de senhas do login local (SHA-256 + salt aleatório). */
object Seguranca {

    fun gerarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(texto: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + texto).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun conferir(texto: String, salt: String, hashEsperado: String): Boolean =
        hash(texto, salt) == hashEsperado
}
