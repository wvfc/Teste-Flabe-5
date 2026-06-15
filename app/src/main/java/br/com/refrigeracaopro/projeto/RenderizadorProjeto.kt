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
import kotlin.math.tan

/**
 * Desenha o projeto em um Bitmap. Dois modos:
 *  - 2D: planta com símbolos técnicos (estilo CAD) e grade isométrica.
 *  - 3D: projeção isométrica com blocos extrudados e tubos, com rotação de
 *    câmera (azimute) — visão tridimensional, tudo offline no Canvas.
 */
object RenderizadorProjeto {

    const val COMP_W = 74f
    const val COMP_H = 46f

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

        val linha = Paint().apply { strokeWidth = (3f * escala).coerceIn(2f, 8f); isAntiAlias = true; style = Paint.Style.STROKE }
        val mapa = estado.componentes.associateBy { it.id }
        estado.conexoes.forEach { x ->
            val a = mapa[x.deId]; val b = mapa[x.paraId]
            if (a != null && b != null) {
                linha.color = FluidosLinha.cor(x.fluido).toInt()
                c.drawLine(a.x * escala + offX, a.y * escala + offY, b.x * escala + offX, b.y * escala + offY, linha)
            }
        }
        estado.componentes.forEach { comp -> desenharComponente2D(c, comp, escala, offX, offY, comp.id == selecionadoId) }
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

    private fun desenharComponente2D(c: Canvas, comp: CompIso, escala: Float, offX: Float, offY: Float, selecionado: Boolean) {
        val cx = comp.x * escala + offX
        val cy = comp.y * escala + offY
        val wpx = COMP_W * escala
        val hpx = COMP_H * escala
        val cor = if (comp.cor != 0L) comp.cor.toInt() else corCategoria(comp.categoria)

        c.save(); c.rotate(comp.rotacao, cx, cy)
        val rect = RectF(cx - wpx / 2, cy - hpx / 2, cx + wpx / 2, cy + hpx / 2)
        val raio = 6f * escala
        c.drawRoundRect(rect, raio, raio, Paint().apply { color = cor; alpha = 28; isAntiAlias = true })
        c.drawRoundRect(rect, raio, raio, Paint().apply {
            color = if (selecionado) Color.rgb(46, 166, 107) else Color.rgb(70, 84, 104)
            style = Paint.Style.STROKE; strokeWidth = (if (selecionado) 3.5f else 1.4f) * escala; isAntiAlias = true
        })
        SimbolosComponente.desenhar(c, comp, cx, cy, wpx * 0.34f, hpx * 0.34f, escala, cor)
        c.restore()

        val rotulo = comp.etiqueta.ifBlank { comp.nome.ifBlank { comp.tipo } }
        c.drawText(rotulo.take(18), cx, cy + hpx / 2 + 12f * escala,
            Paint().apply { color = Color.rgb(40, 50, 70); isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 9.5f * escala })
    }

    // =================== VISTA 3D (isométrica) ===================

    fun render3d(
        estado: EstadoProjeto, w: Int, h: Int, escala: Float, panX: Float, panY: Float,
        mostrarGrade: Boolean, azimute: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fundo)

        fun rot(x: Float, y: Float): Pair<Float, Float> = when (((azimute % 360) + 360) % 360) {
            90 -> y to -x; 180 -> -x to -y; 270 -> -y to x; else -> x to y
        }
        // projeção isométrica: (X,Y,Z) -> tela (sem escala/offset)
        fun projXY(x: Float, y: Float): Pair<Float, Float> { val (rx, ry) = rot(x, y); return (rx - ry) * 0.866f to (rx + ry) * 0.5f }

