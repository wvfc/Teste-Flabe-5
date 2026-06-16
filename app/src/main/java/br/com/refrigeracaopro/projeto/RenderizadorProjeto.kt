package br.com.refrigeracaopro.projeto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import br.com.refrigeracaopro.data.CompIso
import br.com.refrigeracaopro.data.EstadoProjeto
import br.com.refrigeracaopro.data.FluidosLinha
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Desenha o projeto em um Bitmap. Dois modos:
 *  - 2D: planta com símbolos técnicos (estilo CAD) e grade isométrica.
 *  - 3D: projeção ortográfica com órbita livre de câmera (yaw/pitch), blocos
 *    extrudados e tubos contínuos — visão tridimensional, tudo offline no Canvas.
 */
object RenderizadorProjeto {

    const val COMP_W = 74f
    const val COMP_H = 46f
    private const val LIFT = 0.6f // fator de elevação na vista 2D iso
    // Categorias renderizadas como volumes (caixas) na vista 3D; o resto vira nó compacto.
    private val COM_CAIXA = setOf("Refrigeração", "Ar comprimido")

    data class Vista(val escala: Float, val offX: Float, val offY: Float)

    private val fundo = Paint().apply { color = Color.WHITE }
    private val gradePaint = Paint().apply { color = Color.rgb(225, 232, 240); strokeWidth = 1f; isAntiAlias = true }

    // =================== VISTA 2D (planta) ===================

    fun render(
        estado: EstadoProjeto, w: Int, h: Int, escala: Float, offX: Float, offY: Float,
        mostrarGrade: Boolean, selecionadoId: Long?,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fundo)
        if (mostrarGrade) desenharGrade(c, w, h, escala)

        fun cx2(comp: CompIso) = comp.x * escala + offX
        fun cy2(comp: CompIso) = comp.y * escala + offY - comp.z * escala * LIFT

        // Postes de elevação (do componente elevado até o piso)
        val poste = Paint().apply { color = Color.rgb(180, 190, 205); strokeWidth = 1.2f; isAntiAlias = true }
        estado.componentes.filter { it.z != 0f }.forEach { comp ->
            c.drawLine(cx2(comp), cy2(comp), cx2(comp), comp.y * escala + offY, poste)
        }

        val linha = Paint().apply { strokeWidth = (3f * escala).coerceIn(2f, 8f); isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val mapa = estado.componentes.associateBy { it.id }
        estado.conexoes.forEach { x ->
            val a = mapa[x.deId]; val b = mapa[x.paraId]
            if (a != null && b != null) {
                linha.color = FluidosLinha.cor(x.fluido).toInt()
                c.drawLine(cx2(a), cy2(a), cx2(b), cy2(b), linha)
            }
        }
        estado.componentes.forEach { comp -> desenharComponente2D(c, comp, cx2(comp), cy2(comp), escala, comp.id == selecionadoId) }
        return bmp
    }

    private fun desenharGrade(c: Canvas, w: Int, h: Int, escala: Float) {
        val passo = (28f * escala).coerceIn(14f, 80f)
        val incl = tan(Math.toRadians(30.0)).toFloat()
        var x = -h * incl
        while (x < w + h * incl) {
            c.drawLine(x, 0f, x + h * incl, h.toFloat(), gradePaint)
            c.drawLine(x, h.toFloat(), x + h * incl, 0f, gradePaint)
            x += passo
        }
    }

    private fun desenharComponente2D(c: Canvas, comp: CompIso, cx: Float, cy: Float, escala: Float, selecionado: Boolean) {
        val wpx = COMP_W * escala
        val hpx = COMP_H * escala
        val cor = if (comp.cor != 0L) comp.cor.toInt() else corCategoria(comp.categoria)
        val temCaixa = comp.categoria in COM_CAIXA

        c.save(); c.rotate(comp.rotacao, cx, cy)
        val rect = RectF(cx - wpx / 2, cy - hpx / 2, cx + wpx / 2, cy + hpx / 2)
        val raio = 6f * escala
        if (temCaixa) {
            c.drawRoundRect(rect, raio, raio, Paint().apply { color = cor; alpha = 26; isAntiAlias = true })
            c.drawRoundRect(rect, raio, raio, Paint().apply {
                color = if (selecionado) Color.rgb(46, 166, 107) else Color.rgb(70, 84, 104)
                style = Paint.Style.STROKE; strokeWidth = (if (selecionado) 3.5f else 1.4f) * escala; isAntiAlias = true
            })
        } else if (selecionado) {
            c.drawRoundRect(rect, raio, raio, Paint().apply { color = Color.rgb(46, 166, 107); style = Paint.Style.STROKE; strokeWidth = 2.5f * escala; isAntiAlias = true })
        }
        SimbolosComponente.desenhar(c, comp, cx, cy, wpx * 0.36f, hpx * 0.36f, escala, cor)
        c.restore()

        val rotulo = comp.etiqueta.ifBlank { comp.nome.ifBlank { comp.tipo } }
        val sufixo = if (comp.z != 0f) "  (z ${comp.z.toInt()})" else ""
        c.drawText(rotulo.take(16) + sufixo, cx, cy + hpx / 2 + 12f * escala,
            Paint().apply { color = Color.rgb(40, 50, 70); isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 9.5f * escala })
    }

