package br.com.refrigeracaopro.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Preferências do app.
 *
 * - Dados da empresa/técnico: SharedPreferences comum.
 * - Chave da OpenAI: EncryptedSharedPreferences (AES-256), nunca exposta no código.
 */
object Prefs {

    private const val ARQ_CONFIG = "config"
    private const val ARQ_SEGURO = "config_segura"

    fun config(context: Context): SharedPreferences =
        context.getSharedPreferences(ARQ_CONFIG, Context.MODE_PRIVATE)

    /** Preferências criptografadas para dados sensíveis (chave OpenAI). */
    fun seguras(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            ARQ_SEGURO,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ----- Dados da empresa / técnico -----
    var Context.nomeEmpresa: String
        get() = config(this).getString("nome_empresa", "") ?: ""
        set(v) = config(this).edit().putString("nome_empresa", v).apply()

    var Context.cnpjEmpresa: String
        get() = config(this).getString("cnpj_empresa", "") ?: ""
        set(v) = config(this).edit().putString("cnpj_empresa", v).apply()

    var Context.telefoneEmpresa: String
        get() = config(this).getString("tel_empresa", "") ?: ""
        set(v) = config(this).edit().putString("tel_empresa", v).apply()

    var Context.emailEmpresa: String
        get() = config(this).getString("email_empresa", "") ?: ""
        set(v) = config(this).edit().putString("email_empresa", v).apply()

    var Context.enderecoEmpresa: String
        get() = config(this).getString("end_empresa", "") ?: ""
        set(v) = config(this).edit().putString("end_empresa", v).apply()

    var Context.logoEmpresa: String
        get() = config(this).getString("logo_empresa", "") ?: ""
        set(v) = config(this).edit().putString("logo_empresa", v).apply()

    var Context.nomeTecnico: String
        get() = config(this).getString("nome_tecnico", "") ?: ""
        set(v) = config(this).edit().putString("nome_tecnico", v).apply()

    var Context.registroTecnico: String
        get() = config(this).getString("registro_tecnico", "") ?: ""
        set(v) = config(this).edit().putString("registro_tecnico", v).apply()

    // ----- IA -----
    var Context.chaveOpenAi: String
        get() = seguras(this).getString("openai_key", "") ?: ""
        set(v) = seguras(this).edit().putString("openai_key", v).apply()

    var Context.modeloIa: String
        get() = config(this).getString("modelo_ia", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(v) = config(this).edit().putString("modelo_ia", v).apply()

    var Context.iaAtiva: Boolean
        get() = config(this).getBoolean("ia_ativa", true)
        set(v) = config(this).edit().putBoolean("ia_ativa", v).apply()

    // ----- Sessão (mantém o usuário logado entre aberturas do app) -----
    var Context.sessaoAtiva: Boolean
        get() = config(this).getBoolean("sessao_ativa", false)
        set(v) = config(this).edit().putBoolean("sessao_ativa", v).apply()

    // ----- Backup automático no Google Drive -----
    var Context.backupAutomatico: Boolean
        get() = config(this).getBoolean("backup_auto", false)
        set(v) = config(this).edit().putBoolean("backup_auto", v).apply()

    /** "Diária" ou "Semanal". */
    var Context.backupFrequencia: String
        get() = config(this).getString("backup_freq", "Diária") ?: "Diária"
        set(v) = config(this).edit().putString("backup_freq", v).apply()
}
