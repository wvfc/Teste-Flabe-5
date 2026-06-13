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
        // ---- Compressores ----
        Componente(
            categoria = "Compressor", marca = "Embraco", modelo = "EMIS70HER", aplicacao = "Média/Alta temperatura",
            capacidade = "≈ 280 W @ -10°C", fluido = "R134a", tensao = "220 V mono", consumo = "≈ 1,2 A",
            dimensoes = "Hermético pequeno", conexoes = "Solda",
            equivalentes = "Tecumseh AE, Secop de mesma capacidade e fluido",
            vantagens = "Baixo custo, ampla disponibilidade", limitacoes = "Somente R134a média/alta",
            observacoes = "Refrigeração comercial leve."
        ),
        Componente(
            categoria = "Compressor", marca = "Tecumseh", modelo = "AE4440Y-FZ1A", aplicacao = "Baixa/Média temperatura",
            capacidade = "≈ 1.050 W @ -10°C", fluido = "R404A", tensao = "220 V mono", consumo = "≈ 4,1 A",
            dimensoes = "Hermético médio", conexoes = "Rotalock",
            equivalentes = "Danfoss NTZ, Embraco NJ de mesma faixa",
            vantagens = "Robusto, óleo POE", limitacoes = "Confirmar capacidade na temperatura de projeto",
            observacoes = "Comercial baixa/média."
        ),
        Componente(
            categoria = "Compressor", marca = "Danfoss/Secop", modelo = "SC15G", aplicacao = "Média temperatura",
            capacidade = "≈ 1.350 W @ +5°C", fluido = "R134a", tensao = "220 V mono", consumo = "≈ 4,0 A",
            dimensoes = "Hermético médio", conexoes = "Solda",
            equivalentes = "Tecumseh TFH, Embraco NT de mesma capacidade",
            vantagens = "Eficiente, silencioso", limitacoes = "Média temperatura",
            observacoes = "Substituto comum de média temperatura."
        ),
        Componente(
            categoria = "Compressor", marca = "Copeland", modelo = "ZB21KQE", aplicacao = "Média temperatura",
            capacidade = "≈ 6.300 W @ -5°C", fluido = "R404A/R134a", tensao = "380 V tri", consumo = "≈ 5,2 A",
            dimensoes = "Scroll", conexoes = "Solda",
            equivalentes = "Bitzer, Danfoss scroll de igual capacidade",
            vantagens = "Scroll eficiente, baixa vibração", limitacoes = "Exige rede trifásica",
            observacoes = "Comercial de maior porte."
        ),

        // ---- Unidades condensadoras ----
        Componente(
            categoria = "Unidade condensadora", marca = "Elgin", modelo = "UCM 2050", aplicacao = "Média temperatura",
            capacidade = "≈ 2.000 kcal/h", fluido = "R404A/R134a", tensao = "220 V mono", consumo = "≈ 750 W",
            dimensoes = "60×40×40 cm", conexoes = "Solda 1/4\" e 1/2\"",
            equivalentes = "Tecumseh, Embraco de mesma capacidade",
            vantagens = "Disponibilidade de peças", limitacoes = "Conferir fluido e óleo",
            observacoes = "Conferir envelope do compressor embarcado."
        ),
        Componente(
            categoria = "Unidade condensadora", marca = "Tecumseh", modelo = "UTH2480Z", aplicacao = "Baixa temperatura",
            capacidade = "≈ 1.500 kcal/h @ -25°C", fluido = "R404A", tensao = "220 V mono", consumo = "≈ 1.100 W",
            dimensoes = "Compacta", conexoes = "Solda",
            equivalentes = "Elgin, Danfoss Optyma de igual capacidade",
            vantagens = "Pronta para baixa temperatura", limitacoes = "Verificar separador/acumulador",
            observacoes = "Para câmaras de congelados."
        ),
        Componente(
            categoria = "Unidade condensadora", marca = "Danfoss", modelo = "Optyma Plus", aplicacao = "Média/Baixa temperatura",
            capacidade = "Diversas faixas", fluido = "R404A/R448A/R134a", tensao = "220/380 V", consumo = "Conforme modelo",
            dimensoes = "Gabinete fechado", conexoes = "Rotalock/solda",
            equivalentes = "Bitzer LH, Embraco UR de igual capacidade",
            vantagens = "Gabinete acústico, pronta", limitacoes = "Custo maior",
            observacoes = "Selecionar pela carga térmica."
        ),

        // ---- Evaporadores ----
        Componente(
            categoria = "Evaporador", marca = "Trineva", modelo = "TVE 2.5", aplicacao = "Câmara média temperatura",
            capacidade = "≈ 2.500 kcal/h", fluido = "R404A/R134a", tensao = "220 V", consumo = "≈ 60 W",
            dimensoes = "Forçador compacto", conexoes = "Solda + dreno",
            equivalentes = "Elgin, Mipal de mesma capacidade",
            vantagens = "Boa troca térmica", limitacoes = "Dimensionar pela carga térmica",
            observacoes = "Prever degelo adequado."
        ),
        Componente(
            categoria = "Evaporador", marca = "Elgin", modelo = "VEF 3.0", aplicacao = "Câmara baixa temperatura",
            capacidade = "≈ 3.000 kcal/h", fluido = "R404A", tensao = "220 V", consumo = "≈ 90 W + degelo",
            dimensoes = "Forçador médio", conexoes = "Solda + dreno aquecido",
            equivalentes = "Trineva, Mipal de igual capacidade",
            vantagens = "Degelo elétrico integrado", limitacoes = "Maior consumo no degelo",
            observacoes = "Para congelados."
        ),
        Componente(
            categoria = "Evaporador", marca = "Mipal", modelo = "MFA 2.0", aplicacao = "Balcão/expositor",
            capacidade = "≈ 2.000 kcal/h", fluido = "R134a/R290", tensao = "220 V", consumo = "≈ 45 W",
            dimensoes = "Compacto", conexoes = "Solda",
            equivalentes = "Elgin, Trineva compactos",
            vantagens = "Compacto, leve", limitacoes = "Capacidade limitada",
            observacoes = "Conferir compatibilidade com R290 (inflamável)."
        ),

        // ---- Motores ventiladores ----
        Componente(
            categoria = "Motor ventilador", marca = "Elco", modelo = "VN 10-20", aplicacao = "Evaporador/condensador",
            capacidade = "1.300 rpm", fluido = "—", tensao = "220 V mono", consumo = "16 W",
            dimensoes = "Eixo duplo Ø10 mm", conexoes = "3 fios + capacitor",
            equivalentes = "Weiguang YWF, Kostal de mesma rotação",
            vantagens = "Baixo consumo, rolamento blindado", limitacoes = "Confirmar sentido de giro/eixo",
            observacoes = "Capacitor permanente conforme plaqueta."
        ),
        Componente(
            categoria = "Motor ventilador", marca = "Weiguang", modelo = "YWF4E-300", aplicacao = "Condensador",
            capacidade = "1.450 rpm / Ø300 mm", fluido = "—", tensao = "220 V mono", consumo = "55 W",
            dimensoes = "Hélice Ø300", conexoes = "Fios + capacitor",
            equivalentes = "Elco, Ziehl de mesmo diâmetro/rotação",
            vantagens = "Boa vazão de ar", limitacoes = "Conferir grade e fixação",
            observacoes = "Verificar IP para uso externo."
        ),
        Componente(
            categoria = "Motor ventilador", marca = "Kostal", modelo = "C-5-13/35", aplicacao = "Evaporador",
            capacidade = "1.550 rpm", fluido = "—", tensao = "220 V mono", consumo = "10 W",
            dimensoes = "Eixo Ø em mm conforme modelo", conexoes = "3 fios",
            equivalentes = "Elco VN, Weiguang pequenos",
            vantagens = "Compacto para evaporador", limitacoes = "Baixa potência",
            observacoes = "Uso interno em forçadores."
        ),

        // ---- Controladores ----
        Componente(
            categoria = "Controlador", marca = "Full Gauge", modelo = "MT-512E 2HP", aplicacao = "Temperatura c/ degelo",
            capacidade = "2 HP por relé", fluido = "—", tensao = "115/220 V", consumo = "≈ 3 W",
            dimensoes = "76×34 mm", conexoes = "Bornes + sensor NTC",
            equivalentes = "Eliwell, Coel, Ageon de mesma faixa",
            vantagens = "Degelo programável, fácil", limitacoes = "Conferir corrente do relé",
            observacoes = "Sensor NTC compatível."
        ),
        Componente(
            categoria = "Controlador", marca = "Eliwell", modelo = "IDPlus 902", aplicacao = "Temperatura c/ degelo",
            capacidade = "Relés compressor/degelo/vent.", fluido = "—", tensao = "230 V", consumo = "≈ 3 W",
            dimensoes = "32×74 mm", conexoes = "Bornes + 1/2 sensores",
            equivalentes = "Full Gauge MT, Coel de mesma classe",
            vantagens = "2 sensores, função degelo", limitacoes = "Parametrização mais extensa",
            observacoes = "Conferir tipo de sensor (NTC/PTC)."
        ),
        Componente(
            categoria = "Controlador", marca = "Coel", modelo = "RC-410", aplicacao = "Temperatura",
            capacidade = "Saída a relé", fluido = "—", tensao = "115/230 V", consumo = "≈ 3 W",
            dimensoes = "Painel", conexoes = "Bornes + sensor",
            equivalentes = "Full Gauge, Eliwell equivalentes",
            vantagens = "Simples e econômico", limitacoes = "Recursos básicos",
            observacoes = "Para controle simples sem degelo complexo."
        ),

        // ---- Válvulas de expansão ----
        Componente(
            categoria = "Válvula de expansão", marca = "Danfoss", modelo = "TUB (R404A)", aplicacao = "Baixa/Média temperatura",
            capacidade = "Orifício intercambiável", fluido = "R404A", tensao = "—", consumo = "—",
            dimensoes = "1/4\"×1/2\"", conexoes = "Solda + equalização externa",
            equivalentes = "Emerson/Alco TX6, Saginomiya por fluido/capacidade",
            vantagens = "Orifício intercambiável", limitacoes = "Selecionar orifício correto",
            observacoes = "Posicionar e isolar o bulbo."
        ),
        Componente(
            categoria = "Válvula de expansão", marca = "Emerson/Alco", modelo = "TX6 (R404A)", aplicacao = "Média/Baixa temperatura",
            capacidade = "Ampla faixa", fluido = "R404A/R507", tensao = "—", consumo = "—",
            dimensoes = "Conexões diversas", conexoes = "Solda/flare",
            equivalentes = "Danfoss TUB, Saginomiya equivalentes",
            vantagens = "Robusta, ampla seleção", limitacoes = "Selecionar carga térmica do bulbo",
            observacoes = "Equalização externa recomendada."
        ),
        Componente(
            categoria = "Válvula de expansão", marca = "Danfoss", modelo = "TGE (R134a)", aplicacao = "Média/Alta temperatura",
            capacidade = "Conforme orifício", fluido = "R134a", tensao = "—", consumo = "—",
            dimensoes = "1/4\"×1/2\"", conexoes = "Solda",
            equivalentes = "Emerson TX6 carga R134a, Saginomiya",
            vantagens = "Boa modulação", limitacoes = "Específica por fluido",
            observacoes = "Não intercambiar carga entre fluidos."
        ),

        // ---- Pressostatos ----
        Componente(
            categoria = "Pressostato", marca = "Danfoss", modelo = "KP15", aplicacao = "Alta e baixa (dual)",
            capacidade = "Ajuste manual", fluido = "HFC/HCFC", tensao = "Contato seco", consumo = "—",
            dimensoes = "1/4\" SAE", conexoes = "Capilar + terminais",
            equivalentes = "Saginomiya DNS, Eliwell de dupla pressão",
            vantagens = "Robusto, rearme manual/auto", limitacoes = "Conferir faixa e reset",
            observacoes = "Ajustar conforme fluido."
        ),
        Componente(
            categoria = "Pressostato", marca = "Saginomiya", modelo = "DNS-D606", aplicacao = "Alta e baixa (dual)",
            capacidade = "Ajuste manual", fluido = "HFC/HCFC", tensao = "Contato seco", consumo = "—",
            dimensoes = "1/4\" SAE", conexoes = "Capilar + terminais",
            equivalentes = "Danfoss KP15, Eliwell duais",
            vantagens = "Confiável, custo médio", limitacoes = "Conferir diferencial",
            observacoes = "Verificar reset de alta."
        ),
        Componente(
            categoria = "Pressostato", marca = "Danfoss", modelo = "KP1 (baixa)", aplicacao = "Baixa pressão",
            capacidade = "Ajuste manual", fluido = "HFC/HCFC", tensao = "Contato seco", consumo = "—",
            dimensoes = "1/4\" SAE", conexoes = "Capilar + terminais",
            equivalentes = "Saginomiya SNS, Eliwell de baixa",
            vantagens = "Simples para baixa pressão", limitacoes = "Apenas baixa",
            observacoes = "Para controle de capacidade/segurança de baixa."
        ),

        // ---- Filtros secadores ----
        Componente(
            categoria = "Filtro secador", marca = "Danfoss", modelo = "DML 053 (3/8\")", aplicacao = "Linha de líquido",
            capacidade = "Sólido molecular", fluido = "HFC/HCFC", tensao = "—", consumo = "—",
            dimensoes = "3/8\" solda", conexoes = "Solda",
            equivalentes = "Emerson EK, Suniso de mesma conexão",
            vantagens = "Boa retenção de umidade", limitacoes = "Trocar ao abrir o sistema",
            observacoes = "Respeitar sentido do fluxo."
        ),
        Componente(
            categoria = "Filtro secador", marca = "Emerson", modelo = "EK-083 (3/8\")", aplicacao = "Linha de líquido",
            capacidade = "Sólido molecular", fluido = "HFC/HCFC", tensao = "—", consumo = "—",
            dimensoes = "3/8\" solda", conexoes = "Solda",
            equivalentes = "Danfoss DML, Suniso equivalentes",
            vantagens = "Alta capacidade de secagem", limitacoes = "Conferir compatibilidade de fluido",
            observacoes = "Trocar a cada intervenção."
        ),
        Componente(
            categoria = "Filtro secador", marca = "Suniso", modelo = "SD-052 (1/4\")", aplicacao = "Linha de líquido",
            capacidade = "Sólido molecular", fluido = "HFC/HCFC", tensao = "—", consumo = "—",
            dimensoes = "1/4\" solda", conexoes = "Solda",
            equivalentes = "Danfoss DML, Emerson EK menores",
            vantagens = "Econômico", limitacoes = "Menor capacidade",
            observacoes = "Para sistemas pequenos."
        ),
    )

    fun pesquisar(categoria: String?, termo: String): List<Componente> =
        LISTA.filter {
            (categoria == null || it.categoria == categoria) &&
                (termo.isBlank() || it.modelo.contains(termo, true) ||
                    it.marca.contains(termo, true) || it.aplicacao.contains(termo, true))
        }
}