    // =================== VISTA 3D (órbita livre) ===================

    /** Câmera de órbita: yaw (giro horizontal) e pitch (elevação) em graus. */
    data class Camera3D(val yaw: Float, val pitch: Float)

    /** Presets de câmera (yaw, pitch). */
    val PRESETS: List<Pair<String, Camera3D>> = listOf(
        "Iso" to Camera3D(45f, 30f),
        "Topo" to Camera3D(0f, 89f),
        "Frente" to Camera3D(0f, 4f),
        "Direita" to Camera3D(90f, 4f),
        "Trás" to Camera3D(180f, 4f),
        "Esquerda" to Camera3D(270f, 4f),
        "Iso traseira" to Camera3D(225f, 30f),
    )

    fun render3d(
        estado: EstadoProjeto, w: Int, h: Int, escala: Float, panX: Float, panY: Float,
        mostrarGrade: Boolean, yaw: Float, pitch: Float,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fundo)

        // Projeção ortográfica de órbita. Mundo: x (leste), y (norte), z (cima).
        val ar = Math.toRadians(yaw.toDouble()); val er = Math.toRadians(pitch.toDouble())
        val sa = sin(ar).toFloat(); val ca = cos(ar).toFloat(); val se = sin(er).toFloat(); val ce = cos(er).toFloat()
        fun sx(x: Float, y: Float) = -x * sa + y * ca
        fun sy(x: Float, y: Float, z: Float) = -(x * ca * se + y * sa * se) + z * ce // para cima = positivo
        fun depth(x: Float, y: Float, z: Float) = x * ca * ce + y * sa * ce + z * se // maior = mais perto

        // Auto-centralização do conteúdo (inclui elevação no eixo vertical)
        val pts = estado.componentes.map { sx(it.x, it.y) to sy(it.x, it.y, it.z) }
        val minX = pts.minOfOrNull { it.first } ?: 0f; val maxX = pts.maxOfOrNull { it.first } ?: 0f
        val minY = pts.minOfOrNull { it.second } ?: 0f; val maxY = pts.maxOfOrNull { it.second } ?: 0f
        val cOffX = w / 2f - (minX + maxX) / 2f * escala + panX
        val cOffY = h / 2f + (minY + maxY) / 2f * escala + panY
        fun tela(x: Float, y: Float, z: Float): Pair<Float, Float> =
            sx(x, y) * escala + cOffX to -sy(x, y, z) * escala + cOffY

        if (mostrarGrade) desenharGrade3D(c, estado, escala, ::tela)

        fun zConex(comp: CompIso) = comp.z + if (comp.categoria in COM_CAIXA) altura3d(comp.categoria, comp.tipo) * 0.5f else 6f

        // Postes de elevação (do nível do componente até o piso)
        val poste = Paint().apply { color = Color.rgb(170, 182, 200); strokeWidth = 1.4f; isAntiAlias = true }
        estado.componentes.filter { it.z != 0f }.forEach { comp ->
            val (x1, y1) = tela(comp.x, comp.y, comp.z); val (x2, y2) = tela(comp.x, comp.y, 0f)
            c.drawLine(x1, y1, x2, y2, poste)
        }

