package br.com.refrigeracaopro.data

/**
 * Tabela de referência técnica dos principais fluidos refrigerantes.
 *
 * IMPORTANTE: valores aproximados, apenas para consulta geral em campo.
 * O técnico deve sempre confirmar com o fabricante e com a tabela P/T
 * oficial do fluido.
 */
data class GasRefrigerante(
    val nome: String,
    val tipo: String,
    val aplicacao: String,
    val pressoesTrabalho: String,
    val tempEvaporacao: String,
    val tempCondensacao: String,
    val oleoCompativel: String,
    val classeSeguranca: String,
    val gwp: String,
    val observacoes: String,
    val substitutos: String,
)

object GasesRefrigerantes {

    val LISTA = listOf(
        GasRefrigerante(
            nome = "R22",
            tipo = "HCFC",
            aplicacao = "Ar-condicionado e refrigeração comercial (sistemas antigos)",
            pressoesTrabalho = "Baixa: 4–6 bar | Alta: 15–19 bar",
            tempEvaporacao = "-40 °C a +5 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Mineral (MO) ou Alquilbenzeno (AB)",
            classeSeguranca = "A1",
            gwp = "≈ 1810",
            observacoes = "Em eliminação gradual (Protocolo de Montreal). Proibida fabricação/importação em equipamentos novos no Brasil.",
            substitutos = "R407C, R422D, R438A (retrofit); R32/R410A em equipamentos novos"
        ),
        GasRefrigerante(
            nome = "R134a",
            tipo = "HFC",
            aplicacao = "Refrigeração doméstica/comercial de média temperatura, ar-condicionado automotivo, bebedouros",
            pressoesTrabalho = "Baixa: 0,8–2 bar | Alta: 7–12 bar",
            tempEvaporacao = "-25 °C a +10 °C",
            tempCondensacao = "+30 °C a +60 °C",
            oleoCompativel = "Poliol éster (POE) / PAG (automotivo)",
            classeSeguranca = "A1",
            gwp = "≈ 1430",
            observacoes = "Muito comum em geladeiras e bebedouros. Não usar óleo mineral.",
            substitutos = "R1234yf (automotivo), R600a (doméstico), R513A"
        ),
        GasRefrigerante(
            nome = "R404A",
            tipo = "Mistura HFC (R125/R143a/R134a)",
            aplicacao = "Refrigeração comercial de baixa e média temperatura: câmaras frias, freezers, balcões",
            pressoesTrabalho = "Baixa: 1–4 bar | Alta: 16–22 bar",
            tempEvaporacao = "-45 °C a 0 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Poliol éster (POE)",
            classeSeguranca = "A1",
            gwp = "≈ 3922",
            observacoes = "GWP muito alto — em substituição na Europa e tendência mundial. Carga sempre em fase líquida (mistura zeotrópica, glide baixo).",
            substitutos = "R448A, R449A, R452A; CO2 em novos sistemas"
        ),
        GasRefrigerante(
            nome = "R407C",
            tipo = "Mistura HFC (R32/R125/R134a)",
            aplicacao = "Ar-condicionado e bombas de calor (substituto comum do R22)",
            pressoesTrabalho = "Baixa: 4–6 bar | Alta: 16–21 bar",
            tempEvaporacao = "-35 °C a +10 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Poliol éster (POE)",
            classeSeguranca = "A1",
            gwp = "≈ 1774",
            observacoes = "Glide de temperatura alto (≈ 5–7 K): atenção ao ajustar superaquecimento. Carga em fase líquida.",
            substitutos = "R32, R410A (equipamentos novos)"
        ),
        GasRefrigerante(
            nome = "R410A",
            tipo = "Mistura HFC (R32/R125)",
            aplicacao = "Ar-condicionado residencial e comercial (split, VRF)",
            pressoesTrabalho = "Baixa: 7–10 bar | Alta: 24–32 bar",
            tempEvaporacao = "-30 °C a +10 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Poliol éster (POE)",
            classeSeguranca = "A1",
            gwp = "≈ 2088",
            observacoes = "Pressões ~60% maiores que R22: usar manifold, mangueiras e componentes específicos. Glide desprezível.",
            substitutos = "R32 (equipamentos novos)"
        ),
        GasRefrigerante(
            nome = "R507",
            tipo = "Mistura azeotrópica HFC (R125/R143a)",
            aplicacao = "Refrigeração comercial/industrial de baixa temperatura",
            pressoesTrabalho = "Baixa: 1–4 bar | Alta: 17–23 bar",
            tempEvaporacao = "-45 °C a 0 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Poliol éster (POE)",
            classeSeguranca = "A1",
            gwp = "≈ 3985",
            observacoes = "Comportamento semelhante ao R404A, sem glide (azeótropo). GWP muito alto.",
            substitutos = "R448A, R449A; CO2/NH3 em novos sistemas"
        ),
        GasRefrigerante(
            nome = "R32",
            tipo = "HFC",
            aplicacao = "Ar-condicionado split e VRF de nova geração",
            pressoesTrabalho = "Baixa: 8–11 bar | Alta: 26–35 bar",
            tempEvaporacao = "-30 °C a +10 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Poliol éster (POE) / PVE",
            classeSeguranca = "A2L (levemente inflamável)",
            gwp = "≈ 675",
            observacoes = "Levemente inflamável: ventilar o ambiente, não usar chama aberta sem recolhimento, ferramentas adequadas A2L. Temperatura de descarga alta.",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "R290 (Propano)",
            tipo = "HC (hidrocarboneto natural)",
            aplicacao = "Refrigeração comercial leve: balcões, expositores, freezers plug-in",
            pressoesTrabalho = "Baixa: 1–5 bar | Alta: 12–18 bar",
            tempEvaporacao = "-40 °C a +5 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Mineral (MO) ou POE",
            classeSeguranca = "A3 (inflamável)",
            gwp = "≈ 3",
            observacoes = "ALTAMENTE INFLAMÁVEL: carga limitada por norma, jamais soldar com gás no sistema, ambiente ventilado, detector de gás. Excelente eficiência.",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "R600a (Isobutano)",
            tipo = "HC (hidrocarboneto natural)",
            aplicacao = "Refrigeração doméstica: geladeiras e freezers residenciais",
            pressoesTrabalho = "Baixa: vácuo a 0,8 bar | Alta: 4–7 bar",
            tempEvaporacao = "-25 °C a 0 °C",
            tempCondensacao = "+30 °C a +55 °C",
            oleoCompativel = "Mineral (MO)",
            classeSeguranca = "A3 (inflamável)",
            gwp = "≈ 3",
            observacoes = "INFLAMÁVEL. Pressão de sucção frequentemente em vácuo — atenção a infiltração de ar/umidade. Carga pequena, pesada em balança de precisão.",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "R1234yf",
            tipo = "HFO",
            aplicacao = "Ar-condicionado automotivo de nova geração",
            pressoesTrabalho = "Baixa: 1–3 bar | Alta: 7–14 bar",
            tempEvaporacao = "-20 °C a +10 °C",
            tempCondensacao = "+35 °C a +65 °C",
            oleoCompativel = "PAG específico / POE",
            classeSeguranca = "A2L (levemente inflamável)",
            gwp = "< 1",
            observacoes = "Substituto direto do R134a no setor automotivo. Pressões similares ao R134a. Custo elevado.",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "R1234ze",
            tipo = "HFO",
            aplicacao = "Chillers, bombas de calor e refrigeração de média temperatura",
            pressoesTrabalho = "Baixa: 0,3–2 bar | Alta: 5–10 bar",
            tempEvaporacao = "-15 °C a +10 °C",
            tempCondensacao = "+30 °C a +60 °C",
            oleoCompativel = "Poliol éster (POE)",
            classeSeguranca = "A2L (levemente inflamável)",
            gwp = "< 1",
            observacoes = "Capacidade volumétrica menor que R134a (~75%). Usado em chillers de alta eficiência.",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "CO2 / R744",
            tipo = "Natural (dióxido de carbono)",
            aplicacao = "Supermercados (racks transcríticos), câmaras frias industriais, bombas de calor",
            pressoesTrabalho = "Baixa: 10–30 bar | Alta: 45–120 bar (transcrítico)",
            tempEvaporacao = "-50 °C a +5 °C",
            tempCondensacao = "Crítica em 31 °C (acima: operação transcrítica)",
            oleoCompativel = "POE específico para CO2",
            classeSeguranca = "A1",
            gwp = "1",
            observacoes = "PRESSÕES MUITO ELEVADAS: exige componentes, mangueiras e treinamento específicos. Acima de 31 °C não há condensação (gas cooler).",
            substitutos = "—"
        ),
        GasRefrigerante(
            nome = "Amônia / R717",
            tipo = "Natural (NH3)",
            aplicacao = "Refrigeração industrial: frigoríficos, laticínios, grandes câmaras",
            pressoesTrabalho = "Baixa: 0,5–3 bar | Alta: 10–16 bar",
            tempEvaporacao = "-50 °C a +5 °C",
            tempCondensacao = "+25 °C a +40 °C",
            oleoCompativel = "Mineral específico (não miscível)",
            classeSeguranca = "B2L (tóxica e levemente inflamável)",
            gwp = "0",
            observacoes = "TÓXICA: exige EPI completo, detecção de vazamento e procedimentos de segurança rigorosos. Incompatível com cobre — tubulação de aço.",
            substitutos = "—"
        ),
    )

    const val AVISO = "Valores aproximados para referência geral. Consulte sempre o fabricante " +
        "do equipamento e a tabela P/T oficial do fluido antes de qualquer intervenção."
}
