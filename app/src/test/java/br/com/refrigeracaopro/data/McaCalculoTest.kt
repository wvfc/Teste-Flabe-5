package br.com.refrigeracaopro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

/**
 * Testes do motor de cálculo do MCA: cada fórmula, cada ramo da árvore de
 * diagnóstico e os casos de borda (divisão por zero, campo vazio, valores
 * negativos). Rodam na JVM, sem device.
 */
class McaCalculoTest {

    private val tol = 1e-6
    private val limites = Mca.LimitesMca()

    // ------------------------------------------------------------------
    // Fórmulas
    // ------------------------------------------------------------------

    @Test
    fun `correcao de R para 40 graus usa a constante do cobre`() {
        // R40 = 1,0 * (234,5 + 40) / (234,5 + 25)
        val esperado = 1.0 * (234.5 + 40.0) / (234.5 + 25.0)
        assertEquals(esperado, Mca.corrigirR40(1.0, 25.0)!!, tol)
    }

    @Test
    fun `correcao a 40 graus devolve a propria leitura`() {
        assertEquals(2.5, Mca.corrigirR40(2.5, 40.0)!!, tol)
    }

    @Test
    fun `correcao de R protege divisao por zero`() {
        assertNull(Mca.corrigirR40(1.0, -234.5))
    }

    @Test
    fun `desbalanceamento segue max menos min sobre a media`() {
        // (10,4 - 9,6) / 10 * 100 = 8%
        assertEquals(8.0, Mca.desbalanceamento(listOf(9.6, 10.0, 10.4))!!, 1e-9)
    }

    @Test
    fun `desbalanceamento de valores iguais e zero`() {
        assertEquals(0.0, Mca.desbalanceamento(listOf(5.0, 5.0, 5.0))!!, tol)
    }

    @Test
    fun `desbalanceamento exige ao menos dois valores`() {
        assertNull(Mca.desbalanceamento(listOf(5.0)))
        assertNull(Mca.desbalanceamento(emptyList()))
    }

    @Test
    fun `desbalanceamento protege media zero`() {
        assertNull(Mca.desbalanceamento(listOf(-5.0, 5.0)))
    }

    @Test
    fun `delta theta e a diferenca entre extremos`() {
        assertEquals(1.2, Mca.deltaTheta(listOf(45.1, 44.3, 45.5))!!, 1e-9)
        assertNull(Mca.deltaTheta(listOf(45.0)))
    }

    @Test
    fun `indice IF compara as duas frequencias`() {
        // (100 - 95) / 100 * 100 = 5%
        assertEquals(5.0, Mca.indiceIF(100.0, 95.0)!!, tol)
    }

    @Test
    fun `indice IF protege L de 100 Hz igual a zero`() {
        assertNull(Mca.indiceIF(0.0, 10.0))
    }

    @Test
    fun `indice de polarizacao e a razao das leituras`() {
        assertEquals(2.5, Mca.indicePolarizacao(200.0, 500.0)!!, tol)
    }

    @Test
    fun `indice de polarizacao protege leitura de um minuto invalida`() {
        assertNull(Mca.indicePolarizacao(0.0, 500.0))
        assertNull(Mca.indicePolarizacao(-10.0, 500.0))
    }

    @Test
    fun `amplitude pico a pico ignora posicoes nao medidas`() {
        assertEquals(4.0, Mca.amplitude(listOf(10.0, null, 14.0, null))!!, tol)
        assertNull(Mca.amplitude(listOf(null, 10.0)))
    }

    // ------------------------------------------------------------------
    // RIC — ajuste senoidal
    // ------------------------------------------------------------------

    @Test
    fun `serie senoidal perfeita tem desvio praticamente nulo`() {
        val serie = List(Mca.RIC_POSICOES) { i ->
            100.0 + 5.0 * cos(i * Mca.RIC_PASSO_GRAUS * Math.PI / 180.0)
        }
        val desvio = Mca.desvioSenoidal(serie)!!
        assertTrue("desvio deveria ser ~0, veio $desvio", desvio < 0.01)
    }

