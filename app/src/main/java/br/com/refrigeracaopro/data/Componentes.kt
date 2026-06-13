package br.com.refrigeracaopro.data

/**
 * Base de componentes de refrigeração para o módulo de comparação de marcas/
 * modelos. Dados aproximados para orientação — confirme com o fabricante.
 */
data class Componente(
    val categoria: String,
    val marca: String,
    val modelo: String,
    val aplicacao: String,
    val capacidade: String,
    val fluido: String,
    val tensao: String,
    val consumo: String,
    val dimensoes: String,
    val conexoes: String,
    val equivalentes: String,
    val vantagens: String,
    val limitacoes: String,
    val observacoes: String,
)

object Componentes {

    val CATEGORIAS = listOf(
        "Compressor", "Unidade condensadora", "Evaporador", "Motor ventilador",
        "Controlador", "Válvula de expansão", "Pressostato", "Filtro secador",
    )

    val LISTA = listOf(
        Componente(
            categoria = "Unidade condensadora", marca = "Elgin", modelo = "UCM 2050",
            aplicacao = "Média temperatura", capacidade = "≈ 2.000 kcal/h",
            fluido = "R404A/R134a", tensao = "220 V mono", consumo = "≈ 750 W",
            dimensoes = "60×40×40 cm", conexoes = "Solda 1/4\" e 1/2\"",
            equivalentes = "Tecumseh, Embraco condensing units de mesma capacidade",
            vantagens = "Boa disponibilidade de peças no Brasil",
            limitacoes = "Verificar compatibilidade de fluido e óleo",
            observacoes = "Conferir envelope de trabalho do compressor embarcado."
        ),
        Componente(
            categoria = "Motor ventilador", marca = "Elco", modelo = "VN 10-20",
            aplicacao = "Evaporador/condensador", capacidade = "1.300 rpm",
            fluido = "—", tensao = "220 V mono", consumo = "16 W",
            dimensoes = "Eixo duplo Ø10 mm", conexoes = "3 fios + capacitor",
            equivalentes = "Weiguang YWF, Kostal de mesma potência e rotação",
            vantagens = "Baixo consumo, rolamento blindado",
            limitacoes = "Confirmar sentido de giro e diâmetro do eixo",
            observacoes = "Capacitor permanente conforme plaqueta."
        ),
        Componente(
            categoria = "Válvula de expansão", marca = "Danfoss", modelo = "TUA/TUB (R404A)",
            aplicacao = "Baixa/Média temperatura", capacidade = "Orifício intercambiável",
            fluido = "R404A", tensao = "—", consumo = "—",
            dimensoes = "Conexão 1/4\"×1/2\"", conexoes = "Solda/flare + equalização externa",
            equivalentes = "Emerson/Alco TX6, Eietz equivalentes por fluido e capacidade",
            vantagens = "Orifício intercambiável, ampla faixa",
            limitacoes = "Selecionar orifício correto para a capacidade",
            observacoes = "Posicionar bulbo corretamente e isolar."
        ),
        Componente(
            categoria = "Pressostato", marca = "Danfoss", modelo = "KP15",
            aplicacao = "Alta e baixa pressão (dual)", capacidade = "Ajuste manual",
            fluido = "Compatível HFC/HCFC", tensao = "Contato seco", consumo = "—",
            dimensoes = "Conexão 1/4\" SAE", conexoes = "Capilar + terminais",
            equivalentes = "Saginomiya, Eliwell de dupla pressão",
            vantagens = "Robusto, rearme manual/automático",
            limitacoes = "Conferir faixa de ajuste e reset",
            observacoes = "Ajustar conforme fluido e aplicação."
        ),
        Componente(
            categoria = "Filtro secador", marca = "Danfoss", modelo = "DML 053 (3/8\")",
            aplicacao = "Linha de líquido", capacidade = "Sólido molecular",
            fluido = "HFC/HCFC", tensao = "—", consumo = "—",
            dimensoes = "3/8\" solda", conexoes = "Solda",
            equivalentes = "Emerson EK, Suniso de mesma conexão",
            vantagens = "Boa retenção de umidade e partículas",
            limitacoes = "Trocar sempre que abrir o sistema",
            observacoes = "Respeitar sentido do fluxo (seta)."
        ),
        Componente(
            categoria = "Controlador", marca = "Full Gauge", modelo = "MT-512E 2HP",
            aplicacao = "Controle de temperatura com degelo", capacidade = "2 HP por relé",
            fluido = "—", tensao = "115/220 V", consumo = "≈ 3 W",
            dimensoes = "76×34 mm (frontal)", conexoes = "Bornes + sensor NTC",
            equivalentes = "Eliwell, Coel, Ageon de mesma faixa",
            vantagens = "Degelo programável, fácil parametrização",
            limitacoes = "Conferir corrente máxima do relé",
            observacoes = "Usar sensor NTC compatível."
        ),
        Componente(
            categoria = "Evaporador", marca = "Trineva", modelo = "TVE 2.5",
            aplicacao = "Câmara de média temperatura", capacidade = "≈ 2.500 kcal/h",
            fluido = "R404A/R134a", tensao = "220 V (ventiladores)", consumo = "≈ 60 W",
            dimensoes = "Forçador compacto", conexoes = "Solda + dreno",
            equivalentes = "Elgin, Mipal de mesma capacidade",
            vantagens = "Boa troca térmica, degelo elétrico opcional",
            limitacoes = "Dimensionar pela carga térmica real",
            observacoes = "Prever degelo adequado para baixa temperatura."
        ),
    )

    fun pesquisar(categoria: String?, termo: String): List<Componente> =
        LISTA.filter {
            (categoria == null || it.categoria == categoria) &&
                (termo.isBlank() || it.modelo.contains(termo, true) ||
                    it.marca.contains(termo, true) || it.aplicacao.contains(termo, true))
        }
}