        // Auto-centralização do conteúdo
        val pts = estado.componentes.map { projXY(it.x, it.y) }
        val minX = (pts.minOfOrNull { it.first } ?: 0f); val maxX = (pts.maxOfOrNull { it.first } ?: 0f)
        val minY = (pts.minOfOrNull { it.second } ?: 0f); val maxY = (pts.maxOfOrNull { it.second } ?: 0f)
        val cOffX = w / 2f - (minX + maxX) / 2f * escala + panX
        val cOffY = h / 2f - (minY + maxY) / 2f * escala + panY
        fun tela(x: Float, y: Float, z: Float): Pair<Float, Float> {
            val (px, py) = projXY(x, y); return px * escala + cOffX to (py - z) * escala + cOffY
        }

        if (mostrarGrade) desenharGrade3D(c, estado, escala, ::tela)

        // Tubos (conexões) na altura média
        val mapa = estado.componentes.associateBy { it.id }
        val tubo = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        estado.conexoes.forEach { x ->
            val a = mapa[x.deId]; val b = mapa[x.paraId]
            if (a != null && b != null) {
                val (ax, ay) = tela(a.x, a.y, 14f); val (bx, by) = tela(b.x, b.y, 14f)
                tubo.color = escurecer(FluidosLinha.cor(x.fluido).toInt(), 0.7f); tubo.strokeWidth = 7f * escala
                c.drawLine(ax, ay, bx, by, tubo)
                tubo.color = FluidosLinha.cor(x.fluido).toInt(); tubo.strokeWidth = 4.5f * escala
                c.drawLine(ax, ay, bx, by, tubo)
            }
        }

        // Componentes como blocos extrudados (ordenados de trás p/ frente)
        estado.componentes.sortedBy { val (rx, ry) = rot(it.x, it.y); rx + ry }.forEach { comp ->
            desenharBloco3D(c, comp, escala, ::tela)
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

    private fun desenharBloco3D(c: Canvas, comp: CompIso, escala: Float, tela: (Float, Float, Float) -> Pair<Float, Float>) {
        val fx = 26f; val fy = 18f
        val hz = altura3d(comp.categoria, comp.tipo)
        val cor = if (comp.cor != 0L) comp.cor.toInt() else corCategoria(comp.categoria)

        // Cantos do topo (Z=hz) e da base (Z=0) na ordem A,B,C,D
        val topo = listOf(
            tela(comp.x - fx, comp.y - fy, hz), tela(comp.x + fx, comp.y - fy, hz),
            tela(comp.x + fx, comp.y + fy, hz), tela(comp.x - fx, comp.y + fy, hz),
        )
        val base = listOf(
            tela(comp.x - fx, comp.y - fy, 0f), tela(comp.x + fx, comp.y - fy, 0f),
            tela(comp.x + fx, comp.y + fy, 0f), tela(comp.x - fx, comp.y + fy, 0f),
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
        tipo == "Compressor" || tipo == "Secador" || tipo == "Booster" -> 66f
        categoria == "Equipamentos" -> 58f
        categoria == "Consumidores" -> 48f
        categoria == "Filtros" -> 42f
        categoria == "Válvulas" -> 34f
        categoria == "Instrumentação" -> 30f
        else -> 18f
    }

    private fun corCategoria(categoria: String): Int = when (categoria) {
        "Equipamentos" -> Color.rgb(21, 101, 192)
        "Válvulas" -> Color.rgb(245, 124, 0)
        "Filtros" -> Color.rgb(123, 31, 162)
        "Instrumentação" -> Color.rgb(0, 137, 123)
        "Consumidores" -> Color.rgb(84, 110, 122)
        "Conexões" -> Color.rgb(57, 73, 171)
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
    fun enquadrar3d(estado: EstadoProjeto, w: Int, h: Int, azimute: Int): Float {
        if (estado.componentes.isEmpty()) return 1f
        fun rot(x: Float, y: Float): Pair<Float, Float> = when (((azimute % 360) + 360) % 360) {
            90 -> y to -x; 180 -> -x to -y; 270 -> -y to x; else -> x to y
        }
        val pts = estado.componentes.map { val (rx, ry) = rot(it.x, it.y); (rx - ry) * 0.866f to (rx + ry) * 0.5f }
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