    @Test
    fun `serie irregular acusa desvio senoidal relevante`() {
        val serie = listOf(
            100.0, 118.0, 101.0, 96.0, 119.0, 99.0,
            100.0, 117.0, 102.0, 97.0, 120.0, 98.0,
        )
        val desvio = Mca.desvioSenoidal(serie)!!
        assertTrue("desvio deveria ser alto, veio $desvio", desvio > 10.0)
    }

    @Test
    fun `desvio senoidal exige pontos suficientes`() {
        assertNull(Mca.desvioSenoidal(listOf(1.0, 2.0, null)))
    }

    @Test
    fun `serie constante nao produz desvio`() {
        assertNull(Mca.desvioSenoidal(List(12) { 100.0 }))
    }

    // ------------------------------------------------------------------
    // Repetibilidade das 3 medições de R
    // ------------------------------------------------------------------

    @Test
    fun `desvio acima de um por cento entre repeticoes gera alerta`() {
        val leituras = Mca.Leituras(
            temperaturaCarcacaC = 25.0,
            resistencias = mapOf(
                Mca.Par.AB to Mca.LeituraResistencia(listOf(1.000, 1.030, 1.010), 45.0),
                Mca.Par.BC to Mca.LeituraResistencia(listOf(1.000, 1.001, 1.000), 45.0),
                Mca.Par.CA to Mca.LeituraResistencia(listOf(1.000, 1.000, 1.000), 45.0),
            ),
        )
        val indices = Mca.calcularIndices(leituras)
        val ab = indices.repeticoes.first { it.par == Mca.Par.AB }
        val bc = indices.repeticoes.first { it.par == Mca.Par.BC }
        assertTrue("AB deveria alertar", ab.alerta)
        assertTrue("BC não deveria alertar", !bc.alerta)
    }

    // ------------------------------------------------------------------
    // Árvore de diagnóstico
    // ------------------------------------------------------------------

    /** Ensaio equilibrado, usado como base dos cenários. */
    private fun leiturasEquilibradas(
        resistencias: List<Double> = listOf(1.0, 1.0, 1.0),
        l1k: List<Double> = listOf(50.0, 50.0, 50.0),
        z1k: List<Double> = listOf(60.0, 60.0, 60.0),
        z10k: List<Double> = listOf(600.0, 600.0, 600.0),
        thetas: List<Double> = listOf(45.0, 45.0, 45.0),
        capacitancias: List<Double> = listOf(20.0, 20.0, 20.0),
    ): Mca.Leituras {
        val pares = Mca.Par.entries
        return Mca.Leituras(
            temperaturaCarcacaC = 40.0,
            resistencias = pares.mapIndexed { i, p ->
                p to Mca.LeituraResistencia(List(3) { resistencias[i] }, thetas[i])
            }.toMap(),
            lz100Hz = pares.mapIndexed { i, p -> p to Mca.LeituraLZ(l1k[i] * 1.05, z1k[i] * 0.9) }.toMap(),
            lz1kHz = pares.mapIndexed { i, p -> p to Mca.LeituraLZ(l1k[i], z1k[i]) }.toMap(),
            altaFrequencia = pares.mapIndexed { i, p -> p to Mca.LeituraAltaFrequencia(z10k[i], thetas[i]) }.toMap(),
            capacitanciasNf = Mca.Fase.entries.mapIndexed { i, f -> f to capacitancias[i] }.toMap(),
            ric = pares.associateWith { par ->
                List(Mca.RIC_POSICOES) { i -> 100.0 + 5.0 * cos(i * 30 * Math.PI / 180.0) }
            },
            isolacaoMOhm = 500.0,
            leitura1min = 200.0,
            leitura10min = 600.0,
        )
    }

    @Test
    fun `ramo 8 - tudo dentro dos limites`() {
        val parecer = Mca.avaliar(leiturasEquilibradas(), limites)
        assertEquals(1, parecer.achados.size)
        assertEquals("Sem anomalia detectada", parecer.achados.first().titulo)
        assertEquals(Mca.Severidade.OK, parecer.achados.first().severidade)
    }

    @Test
    fun `ramo 1 - R40 desbalanceada com o resto no limite`() {
        val parecer = Mca.avaliar(leiturasEquilibradas(resistencias = listOf(1.0, 1.0, 1.1)), limites)
        val achado = parecer.achados.firstOrNull { it.titulo.contains("alta resistência") }
        assertNotNull("deveria acusar alta resistência", achado)
        assertTrue(achado!!.evidencia.contains("R40"))
    }

