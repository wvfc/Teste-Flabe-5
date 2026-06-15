package br.com.refrigeracaopro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Projeto isométrico de tubulações (ar comprimido / refrigeração). O cadastro
 * fica em colunas; o estado do editor (componentes + conexões) é guardado como
 * JSON para permitir autosave e desfazer/refazer de forma simples e offline.
 */
@Entity(tableName = "projetos")
data class Projeto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String = "Projeto",
    val clienteId: Long? = null,
    val equipamento: String = "",
    val local: String = "",
    val responsavel: String = "",
    val data: String = "",
    val pressaoTrabalho: String = "",
    val fluido: String = "",
    val vazao: String = "",
    val temperatura: String = "",
    val observacoes: String = "",
    val favorito: Boolean = false,
    val estado: String = "", // JSON: { componentes:[...], conexoes:[...] }
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis(),
)

/** Componente posicionado no desenho. */
data class CompIso(
    val id: Long,
    val categoria: String,
    val tipo: String,
    val x: Float,
    val y: Float,
    val rotacao: Float = 0f,
    val nome: String = "",
    val codigo: String = "",
    val fabricante: String = "",
    val modelo: String = "",
    val diametro: String = "",
    val material: String = "",
    val comprimento: String = "",
    val peso: String = "",
    val pressaoMax: String = "",
    val tempMax: String = "",
    val observacoes: String = "",
    val cor: Long = 0L, // 0 = cor padrão da categoria
    val etiqueta: String = "",
)

/** Conexão (trecho de tubulação) entre dois componentes. */
data class ConexaoIso(
    val id: Long,
    val deId: Long,
    val paraId: Long,
    val fluido: String = FluidosLinha.AR_COMPRIMIDO,
    val comprimento: String = "",
    val material: String = "",
    val diametroInterno: String = "",
    val diametroExterno: String = "",
    val espessura: String = "",
    val classe: String = "",
    val pressao: String = "",
)

data class EstadoProjeto(
    val componentes: List<CompIso> = emptyList(),
    val conexoes: List<ConexaoIso> = emptyList(),
)

/** Tipos de linha/fluido e suas cores padrão (ARGB). */
object FluidosLinha {
    const val AR_COMPRIMIDO = "Ar comprimido"
    const val LIQUIDO = "Linha de líquido"
    const val SUCCAO = "Sucção"
    const val DESCARGA = "Descarga"
    const val DRENO = "Dreno"
    const val GERAL = "Tubulação"

    val TODOS = listOf(GERAL, AR_COMPRIMIDO, LIQUIDO, SUCCAO, DESCARGA, DRENO)

    fun cor(fluido: String): Long = when (fluido) {
        AR_COMPRIMIDO -> 0xFF4FC3F7  // azul claro
        LIQUIDO -> 0xFFE53935        // vermelho
        SUCCAO -> 0xFF1A237E         // azul escuro
        DESCARGA -> 0xFFF57C00       // laranja
        DRENO -> 0xFF2E7D32          // verde
        else -> 0xFF1565C0           // azul (tubulação geral)
    }
}

/** Catálogo de componentes por categoria (conforme a biblioteca pedida). */
object CatalogoComponentes {
    val CATEGORIAS: List<Pair<String, List<String>>> = listOf(
        "Tubulação" to listOf("Tubo reto", "Tubo vertical", "Tubo flexível"),
        "Conexões" to listOf("Curva 90°", "Curva 45°", "Curva longa", "Curva curta", "União", "Luva", "Niple", "Cruzeta", "Tee", "Tee Redução", "Redução Concêntrica", "Redução Excêntrica"),
        "Válvulas" to listOf("Registro esfera", "Registro gaveta", "Válvula retenção", "Válvula segurança", "Válvula solenóide", "Válvula expansão", "Válvula agulha", "Schraders"),
        "Filtros" to listOf("Filtro secador", "Filtro de sucção", "Separador de óleo", "Separador de líquido"),
        "Equipamentos" to listOf("Compressor", "Condensadora", "Evaporadora", "Reservatório", "Secador", "Booster", "Chiller", "Torre", "Bomba", "Painel elétrico"),
        "Instrumentação" to listOf("Pressostato", "Manômetro", "Sensor temperatura", "Fluxostato", "Medidor vazão"),
        "Consumidores" to listOf("Máquina", "Laser", "Cilindro pneumático", "Pistola", "Máquina CNC", "Outro"),
    )

