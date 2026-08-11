package br.com.refrigeracaopro.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

/**
 * Reprodução do fluxo real de um ensaio **completamente preenchido**:
 * campos da tela → grades codificadas → registro → decodificação →
 * índices → parecer → exportações.
 *
 * Cobre o caminho que roda ao salvar o ensaio e ao abrir o parecer.
 */
class McaEnsaioCompletoTest {

    /** Ensaio com todos os campos de todos os blocos preenchidos. */
    private fun leiturasCompletas(): Mca.Leituras {
        val pares = Mca.Par.entries
        return Mca.Leituras(
            temperaturaCarcacaC = 38.5,
            resistencias = pares.mapIndexed { i, p ->
                p to Mca.LeituraResistencia(
                    listOf(1.234 + i * 0.01, 1.236 + i * 0.01, 1.235 + i * 0.01),
                    45.2 + i * 0.3,
                )
            }.toMap(),
            lz100Hz = pares.mapIndexed { i, p -> p to Mca.LeituraLZ(52.4 + i, 54.1 + i) }.toMap(),
            lz1kHz = pares.mapIndexed { i, p -> p to Mca.LeituraLZ(50.1 + i, 60.7 + i) }.toMap(),
            altaFrequencia = pares.mapIndexed { i, p ->
                p to Mca.LeituraAltaFrequencia(601.3 + i * 5, 44.8 + i * 0.2)
            }.toMap(),
            capacitanciasNf = Mca.Fase.entries.mapIndexed { i, f -> f to 20.5 + i * 0.4 }.toMap(),
            ric = pares.mapIndexed { i, p ->
                p to List(Mca.RIC_POSICOES) { pos ->
                    100.0 + (5.0 + i) * cos(pos * Mca.RIC_PASSO_GRAUS * Math.PI / 180.0)
                }
            }.toMap(),
            isolacaoMOhm = 850.0,
            tensaoEnsaioV = 1000.0,
            leitura1min = 700.0,
            leitura10min = 2100.0,
        )
    }

    /** Simula o que a tela grava: campos → grades codificadas. */
    private fun codificar(leituras: Mca.Leituras) = EnsaioMca(
        motorId = 1L,
        numero = "MCA-2026-0001",
        tecnico = "Técnico",
        temperaturaCarcacaC = leituras.temperaturaCarcacaC,
        umidadeRelativa = 62.0,
        rascunho = false,
        blocoAtual = 6,
        resistencias = Mca.codificarResistencias(leituras.resistencias),
        indutancias = Mca.codificarLZ(leituras.lz100Hz, leituras.lz1kHz),
        altaFrequencia = Mca.codificarAltaFrequencia(leituras.altaFrequencia),
        capacitancias = Mca.codificarCapacitancias(leituras.capacitanciasNf),
        ric = Mca.codificarRic(leituras.ric),
        isolacaoMOhm = leituras.isolacaoMOhm,
        tensaoEnsaioV = leituras.tensaoEnsaioV,
        leitura1min = leituras.leitura1min,
        leitura10min = leituras.leitura10min,
    )

    /** Simula o que o app lê de volta: registro → leituras tipadas. */
    private fun decodificar(ensaio: EnsaioMca): Mca.Leituras {
        val (cem, mil) = Mca.decodificarLZ(ensaio.indutancias)
        return Mca.Leituras(
            temperaturaCarcacaC = ensaio.temperaturaCarcacaC,
            resistencias = Mca.decodificarResistencias(ensaio.resistencias),
            lz100Hz = cem,
            lz1kHz = mil,
            altaFrequencia = Mca.decodificarAltaFrequencia(ensaio.altaFrequencia),
            capacitanciasNf = Mca.decodificarCapacitancias(ensaio.capacitancias),
            ric = Mca.decodificarRic(ensaio.ric),
            isolacaoMOhm = ensaio.isolacaoMOhm,
            tensaoEnsaioV = ensaio.tensaoEnsaioV,
            leitura1min = ensaio.leitura1min,
            leitura10min = ensaio.leitura10min,
        )
    }