    @Test
    fun `ramo 2 - L e Z desbalanceados com R40 no limite`() {
        val parecer = Mca.avaliar(
            leiturasEquilibradas(l1k = listOf(50.0, 50.0, 56.0), z1k = listOf(60.0, 60.0, 67.0)),
            limites,
        )
        assertNotNull(
            "deveria acusar curto entre espiras",
            parecer.achados.firstOrNull { it.titulo.contains("Curto entre espiras (falha de estator)") },
        )
    }

    @Test
    fun `ramo 3 - desbalanceamento cresce da faixa de 1 kHz para a de 10 kHz`() {
        val parecer = Mca.avaliar(
            leiturasEquilibradas(z1k = listOf(60.0, 60.0, 61.0), z10k = listOf(600.0, 600.0, 680.0)),
            limites,
        )
        assertNotNull(
            "deveria acusar curto incipiente",
            parecer.achados.firstOrNull { it.titulo.contains("incipiente") },
        )
    }

    @Test
    fun `ramo 4 - sem ensaio anterior a contaminacao fica nao avaliada`() {
        val parecer = Mca.avaliar(leiturasEquilibradas(), limites, baseline = null)
        assertTrue(
            "deveria registrar como não avaliado",
            parecer.naoAvaliados.any { it.contains("Contaminação") },
        )
    }

    @Test
    fun `ramo 4 - queda simultanea de I-F nas tres fases com baseline`() {
        // I/F atual ~4,76% (L100 = L1k * 1,05); baseline bem menor nas três
        val baseline = Mca.Baseline(
            ifPorPar = Mca.Par.entries.associateWith { 1.0 },
        )
        val parecer = Mca.avaliar(leiturasEquilibradas(), limites, baseline)
        assertNotNull(
            "deveria acusar contaminação do enrolamento",
            parecer.achados.firstOrNull { it.titulo.contains("Contaminação") },
        )
    }

    @Test
    fun `ramo 5 - capacitancia para terra desbalanceada`() {
        val parecer = Mca.avaliar(
            leiturasEquilibradas(capacitancias = listOf(20.0, 20.0, 25.0)),
            limites,
        )
        assertNotNull(
            "deveria acusar umidade ou contaminação",
            parecer.achados.firstOrNull { it.titulo.contains("Umidade") },
        )
    }

    @Test
    fun `ramo 5 - capacitancia media acima do baseline historico`() {
        val baseline = Mca.Baseline(
            ifPorPar = Mca.Par.entries.associateWith { 100.0 }, // evita disparar o ramo 4
            capacitanciaMediaNf = 10.0,
        )
        val parecer = Mca.avaliar(leiturasEquilibradas(), limites, baseline)
        assertNotNull(
            "deveria acusar pela subida em relação ao histórico",
            parecer.achados.firstOrNull { it.titulo.contains("Umidade") },
        )
    }

    @Test
    fun `ramo 6 - isolacao e PI abaixo do limite`() {
        val base = leiturasEquilibradas()
        val parecer = Mca.avaliar(
            base.copy(isolacaoMOhm = 80.0, leitura1min = 100.0, leitura10min = 150.0),
            limites,
        )
        val achado = parecer.achados.firstOrNull { it.titulo.contains("Isolação para terra") }
        assertNotNull("deveria acusar isolação degradada", achado)
        assertEquals(Mca.Severidade.CRITICO, achado!!.severidade)
    }

    @Test
    fun `ramo 7 - RIC com espalhamento de amplitude acima do limite`() {
        val base = leiturasEquilibradas()
        val ric = mapOf(
            Mca.Par.AB to List(12) { i -> 100.0 + 5.0 * cos(i * 30 * Math.PI / 180.0) },
            Mca.Par.BC to List(12) { i -> 100.0 + 5.0 * cos(i * 30 * Math.PI / 180.0) },
            Mca.Par.CA to List(12) { i -> 100.0 + 9.0 * cos(i * 30 * Math.PI / 180.0) },
        )
        val parecer = Mca.avaliar(base.copy(ric = ric), limites)
        val achado = parecer.achados.firstOrNull { it.titulo.contains("Rotor") }
        assertNotNull("deveria acusar problema de rotor", achado)
        assertTrue(
            "o parecer deve trazer a ressalva do MCSA",
            achado!!.detalhe.contains("MCSA"),
        )
    }

