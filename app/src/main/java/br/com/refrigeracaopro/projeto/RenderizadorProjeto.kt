package br.com.refrigeracaopro.projeto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import br.com.refrigeracaopro.data.CatalogoComponentes
import br.com.refrigeracaopro.data.CompIso
import br.com.refrigeracaopro.data.EstadoProjeto
import br.com.refrigeracaopro.data.FluidosLinha
import kotlin.math.tan

/**
 * Desenha o projeto isométrico em um Bitmap. O mesmo desenho é usado na tela do
 * editor (exibido como imagem) e na exportação (PNG/PDF), evitando duplicar a
 * lógica de renderização.
 */
object RenderizadorProjeto {

    const val COMP_W = 74f   // largura do componente em unidades de mundo
    const val COMP_H = 46f

    data class Vista(val escala: Float, val offX: Float, val offY: Float)

    private val fundo = Paint().apply { color = Color.WHITE }
    private val gradePaint = Paint().apply { color = Color.rgb(225, 232, 240); strokeWidth = 1f; isAntiAlias = true }

    fun render(
        estado: EstadoProjeto, w: Int, h: Int, escala: Float, offX: Float, offY: Float,
        mostrarGrade: Boolean, selecionadoId: Long?,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fundo)

        if (mostrarGrade) desenharGrade(c, w, h, escala)

        // Conexões (linhas coloridas por fluido)
        val linha = Paint().apply { strokeWidth = (3f * escala).coerceIn(2f, 8f); isAntiAlias = true; style = Paint.Style.STROKE }
        val mapa = estado.componentes.associateBy { it.id }
        estado.conexoes.forEach { x ->
            val a = mapa[x.deId]; val b = mapa[x.paraId]
            if (a != null && b != null) {
                linha.color = FluidosLinha.cor(x.fluido).toInt()
                c.drawLine(a.x * escala + offX, a.y * escala + offY, b.x * escala + offX, b.y * escala + offY, linha)
            }
        }

        // Componentes
        estado.componentes.forEach { comp -> desenharComponente(c, comp, escala, offX, offY, comp.id == selecionadoId) }
        return bmp
    }

    private fun desenharGrade(c: Canvas, w: Int, h: Int, escala: Float) {
        val passo = (28f * escala).coerceIn(14f, 80f)
        val incl = tan(Math.toRadians(30.0)).toFloat() // 30°
        // Linhas a +30° e -30° cobrindo a tela
        var x = -h * incl
        while (x < w + h * incl) {
            c.drawLine(x, 0f, x + h * incl, h.toFloat(), gradePaint)            // descendo p/ direita
            c.drawLine(x, h.toFloat(), x + h * incl, 0f, gradePaint)            // subindo p/ direita
            x += passo
        }
    }

    private fun desenharComponente(c: Canvas, comp: CompIso, escala: Float, offX: Float, offY: Float, selecionado: Boolean) {
        val cx = comp.x * escala + offX
        val cy = comp.y * escala + offY
        val wpx = COMP_W * escala
        val hpx = COMP_H * escala

        c.save()
        c.rotate(comp.rotacao, cx, cy)

        val corBase = if (comp.cor != 0L) comp.cor.toInt() else corCategoria(comp.categoria)
        val rect = RectF(cx - wpx / 2, cy - hpx / 2, cx + wpx / 2, cy + hpx / 2)
        val raio = 6f * escala

        val preenche = Paint().apply { color = corBase; isAntiAlias = true; alpha = 235 }
        c.drawRoundRect(rect, raio, raio, preenche)

        val borda = Paint().apply {
            color = if (selecionado) Color.rgb(46, 166, 107) else Color.rgb(40, 50, 70)
            style = Paint.Style.STROKE; strokeWidth = (if (selecionado) 3.5f else 1.5f) * escala; isAntiAlias = true
        }
        c.drawRoundRect(rect, raio, raio, borda)

        // Sigla central
        val sigla = Paint().apply { color = Color.WHITE; isAntiAlias = true; textAlign = Paint.Align.CENTER; isFakeBoldText = true; textSize = 13f * escala }
        c.drawText(CatalogoComponentes.sigla(comp.tipo), cx, cy + 4.5f * escala, sigla)
        c.restore()

        // Nome/etiqueta abaixo (sem rotação, para ficar legível)
        val rotulo = comp.etiqueta.ifBlank { comp.nome.ifBlank { comp.tipo } }
        val txt = Paint().apply { color = Color.rgb(40, 50, 70); isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 9.5f * escala }
        c.drawText(rotulo.take(18), cx, cy + hpx / 2 + 12f * escala, txt)
    }

    private fun corCategoria(categoria: String): Int = when (categoria) {
        "Equipamentos" -> Color.rgb(21, 101, 192)
        "Válvulas" -> Color.rgb(245, 124, 0)
        "Filtros" -> Color.rgb(123, 31, 162)
        "Instrumentação" -> Color.rgb(0, 137, 123)
        "Consumidores" -> Color.rgb(84, 110, 122)
        "Conexões" -> Color.rgb(57, 73, 171)
        else -> Color.rgb(13, 44, 79) // Tubulação
    }

    /** Calcula a vista para enquadrar todos os componentes em [w]x[h] (export). */
    fun enquadrar(estado: EstadoProjeto, w: Int, h: Int): Vista {
        if (estado.componentes.isEmpty()) return Vista(1f, w / 2f, h / 2f)
        val minX = estado.componentes.minOf { it.x } - COMP_W
        val maxX = estado.componentes.maxOf { it.x } + COMP_W
        val minY = estado.componentes.minOf { it.y } - COMP_H
        val maxY = estado.componentes.maxOf { it.y } + COMP_H
        val larg = (maxX - minX).coerceAtLeast(1f)
        val alt = (maxY - minY).coerceAtLeast(1f)
        val margem = 0.9f
        val escala = minOf(w / larg, h / alt) * margem
        val offX = w / 2f - (minX + maxX) / 2f * escala
        val offY = h / 2f - (minY + maxY) / 2f * escala
        return Vista(escala, offX, offY)
    }
}