    @Test
    fun `salvar um ensaio completo nao quebra a codificacao`() {
        val ensaio = codificar(leiturasCompletas())
        assertTrue(ensaio.resistencias.isNotEmpty())
        assertTrue(ensaio.ric.isNotEmpty())
    }

    @Test
    fun `abrir o parecer de um ensaio completo nao lanca excecao`() {
        val ensaio = codificar(leiturasCompletas())
        val volta = decodificar(ensaio)
        val parecer = Mca.avaliar(volta, Mca.LimitesMca())
        assertNotNull(parecer)
        assertTrue("deveria produzir algum achado", parecer.achados.isNotEmpty())
    }

    @Test
    fun `parecer com baseline de ensaio anterior`() {
        val volta = decodificar(codificar(leiturasCompletas()))
        val indices = Mca.calcularIndices(volta)
        val baseline = Mca.Baseline(indices.ifPorPar, indices.capacitanciaMediaNf)
        val parecer = Mca.avaliar(volta, Mca.LimitesMca(), baseline)
        assertNotNull(parecer)
    }

    @Test
    fun `exportacoes de um ensaio completo nao lancam excecao`() {
        val volta = decodificar(codificar(leiturasCompletas()))
        val parecer = Mca.avaliar(volta, Mca.LimitesMca())
        val cabecalho = listOf("Ensaio" to "MCA-2026-0001", "Técnico" to "Técnico")
        val csv = McaExport.paraCsv(cabecalho, volta, parecer)
        val json = McaExport.paraJson(cabecalho, volta, parecer)
        assertTrue(csv.isNotBlank())
        assertTrue(json.trim().startsWith("{"))
    }

    /**
     * Regressão do crash em campo: a unidade "%" era interpolada dentro da
     * string de formato ("%.2f %"), e o `%` final virava um especificador
     * incompleto — `UnknownFormatConversionException` ao abrir o parecer de
     * qualquer ensaio preenchido.
     */
    @Test
    fun `semaforo formata todas as unidades sem quebrar o format`() {
        val indices = Mca.calcularIndices(decodificar(codificar(leiturasCompletas())))
        val linhas = Mca.semaforo(indices, Mca.LimitesMca())

        assertTrue("deveria haver linhas com unidade %", linhas.any { it.unidade == "%" })
        linhas.forEach { linha ->
            // Não pode lançar exceção em nenhuma linha
            val valor = linha.valorTexto
            val limite = linha.limiteTexto
            assertTrue("valor vazio em ${linha.nome}", valor.isNotBlank())
            assertTrue("limite vazio em ${linha.nome}", limite.isNotBlank())
            if (linha.valor != null && linha.unidade.isNotBlank()) {
                assertTrue(
                    "a unidade deveria aparecer no texto de ${linha.nome}: $valor",
                    valor.endsWith(linha.unidade),
                )
            }
        }
    }

    @Test
    fun `semaforo marca nao avaliado quando falta dado`() {
        val linhas = Mca.semaforo(Mca.Indices(), Mca.LimitesMca())
        assertTrue(linhas.all { it.valorTexto == Mca.NAO_AVALIADO })
    }

    @Test
    fun `ensaio completo em locale com virgula decimal`() {
        // O app roda em pt-BR: "%.2f" produz "1,23". Se algum número
        // formatado voltar a ser lido como texto, quebra.
        val padrao = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("pt", "BR"))
            val volta = decodificar(codificar(leiturasCompletas()))
            val parecer = Mca.avaliar(volta, Mca.LimitesMca())
            assertNotNull(parecer)
            assertTrue(McaExport.paraJson(emptyList(), volta, parecer).isNotBlank())
        } finally {
            java.util.Locale.setDefault(padrao)
        }
    }
}
