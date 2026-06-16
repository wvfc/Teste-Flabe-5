package br.com.refrigeracaopro.projeto

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import br.com.refrigeracaopro.data.CompIso

/**
 * Desenha símbolos técnicos (estilo CAD) de cada componente, dentro de uma área
 * centrada em (cx,cy) com meio-tamanho (hw,hh). Usado tanto na vista 2D quanto
 * na face superior dos blocos 3D.
 */
object SimbolosComponente {

    fun desenhar(c: Canvas, comp: CompIso, cx: Float, cy: Float, hw: Float, hh: Float, s: Float, cor: Int) {
        val p = Paint().apply {
            color = cor; isAntiAlias = true; style = Paint.Style.STROKE
            strokeWidth = (2.2f * s).coerceAtLeast(1.5f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint().apply { color = cor; isAntiAlias = true; style = Paint.Style.FILL }
        val l = cx - hw; val r = cx + hw; val t = cy - hh; val b = cy + hh

        when {
            // ---------- Tubulação ----------
            comp.tipo == "Tubo vertical" -> { c.drawLine(cx, t, cx, b, p); c.drawLine(cx - hw * 0.4f, t, cx - hw * 0.4f, b, p); c.drawLine(cx + hw * 0.4f, t, cx + hw * 0.4f, b, p) }
            comp.tipo == "Tubo flexível" -> { val path = Path(); path.moveTo(l, cy); var x = l; var up = true; val step = hw / 3f; while (x < r) { path.quadTo(x + step / 2, cy + (if (up) -hh * 0.6f else hh * 0.6f), x + step, cy); x += step; up = !up }; c.drawPath(path, p) }
            comp.categoria == "Tubulações" -> { c.drawLine(l, cy - hh * 0.45f, r, cy - hh * 0.45f, p); c.drawLine(l, cy + hh * 0.45f, r, cy + hh * 0.45f, p) }

            // ---------- Conexões ----------
            comp.tipo.startsWith("Curva") -> { val path = Path(); path.moveTo(l, cy + hh * 0.5f); path.lineTo(cx, cy + hh * 0.5f); path.quadTo(cx + hw * 0.5f, cy + hh * 0.5f, cx + hw * 0.5f, cy - hh * 0.5f); c.drawPath(path, p) }
            comp.tipo == "Tee" || comp.tipo == "Tee Redução" -> { c.drawLine(l, cy - hh * 0.3f, r, cy - hh * 0.3f, p); c.drawLine(cx, cy - hh * 0.3f, cx, b, p) }
            comp.tipo == "Cruzeta" -> { c.drawLine(l, cy, r, cy, p); c.drawLine(cx, t, cx, b, p) }
            comp.tipo.startsWith("Redução") -> { val path = Path(); path.moveTo(l, cy - hh * 0.5f); path.lineTo(cx, cy - hh * 0.25f); path.lineTo(r, cy - hh * 0.2f); path.moveTo(l, cy + hh * 0.5f); path.lineTo(cx, cy + hh * 0.25f); path.lineTo(r, cy + hh * 0.2f); c.drawPath(path, p) }
            comp.categoria == "Conexões" -> { c.drawLine(l, cy, r, cy, p); c.drawLine(cx - hw * 0.3f, cy - hh * 0.4f, cx - hw * 0.3f, cy + hh * 0.4f, p); c.drawLine(cx + hw * 0.3f, cy - hh * 0.4f, cx + hw * 0.3f, cy + hh * 0.4f, p) }

            // ---------- Válvulas (gravata-borboleta) ----------
            comp.categoria == "Válvulas" -> {
                val path = Path(); path.moveTo(l, cy - hh * 0.5f); path.lineTo(l, cy + hh * 0.5f); path.lineTo(r, cy - hh * 0.5f); path.lineTo(r, cy + hh * 0.5f); path.close()
                c.drawPath(path, p)
                c.drawLine(cx, cy, cx, t, p) // haste
                val tv = comp.tipo.lowercase()
                when {
                    tv.contains("esfera") -> c.drawCircle(cx, cy, hh * 0.22f, p)
                    tv.contains("retenção") -> { val a = Path(); a.moveTo(cx - hw * 0.2f, cy + hh * 0.25f); a.lineTo(cx, cy - hh * 0.1f); a.lineTo(cx + hw * 0.2f, cy + hh * 0.25f); c.drawPath(a, p) }
                    tv.contains("segurança") -> { var yy = t; val zz = Path(); zz.moveTo(cx, cy); var dir = 1; while (yy < cy) { zz.lineTo(cx + dir * hw * 0.18f, yy); yy += hh * 0.18f; dir = -dir }; c.drawPath(zz, p) }
                    tv.contains("solenóide") -> c.drawRect(cx - hw * 0.18f, t, cx + hw * 0.18f, cy - hh * 0.2f, p)
                    tv.contains("expansão") -> { val d = Path(); d.moveTo(cx - hw * 0.2f, cy - hh * 0.5f); d.lineTo(cx + hw * 0.2f, t); d.lineTo(cx, cy - hh * 0.15f); c.drawPath(d, p) }
                    tv.contains("agulha") -> { c.drawLine(cx, cy, cx, cy - hh * 0.5f, p); c.drawLine(cx - hw * 0.12f, t, cx, cy - hh * 0.2f, p); c.drawLine(cx + hw * 0.12f, t, cx, cy - hh * 0.2f, p) }
                    else -> c.drawLine(cx - hw * 0.3f, t, cx + hw * 0.3f, t, p) // volante (gaveta/globo)
                }
            }

            // ---------- Filtros / separadores ----------
            comp.tipo.startsWith("Filtro") || comp.tipo.startsWith("Separador") -> {
                val rect = RectF(l + hw * 0.2f, t + hh * 0.2f, r - hw * 0.2f, b - hh * 0.2f)
                c.drawRoundRect(rect, hw * 0.15f, hh * 0.15f, p)
                if (comp.tipo.contains("Separador")) { c.drawLine(rect.left, cy, rect.right, cy, p); c.drawCircle(cx, cy + hh * 0.18f, hh * 0.12f, p) }
                else { var x = rect.left + hw * 0.18f; while (x < rect.right) { c.drawLine(x, rect.top, x - hw * 0.18f, rect.bottom, p); x += hw * 0.18f } }
            }

            // ---------- Instrumentação ----------
            comp.tipo == "Manômetro" -> { c.drawCircle(cx, cy - hh * 0.1f, hh * 0.5f, p); c.drawLine(cx, cy - hh * 0.1f, cx + hw * 0.3f, cy - hh * 0.35f, p); c.drawLine(cx, cy + hh * 0.4f, cx, b, p) }
            comp.tipo == "Sensor temperatura" -> { c.drawLine(cx, t, cx, cy + hh * 0.3f, p); c.drawCircle(cx, cy + hh * 0.45f, hh * 0.2f, fill) }
            comp.tipo == "Pressostato" -> { c.drawCircle(cx, cy, hh * 0.55f, p); desenharTexto(c, "P", cx, cy, hh * 0.6f, cor) }
            comp.categoria == "Instrumentação" -> { c.drawCircle(cx, cy, hh * 0.55f, p); desenharTexto(c, if (comp.tipo.contains("Fluxo")) "F" else "M", cx, cy, hh * 0.6f, cor) }

            // ---------- Equipamentos (refrigeração / ar comprimido) ----------
            comp.tipo == "Reservatório" || comp.tipo == "Torre" || comp.tipo.startsWith("Acumulador") -> { val rt = hh * 0.25f; c.drawOval(RectF(l + hw * 0.25f, t, r - hw * 0.25f, t + rt * 2), p); c.drawLine(l + hw * 0.25f, t + rt, l + hw * 0.25f, b - rt, p); c.drawLine(r - hw * 0.25f, t + rt, r - hw * 0.25f, b - rt, p); c.drawOval(RectF(l + hw * 0.25f, b - rt * 2, r - hw * 0.25f, b), p) }
            comp.tipo.startsWith("Compressor") -> { c.drawRoundRect(RectF(l + hw * 0.15f, t + hh * 0.25f, r - hw * 0.15f, b - hh * 0.15f), 6f * s, 6f * s, p); c.drawCircle(cx - hw * 0.25f, cy, hh * 0.28f, p) }
            comp.tipo == "Condensadora" || comp.tipo == "Evaporadora" -> { c.drawRect(l + hw * 0.15f, t + hh * 0.2f, r - hw * 0.15f, b - hh * 0.15f, p); var x = l + hw * 0.25f; while (x < r - hw * 0.15f) { c.drawLine(x, t + hh * 0.2f, x, b - hh * 0.15f, p); x += hw * 0.18f } }
            comp.tipo == "Chiller" -> { c.drawRoundRect(RectF(l + hw * 0.1f, t + hh * 0.15f, r - hw * 0.1f, b - hh * 0.1f), 6f * s, 6f * s, p); c.drawCircle(cx, cy, hh * 0.28f, p); desenharTexto(c, "❄", cx, cy, hh * 0.45f, cor) }
            comp.tipo == "Visor de líquido" -> { c.drawCircle(cx, cy, hh * 0.5f, p); c.drawLine(cx - hw * 0.5f, cy, cx - hh * 0.5f, cy, p); c.drawLine(cx + hh * 0.5f, cy, cx + hw * 0.5f, cy, p) }
            comp.tipo == "Bomba" -> { c.drawCircle(cx, cy, hh * 0.5f, p); val tri = Path(); tri.moveTo(cx - hw * 0.25f, cy - hh * 0.25f); tri.lineTo(cx + hw * 0.35f, cy); tri.lineTo(cx - hw * 0.25f, cy + hh * 0.25f); tri.close(); c.drawPath(tri, p) }
            comp.categoria == "Refrigeração" || comp.categoria == "Ar comprimido" -> { c.drawRoundRect(RectF(l + hw * 0.15f, t + hh * 0.15f, r - hw * 0.15f, b - hh * 0.15f), 6f * s, 6f * s, p); for (i in 1..3) c.drawLine(l + hw * 0.15f, t + hh * 0.15f + i * (hh * 0.7f / 4), r - hw * 0.15f, t + hh * 0.15f + i * (hh * 0.7f / 4), p) }

            // ---------- Genérico ----------
            else -> { c.drawRect(l + hw * 0.2f, t + hh * 0.2f, r - hw * 0.2f, b - hh * 0.2f, p); c.drawLine(l + hw * 0.2f, t + hh * 0.2f, r - hw * 0.2f, b - hh * 0.2f, p) }
        }
    }

    private fun desenharTexto(c: Canvas, txt: String, cx: Float, cy: Float, tam: Float, cor: Int) {
        val tp = Paint().apply { color = cor; isAntiAlias = true; textAlign = Paint.Align.CENTER; isFakeBoldText = true; textSize = tam }
        c.drawText(txt, cx, cy + tam * 0.35f, tp)
    }
}
