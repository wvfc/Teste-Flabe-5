package br.com.refrigeracaopro.data

/** Avisos técnicos e de segurança exibidos no app e nos PDFs. */
object Avisos {

    const val CALCULO_AUXILIAR =
        "Este cálculo é apenas auxiliar/estimativo e NÃO substitui a análise técnica " +
            "nem os dados da plaqueta/manual do fabricante."

    /** Lista de boas práticas de segurança em refrigeração. */
    val SEGURANCA = listOf(
        "Sempre recolha o fluido refrigerante corretamente (recolhedora).",
        "Nunca libere fluido refrigerante na atmosfera.",
        "Utilize EPI adequado (luvas, óculos e proteção respiratória).",
        "Confirme todos os dados no manual/plaqueta do fabricante.",
        "Os cálculos do app são auxiliares e não substituem a análise técnica.",
    )

    const val RODAPE_SEGURANCA =
        "Recolha o fluido corretamente • Não libere gás na atmosfera • Use EPI • " +
            "Confira o manual do fabricante • Cálculos auxiliares não substituem análise técnica."
}