    /** Abreviação para o símbolo no desenho. */
    fun sigla(tipo: String): String {
        val limpo = tipo.replace("°", "").trim()
        val palavras = limpo.split(" ", "/").filter { it.isNotBlank() }
        return when {
            palavras.size >= 2 -> (palavras[0].take(1) + palavras[1].take(1)).uppercase()
            else -> limpo.take(2).uppercase()
        }
    }
}

/** Serialização do estado do editor (JSON), para autosave/undo. */
object ProjetoJson {

    fun serializar(estado: EstadoProjeto): String {
        val comps = JSONArray()
        estado.componentes.forEach { c ->
            comps.put(JSONObject().apply {
                put("id", c.id); put("categoria", c.categoria); put("tipo", c.tipo)
                put("x", c.x.toDouble()); put("y", c.y.toDouble()); put("rotacao", c.rotacao.toDouble())
                put("nome", c.nome); put("codigo", c.codigo); put("fabricante", c.fabricante)
                put("modelo", c.modelo); put("diametro", c.diametro); put("material", c.material)
                put("comprimento", c.comprimento); put("peso", c.peso); put("pressaoMax", c.pressaoMax)
                put("tempMax", c.tempMax); put("observacoes", c.observacoes); put("cor", c.cor)
                put("etiqueta", c.etiqueta)
            })
        }
        val cons = JSONArray()
        estado.conexoes.forEach { x ->
            cons.put(JSONObject().apply {
                put("id", x.id); put("deId", x.deId); put("paraId", x.paraId); put("fluido", x.fluido)
                put("comprimento", x.comprimento); put("material", x.material)
                put("diametroInterno", x.diametroInterno); put("diametroExterno", x.diametroExterno)
                put("espessura", x.espessura); put("classe", x.classe); put("pressao", x.pressao)
            })
        }
        return JSONObject().put("componentes", comps).put("conexoes", cons).toString()
    }

    fun desserializar(json: String): EstadoProjeto {
        if (json.isBlank()) return EstadoProjeto()
        return runCatching {
            val obj = JSONObject(json)
            val comps = obj.optJSONArray("componentes") ?: JSONArray()
            val cons = obj.optJSONArray("conexoes") ?: JSONArray()
            val lcomp = (0 until comps.length()).map { i ->
                val o = comps.getJSONObject(i)
                CompIso(
                    id = o.optLong("id"), categoria = o.optString("categoria"), tipo = o.optString("tipo"),
                    x = o.optDouble("x").toFloat(), y = o.optDouble("y").toFloat(), rotacao = o.optDouble("rotacao").toFloat(),
                    nome = o.optString("nome"), codigo = o.optString("codigo"), fabricante = o.optString("fabricante"),
                    modelo = o.optString("modelo"), diametro = o.optString("diametro"), material = o.optString("material"),
                    comprimento = o.optString("comprimento"), peso = o.optString("peso"), pressaoMax = o.optString("pressaoMax"),
                    tempMax = o.optString("tempMax"), observacoes = o.optString("observacoes"), cor = o.optLong("cor"),
                    etiqueta = o.optString("etiqueta"),
                )
            }
            val lcon = (0 until cons.length()).map { i ->
                val o = cons.getJSONObject(i)
                ConexaoIso(
                    id = o.optLong("id"), deId = o.optLong("deId"), paraId = o.optLong("paraId"),
                    fluido = o.optString("fluido", FluidosLinha.AR_COMPRIMIDO), comprimento = o.optString("comprimento"),
                    material = o.optString("material"), diametroInterno = o.optString("diametroInterno"),
                    diametroExterno = o.optString("diametroExterno"), espessura = o.optString("espessura"),
                    classe = o.optString("classe"), pressao = o.optString("pressao"),
                )
            }
            EstadoProjeto(lcomp, lcon)
        }.getOrDefault(EstadoProjeto())
    }
}
