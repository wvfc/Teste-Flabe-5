package br.com.refrigeracaopro.data

/**
 * Conversor de unidades genérico. Cada categoria tem uma unidade-base; as
 * demais convertem para/da base. A maioria é linear (fator); temperatura usa
 * funções próprias (offset).
 */
object Conversor {

    class Unidade(
        val nome: String,
        val paraBase: (Double) -> Double,
        val daBase: (Double) -> Double,
    )

    class Categoria(val nome: String, val unidades: List<Unidade>)

    /** Cria uma unidade linear pelo fator em relação à base. */
    private fun lin(nome: String, fator: Double) =
        Unidade(nome, { it * fator }, { it / fator })

    fun converter(categoria: Categoria, de: Unidade, para: Unidade, valor: Double): Double =
        para.daBase(de.paraBase(valor))

    val CATEGORIAS: List<Categoria> = listOf(
        Categoria("Temperatura", listOf(
            Unidade("°C", { it }, { it }),
            Unidade("°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            Unidade("K", { it - 273.15 }, { it + 273.15 }),
        )),
        Categoria("Pressão", listOf( // base: Pa
            lin("psi", 6894.757),
            lin("bar", 100000.0),
            lin("kPa", 1000.0),
            lin("MPa", 1_000_000.0),
            lin("Pa", 1.0),
            lin("atm", 101325.0),
            lin("mmHg", 133.322),
            lin("kgf/cm²", 98066.5),
        )),
        Categoria("Comprimento", listOf( // base: m
            lin("m", 1.0), lin("cm", 0.01), lin("mm", 0.001), lin("km", 1000.0),
            lin("pol", 0.0254), lin("pé", 0.3048), lin("jarda", 0.9144), lin("milha", 1609.344),
        )),
        Categoria("Massa", listOf( // base: kg
            lin("kg", 1.0), lin("g", 0.001), lin("mg", 1e-6), lin("ton", 1000.0),
            lin("lb", 0.4535924), lin("oz", 0.02834952),
        )),
        Categoria("Volume", listOf( // base: L
            lin("L", 1.0), lin("mL", 0.001), lin("m³", 1000.0),
            lin("gal (US)", 3.785412), lin("gal (UK)", 4.546090),
            lin("pol³", 0.01638706), lin("pé³", 28.31685),
        )),
        Categoria("Área", listOf( // base: m²
            lin("m²", 1.0), lin("cm²", 0.0001), lin("km²", 1e6), lin("ha", 10000.0),
            lin("pé²", 0.09290304), lin("acre", 4046.856),
        )),
        Categoria("Velocidade", listOf( // base: m/s
            lin("m/s", 1.0), lin("km/h", 0.2777778), lin("mph", 0.44704), lin("nó", 0.5144444),
        )),
        Categoria("Potência", listOf( // base: W
            lin("W", 1.0), lin("kW", 1000.0), lin("CV", 735.499), lin("HP", 745.6999),
            lin("BTU/h", 0.2930711), lin("TR (ton. refrig.)", 3516.853), lin("kcal/h", 1.162222),
        )),
        Categoria("Energia", listOf( // base: J
            lin("J", 1.0), lin("kJ", 1000.0), lin("cal", 4.184), lin("kcal", 4184.0),
            lin("Wh", 3600.0), lin("kWh", 3_600_000.0), lin("BTU", 1055.056),
        )),
        Categoria("Vazão", listOf( // base: L/s
            lin("L/s", 1.0), lin("L/min", 1.0 / 60), lin("m³/h", 0.2777778),
            lin("gpm (US)", 0.06309020), lin("cfm (pé³/min)", 0.4719474),
        )),
        Categoria("Tempo", listOf( // base: s
            lin("s", 1.0), lin("min", 60.0), lin("h", 3600.0), lin("dia", 86400.0),
        )),
        Categoria("Torque", listOf( // base: N·m
            lin("N·m", 1.0), lin("kgf·m", 9.80665), lin("lbf·ft", 1.355818),
        )),
        Categoria("Ângulo", listOf( // base: grau
            lin("grau", 1.0), lin("rad", 57.29578), lin("volta", 360.0),
        )),
    )
}
