package br.com.refrigeracaopro.data

import android.content.Context

/**
 * Leitura e gravação dos limites de alerta do MCA nas preferências do app.
 *
 * O tipo [Mca.LimitesMca] mora no motor de cálculo (Kotlin puro, testável);
 * aqui fica só a persistência, que depende do Android.
 */
object McaLimites {

    private const val PREFIXO = "mca_limite_"

    private fun chave(nome: String) = "$PREFIXO$nome"

    private fun ler(context: Context, nome: String, padrao: Double): Double {
        val salvo = Prefs.config(context).getFloat(chave(nome), Float.NaN)
        return if (salvo.isNaN()) padrao else salvo.toDouble()
    }

    /** Limites configurados, caindo nos defaults especificados quando não editados. */
    fun carregar(context: Context): Mca.LimitesMca {
        val padrao = Mca.LimitesMca()
        return Mca.LimitesMca(
            desbalR40Atencao = ler(context, "r40_atencao", padrao.desbalR40Atencao),
            desbalR40Critico = ler(context, "r40_critico", padrao.desbalR40Critico),
            desbalLAtencao = ler(context, "l_atencao", padrao.desbalLAtencao),
            desbalLCritico = ler(context, "l_critico", padrao.desbalLCritico),
            desbalZAtencao = ler(context, "z_atencao", padrao.desbalZAtencao),
            desbalZCritico = ler(context, "z_critico", padrao.desbalZCritico),
            deltaThetaAtencao = ler(context, "theta_atencao", padrao.deltaThetaAtencao),
            deltaThetaCritico = ler(context, "theta_critico", padrao.deltaThetaCritico),
            spreadIfAtencao = ler(context, "if_atencao", padrao.spreadIfAtencao),
            spreadIfCritico = ler(context, "if_critico", padrao.spreadIfCritico),
            desbalCAtencao = ler(context, "c_atencao", padrao.desbalCAtencao),
            desbalCCritico = ler(context, "c_critico", padrao.desbalCCritico),
            ricAmplitudeAtencao = ler(context, "ric_amp_atencao", padrao.ricAmplitudeAtencao),
            ricAmplitudeCritico = ler(context, "ric_amp_critico", padrao.ricAmplitudeCritico),
            ricSenoideAtencao = ler(context, "ric_seno_atencao", padrao.ricSenoideAtencao),
            ricSenoideCritico = ler(context, "ric_seno_critico", padrao.ricSenoideCritico),
            isolacaoAtencaoMOhm = ler(context, "isolacao_atencao", padrao.isolacaoAtencaoMOhm),
            isolacaoCriticoMOhm = ler(context, "isolacao_critico", padrao.isolacaoCriticoMOhm),
            piAtencao = ler(context, "pi_atencao", padrao.piAtencao),
            piCritico = ler(context, "pi_critico", padrao.piCritico),
        )
    }

    fun salvar(context: Context, limites: Mca.LimitesMca) {
        Prefs.config(context).edit().apply {
            putFloat(chave("r40_atencao"), limites.desbalR40Atencao.toFloat())
            putFloat(chave("r40_critico"), limites.desbalR40Critico.toFloat())
            putFloat(chave("l_atencao"), limites.desbalLAtencao.toFloat())
            putFloat(chave("l_critico"), limites.desbalLCritico.toFloat())
            putFloat(chave("z_atencao"), limites.desbalZAtencao.toFloat())
            putFloat(chave("z_critico"), limites.desbalZCritico.toFloat())
            putFloat(chave("theta_atencao"), limites.deltaThetaAtencao.toFloat())
            putFloat(chave("theta_critico"), limites.deltaThetaCritico.toFloat())
            putFloat(chave("if_atencao"), limites.spreadIfAtencao.toFloat())
            putFloat(chave("if_critico"), limites.spreadIfCritico.toFloat())
            putFloat(chave("c_atencao"), limites.desbalCAtencao.toFloat())
            putFloat(chave("c_critico"), limites.desbalCCritico.toFloat())
            putFloat(chave("ric_amp_atencao"), limites.ricAmplitudeAtencao.toFloat())
            putFloat(chave("ric_amp_critico"), limites.ricAmplitudeCritico.toFloat())
            putFloat(chave("ric_seno_atencao"), limites.ricSenoideAtencao.toFloat())
            putFloat(chave("ric_seno_critico"), limites.ricSenoideCritico.toFloat())
            putFloat(chave("isolacao_atencao"), limites.isolacaoAtencaoMOhm.toFloat())
            putFloat(chave("isolacao_critico"), limites.isolacaoCriticoMOhm.toFloat())
            putFloat(chave("pi_atencao"), limites.piAtencao.toFloat())
            putFloat(chave("pi_critico"), limites.piCritico.toFloat())
        }.apply()
    }

    /** Volta todos os limites aos valores de referência do módulo. */
    fun restaurarPadrao(context: Context) {
        val editor = Prefs.config(context).edit()
        Prefs.config(context).all.keys.filter { it.startsWith(PREFIXO) }.forEach { editor.remove(it) }
        editor.apply()
    }

    const val AVISO_REFERENCIA =
        "Os limites de isolação e PI seguem a IEEE 43 e são valores de referência. " +
            "O critério final é a tendência histórica do próprio motor, não o valor absoluto: " +
            "um motor que sempre mediu 800 MΩ e caiu para 250 MΩ merece atenção mesmo estando " +
            "acima do limite."
}