    @Test
    fun `ensaio vazio nao emite diagnostico`() {
        val parecer = Mca.avaliar(Mca.Leituras(), limites)
        assertTrue("não deveria haver achados", parecer.achados.isEmpty())
        assertTrue("tudo deveria estar não avaliado", parecer.naoAvaliados.isNotEmpty())
    }

    @Test
    fun `sem temperatura da carcaca o R40 fica nao avaliado`() {
        val leituras = leiturasEquilibradas().copy(temperaturaCarcacaC = null)
        val parecer = Mca.avaliar(leituras, limites)
        assertTrue(parecer.indices.r40.isEmpty())
        assertNull(parecer.indices.desbalR40)
        assertTrue(parecer.naoAvaliados.any { it.contains("R40") })
    }

    @Test
    fun `bloco incompleto nao gera indice parcial`() {
        val leituras = leiturasEquilibradas()
        val soDoisPares = leituras.copy(
            capacitanciasNf = mapOf(Mca.Fase.A to 20.0, Mca.Fase.B to 21.0),
        )
        val indices = Mca.calcularIndices(soDoisPares)
        assertNull("com uma fase faltando não se calcula desbalanceamento", indices.desbalCapacitancia)
    }

    // ------------------------------------------------------------------
    // Codecs das grades
    // ------------------------------------------------------------------

    @Test
    fun `codec de resistencias preserva valores e ausencias`() {
        val original = mapOf(
            Mca.Par.AB to Mca.LeituraResistencia(listOf(1.1, 1.2, null), 45.5),
            Mca.Par.BC to Mca.LeituraResistencia(listOf(null, null, null), null),
            Mca.Par.CA to Mca.LeituraResistencia(listOf(2.0, 2.0, 2.0), 44.0),
        )
        val voltou = Mca.decodificarResistencias(Mca.codificarResistencias(original))
        assertEquals(listOf(1.1, 1.2, null), voltou[Mca.Par.AB]!!.repeticoes)
        assertEquals(45.5, voltou[Mca.Par.AB]!!.theta!!, tol)
        assertNull(voltou[Mca.Par.BC]!!.theta)
        assertEquals(2.0, voltou[Mca.Par.CA]!!.repeticoes[0]!!, tol)
    }

    @Test
    fun `codec de L e Z separa as duas frequencias`() {
        val cem = mapOf(Mca.Par.AB to Mca.LeituraLZ(52.0, 54.0))
        val mil = mapOf(Mca.Par.AB to Mca.LeituraLZ(50.0, 60.0))
        val (voltouCem, voltouMil) = Mca.decodificarLZ(Mca.codificarLZ(cem, mil))
        assertEquals(52.0, voltouCem[Mca.Par.AB]!!.l!!, tol)
        assertEquals(60.0, voltouMil[Mca.Par.AB]!!.z!!, tol)
    }

    @Test
    fun `codec do RIC preserva as doze posicoes`() {
        val original = mapOf(Mca.Par.AB to List(12) { it.toDouble() })
        val voltou = Mca.decodificarRic(Mca.codificarRic(original))
        assertEquals(12, voltou[Mca.Par.AB]!!.size)
        assertEquals(11.0, voltou[Mca.Par.AB]!![11]!!, tol)
    }

    @Test
    fun `codec tolera texto vazio ou corrompido`() {
        assertTrue(Mca.decodificarResistencias("").isEmpty())
        assertTrue(Mca.decodificarRic("lixo sem igual").isEmpty())
        assertTrue(Mca.decodificarCapacitancias("A=").isNotEmpty().not())
    }

    @Test
    fun `angulos do RIC vao de zero a 330 de trinta em trinta`() {
        val angulos = Mca.angulosRic()
        assertEquals(12, angulos.size)
        assertEquals(0, angulos.first())
        assertEquals(330, angulos.last())
    }
}