        // Tubos (conexões) na elevação de cada extremidade
        val mapa = estado.componentes.associateBy { it.id }
        val tubo = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        estado.conexoes.forEach { x ->
            val a = mapa[x.deId]; val b = mapa[x.paraId]
            if (a != null && b != null) {
                val (ax, ay) = tela(a.x, a.y, zConex(a)); val (bx, by) = tela(b.x, b.y, zConex(b))
                tubo.color = escurecer(FluidosLinha.cor(x.fluido).toInt(), 0.7f); tubo.strokeWidth = 8f * escala
                c.drawLine(ax, ay, bx, by, tubo)
                tubo.color = FluidosLinha.cor(x.fluido).toInt(); tubo.strokeWidth = 5f * escala
                c.drawLine(ax, ay, bx, by, tubo)
            }
        }

        // Componentes: equipamentos como caixas; tubos/conexões/válvulas como nós compactos.
        // Ordenação do pintor: mais distante primeiro (depth crescente).
        estado.componentes.sortedBy { depth(it.x, it.y, it.z) }.forEach { comp ->
            if (comp.categoria in COM_CAIXA) {
                val h = altura3d(comp.categoria, comp.tipo)
                desenharCaixa3D(c, comp, 24f, 16f, comp.z, h, escala, ::tela)
            } else {
                desenharCaixa3D(c, comp, 15f, 11f, comp.z, 10f, escala, ::tela)
            }
        }
        return bmp
    }

    private fun desenharGrade3D(c: Canvas, estado: EstadoProjeto, escala: Float, tela: (Float, Float, Float) -> Pair<Float, Float>) {
        if (estado.componentes.isEmpty()) return
        val minX = estado.componentes.minOf { it.x } - 100f; val maxX = estado.componentes.maxOf { it.x } + 100f
        val minY = estado.componentes.minOf { it.y } - 100f; val maxY = estado.componentes.maxOf { it.y } + 100f
        val passo = 40f
        var x = minX
        while (x <= maxX) { val (x1, y1) = tela(x, minY, 0f); val (x2, y2) = tela(x, maxY, 0f); c.drawLine(x1, y1, x2, y2, gradePaint); x += passo }
        var y = minY
        while (y <= maxY) { val (x1, y1) = tela(minX, y, 0f); val (x2, y2) = tela(maxX, y, 0f); c.drawLine(x1, y1, x2, y2, gradePaint); y += passo }
    }

    private fun desenharCaixa3D(c: Canvas, comp: CompIso, fx: Float, fy: Float, baseZ: Float, hz: Float, escala: Float, tela: (Float, Float, Float) -> Pair<Float, Float>) {
        val cor = if (comp.cor != 0L) comp.cor.toInt() else corCategoria(comp.categoria)
        val topoZ = baseZ + hz

        // Cantos do topo e da base na ordem A,B,C,D
        val topo = listOf(
            tela(comp.x - fx, comp.y - fy, topoZ), tela(comp.x + fx, comp.y - fy, topoZ),
            tela(comp.x + fx, comp.y + fy, topoZ), tela(comp.x - fx, comp.y + fy, topoZ),
        )
        val base = listOf(
            tela(comp.x - fx, comp.y - fy, baseZ), tela(comp.x + fx, comp.y - fy, baseZ),
            tela(comp.x + fx, comp.y + fy, baseZ), tela(comp.x - fx, comp.y + fy, baseZ),
        )
        // Canto frontal = topo com maior Y de tela; desenha as 2 faces adjacentes
        val frente = (0..3).maxByOrNull { topo[it].second } ?: 2
        fun face(i: Int, j: Int, sombra: Float) {
            val p = Path(); p.moveTo(topo[i].first, topo[i].second); p.lineTo(topo[j].first, topo[j].second)
            p.lineTo(base[j].first, base[j].second); p.lineTo(base[i].first, base[i].second); p.close()
            c.drawPath(p, Paint().apply { color = escurecer(cor, sombra); isAntiAlias = true; style = Paint.Style.FILL })
            c.drawPath(p, contorno())
        }
        face((frente + 3) % 4, frente, 0.62f)
        face(frente, (frente + 1) % 4, 0.78f)

        // Topo
        val pTopo = Path(); pTopo.moveTo(topo[0].first, topo[0].second)
        for (i in 1..3) pTopo.lineTo(topo[i].first, topo[i].second)
        pTopo.close()
        c.drawPath(pTopo, Paint().apply { color = clarear(cor, 0.35f); isAntiAlias = true; style = Paint.Style.FILL })
        c.drawPath(pTopo, contorno())

        // Símbolo na face superior (centro do topo)
        val cTopX = topo.map { it.first }.average().toFloat()
        val cTopY = topo.map { it.second }.average().toFloat()
        SimbolosComponente.desenhar(c, comp, cTopX, cTopY, fx * escala * 0.7f, fy * escala * 0.7f, escala, Color.rgb(30, 40, 60))

        // Rótulo acima do bloco
        val rotulo = comp.etiqueta.ifBlank { comp.nome.ifBlank { comp.tipo } }
        c.drawText(rotulo.take(16), cTopX, topo.minOf { it.second } - 4f * escala,
            Paint().apply { color = Color.rgb(40, 50, 70); isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 9.5f * escala })
    }

    private fun contorno() = Paint().apply { color = Color.rgb(35, 45, 65); style = Paint.Style.STROKE; strokeWidth = 1.4f; isAntiAlias = true }

    private fun altura3d(categoria: String, tipo: String): Float = when {
        tipo == "Reservatório" || tipo == "Torre" || tipo == "Chiller" -> 80f
        tipo.startsWith("Compressor") || tipo == "Secador" || tipo == "Booster" -> 66f
        tipo == "Condensadora" || tipo == "Evaporadora" -> 60f
        categoria == "Refrigeração" -> 56f
        categoria == "Ar comprimido" -> 52f
        categoria == "Válvulas" -> 34f
        categoria == "Instrumentação" -> 30f
        else -> 18f
    }

    private fun corCategoria(categoria: String): Int = when (categoria) {
        "Refrigeração" -> Color.rgb(21, 101, 192)
        "Ar comprimido" -> Color.rgb(0, 121, 107)
        "Válvulas" -> Color.rgb(245, 124, 0)
        "Instrumentação" -> Color.rgb(0, 137, 123)
        "Conexões" -> Color.rgb(57, 73, 171)
        "Tubulações" -> Color.rgb(176, 98, 41)
        else -> Color.rgb(13, 44, 79)
    }

    private fun escurecer(cor: Int, f: Float): Int =
        Color.rgb((Color.red(cor) * f).toInt(), (Color.green(cor) * f).toInt(), (Color.blue(cor) * f).toInt())

    private fun clarear(cor: Int, f: Float): Int = Color.rgb(
        (Color.red(cor) + (255 - Color.red(cor)) * f).toInt(),
        (Color.green(cor) + (255 - Color.green(cor)) * f).toInt(),
        (Color.blue(cor) + (255 - Color.blue(cor)) * f).toInt(),
    )

    /** Escala para enquadrar o conteúdo na vista 3D (export). */
    fun enquadrar3d(estado: EstadoProjeto, w: Int, h: Int, yaw: Float, pitch: Float): Float {
        if (estado.componentes.isEmpty()) return 1f
        val ar = Math.toRadians(yaw.toDouble()); val er = Math.toRadians(pitch.toDouble())
        val sa = sin(ar).toFloat(); val ca = cos(ar).toFloat(); val se = sin(er).toFloat(); val ce = cos(er).toFloat()
        val pts = estado.componentes.map {
            (-it.x * sa + it.y * ca) to (-(it.x * ca * se + it.y * sa * se) + it.z * ce)
        }
        val larg = (pts.maxOf { it.first } - pts.minOf { it.first }).coerceAtLeast(1f)
        val alt = (pts.maxOf { it.second } - pts.minOf { it.second } + 90f).coerceAtLeast(1f)
        return minOf(w / larg, h / alt) * 0.7f
    }

    /** Vista para enquadrar todos os componentes (export 2D). */
    fun enquadrar(estado: EstadoProjeto, w: Int, h: Int): Vista {
        if (estado.componentes.isEmpty()) return Vista(1f, w / 2f, h / 2f)
        val minX = estado.componentes.minOf { it.x } - COMP_W
        val maxX = estado.componentes.maxOf { it.x } + COMP_W
        val minY = estado.componentes.minOf { it.y } - COMP_H
        val maxY = estado.componentes.maxOf { it.y } + COMP_H
        val larg = (maxX - minX).coerceAtLeast(1f); val alt = (maxY - minY).coerceAtLeast(1f)
        val escala = minOf(w / larg, h / alt) * 0.9f
        val offX = w / 2f - (minX + maxX) / 2f * escala
        val offY = h / 2f - (minY + maxY) / 2f * escala
        return Vista(escala, offX, offY)
    }
}
