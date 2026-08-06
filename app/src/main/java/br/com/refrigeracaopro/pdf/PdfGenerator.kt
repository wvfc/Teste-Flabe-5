package br.com.refrigeracaopro.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import br.com.refrigeracaopro.data.Cliente
import br.com.refrigeracaopro.data.EnsaioIsolacao
import br.com.refrigeracaopro.data.Equipamento
import br.com.refrigeracaopro.data.Megohmetro
import br.com.refrigeracaopro.data.OrdemServico
import br.com.refrigeracaopro.data.Relatorio
import br.com.refrigeracaopro.data.Prefs.cnpjEmpresa
import br.com.refrigeracaopro.data.Prefs.emailEmpresa
import br.com.refrigeracaopro.data.Prefs.enderecoEmpresa
import br.com.refrigeracaopro.data.Prefs.logoEmpresa
import br.com.refrigeracaopro.data.Prefs.nomeEmpresa
import br.com.refrigeracaopro.data.Prefs.nomeTecnico
import br.com.refrigeracaopro.data.Prefs.registroTecnico
import br.com.refrigeracaopro.data.Prefs.telefoneEmpresa
import br.com.refrigeracaopro.util.Arquivos
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Geração de PDF (A4) usando a API nativa android.graphics.pdf — sem
 * dependências externas, funciona offline.
 */
object PdfGenerator {

    private const val LARGURA = 595 // A4 em pontos (72 dpi)
    private const val ALTURA = 842
    private const val MARGEM = 40f

    private val AZUL_ESCURO = Color.rgb(13, 44, 79)
    private val VERDE = Color.rgb(46, 166, 107)
    private val CINZA = Color.rgb(120, 120, 120)
    private val CINZA_CLARO = Color.rgb(235, 238, 242)

    private val formatoData = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /** Construtor de páginas com controle de cursor vertical e quebra automática. */
    private class Construtor(val context: Context, val documento: PdfDocument, val rodape: String) {
        var pagina: PdfDocument.Page = novaPagina()
        var y = MARGEM
        var numeroPagina = 1

        val tituloPaint = Paint().apply { color = AZUL_ESCURO; textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val secaoPaint = Paint().apply { color = Color.WHITE; textSize = 12f; isFakeBoldText = true; isAntiAlias = true }
        val rotuloPaint = Paint().apply { color = CINZA; textSize = 9f; isAntiAlias = true }
        val textoPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val pequenoPaint = Paint().apply { color = CINZA; textSize = 8f; isAntiAlias = true }
        val fundoSecao = Paint().apply { color = AZUL_ESCURO }
        val fundoLinha = Paint().apply { color = CINZA_CLARO }
        val linhaPaint = Paint().apply { color = VERDE; strokeWidth = 2f }
        // Filtragem para imagens ficarem suaves (sem serrilhado) ao redimensionar
        val imgPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true; isDither = true }

        fun novaPagina(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create()
            return documento.startPage(info)
        }

        fun canvas(): Canvas = pagina.canvas

        fun fecharPagina() {
            // Rodapé com numeração automática
            canvas().drawText(rodape, MARGEM, ALTURA - 20f, pequenoPaint)
            val numTexto = "Página $numeroPagina"
            canvas().drawText(numTexto, LARGURA - MARGEM - pequenoPaint.measureText(numTexto), ALTURA - 20f, pequenoPaint)
            documento.finishPage(pagina)
        }

        fun garantirEspaco(altura: Float) {
            if (y + altura > ALTURA - MARGEM - 10f) {
                fecharPagina()
                numeroPagina++
                pagina = novaPagina()
                y = MARGEM
            }
        }

        fun cabecalho(tituloDoc: String, numeroDoc: String) {
            // Logo configurável da empresa (se cadastrada)
            val caminhoLogo = context.logoEmpresa
            var xTexto = MARGEM
            if (caminhoLogo.isNotBlank() && File(caminhoLogo).exists()) {
                BitmapFactory.decodeFile(caminhoLogo)?.let { logo ->
                    val alvo = 48f
                    val proporcao = alvo / logo.height
                    val destino = Rect(
                        MARGEM.toInt(), y.toInt(),
                        (MARGEM + logo.width * proporcao).toInt(), (y + alvo).toInt()
                    )
                    canvas().drawBitmap(logo, null, destino, imgPaint)
                    xTexto = destino.right + 12f
                }
            }
            val empresa = context.nomeEmpresa.ifBlank { "Gestão Pro" }
            canvas().drawText(empresa, xTexto, y + 16f, tituloPaint)
            val linhas = listOfNotNull(
                context.cnpjEmpresa.takeIf { it.isNotBlank() }?.let { "CNPJ: $it" },
                listOf(context.telefoneEmpresa, context.emailEmpresa).filter { it.isNotBlank() }
                    .joinToString("  •  ").takeIf { it.isNotBlank() },
                context.enderecoEmpresa.takeIf { it.isNotBlank() },
            )
            var yl = y + 30f
            linhas.forEach { canvas().drawText(it, xTexto, yl, pequenoPaint); yl += 11f }

            // Título e número do documento à direita
            val numPaint = Paint(tituloPaint).apply { color = VERDE; textSize = 14f }
            canvas().drawText(tituloDoc, LARGURA - MARGEM - tituloPaint.measureText(tituloDoc), y + 16f, tituloPaint)
            canvas().drawText(numeroDoc, LARGURA - MARGEM - numPaint.measureText(numeroDoc), y + 34f, numPaint)

            y = maxOf(yl, y + 56f)
            canvas().drawLine(MARGEM, y, LARGURA - MARGEM, y, linhaPaint)
            y += 14f
        }

        fun secao(titulo: String) {
            garantirEspaco(34f)
            y += 6f
            canvas().drawRect(MARGEM, y, LARGURA - MARGEM, y + 18f, fundoSecao)
            canvas().drawText(titulo.uppercase(), MARGEM + 8f, y + 13f, secaoPaint)
            y += 26f
        }

        /** Par rótulo/valor em duas colunas. */
        fun camposDuasColunas(campos: List<Pair<String, String>>) {
            val visiveis = campos.filter { it.second.isNotBlank() }
            val metade = LARGURA / 2f
            var i = 0
            while (i < visiveis.size) {
                garantirEspaco(26f)
                val (r1, v1) = visiveis[i]
                canvas().drawText(r1.uppercase(), MARGEM, y, rotuloPaint)
                canvas().drawText(v1, MARGEM, y + 12f, textoPaint)
                if (i + 1 < visiveis.size) {
                    val (r2, v2) = visiveis[i + 1]
                    canvas().drawText(r2.uppercase(), metade, y, rotuloPaint)
                    canvas().drawText(v2, metade, y + 12f, textoPaint)
                }
                y += 26f
                i += 2
            }
        }

        /** Texto longo com quebra de linha automática. */
        fun paragrafo(texto: String) {
            if (texto.isBlank()) return
            val larguraUtil = LARGURA - 2 * MARGEM
            texto.split("\n").forEach { linha ->
                var restante = linha.trim()
                if (restante.isEmpty()) { y += 6f; return@forEach }
                while (restante.isNotEmpty()) {
                    var corte = restante.length
                    while (textoPaint.measureText(restante.substring(0, corte)) > larguraUtil) {
                        val espaco = restante.lastIndexOf(' ', corte - 1)
                        corte = if (espaco > 0) espaco else corte - 1
                    }
                    garantirEspaco(16f)
                    canvas().drawText(restante.substring(0, corte).trim(), MARGEM, y, textoPaint)
                    y += 14f
                    restante = restante.substring(corte).trim()
                }
            }
            y += 4f
        }

        /** Quebra um texto em linhas que cabem em [larguraMax] para o [paint]. */
        fun quebrar(texto: String, larguraMax: Float, paint: Paint): List<String> {
            if (texto.isBlank()) return listOf("")
            val linhas = mutableListOf<String>()
            texto.split("\n").forEach { bruta ->
                var restante = bruta.trim()
                if (restante.isEmpty()) { linhas.add(""); return@forEach }
                while (restante.isNotEmpty()) {
                    var corte = restante.length
                    while (corte > 1 && paint.measureText(restante.substring(0, corte)) > larguraMax) {
                        val espaco = restante.lastIndexOf(' ', corte - 1)
                        corte = if (espaco > 0) espaco else corte - 1
                    }
                    linhas.add(restante.substring(0, corte).trim())
                    restante = restante.substring(corte).trim()
                }
            }
            return linhas
        }

        /** Tabela de duas colunas (medição / valor) com quebra de texto e zebra. */
        fun tabela(linhas: List<Pair<String, String>>) {
            val visiveis = linhas.filter { it.second.isNotBlank() }
            if (visiveis.isEmpty()) return
            val colValor = MARGEM + (LARGURA - 2 * MARGEM) * 0.54f
            val largRotulo = colValor - (MARGEM + 6f) - 8f
            val largValor = (LARGURA - MARGEM) - colValor - 4f
            val alturaLinha = 13f
            visiveis.forEachIndexed { indice, (rotulo, valor) ->
                val lr = quebrar(rotulo, largRotulo, textoPaint)
                val lv = quebrar(valor, largValor, textoPaint)
                val n = maxOf(lr.size, lv.size)
                val alturaRow = n * alturaLinha + 6f
                garantirEspaco(alturaRow)
                if (indice % 2 == 0) canvas().drawRect(MARGEM, y - 10f, LARGURA - MARGEM, y - 10f + alturaRow, fundoLinha)
                lr.forEachIndexed { i, l -> canvas().drawText(l, MARGEM + 6f, y + i * alturaLinha, textoPaint) }
                lv.forEachIndexed { i, l -> canvas().drawText(l, colValor, y + i * alturaLinha, textoPaint) }
                y += alturaRow
            }
            y += 6f
        }

        /** Desenha a foto dentro da célula preservando a proporção (sem distorcer). */
        fun desenharFoto(caminho: String, x: Float, yTopo: Float, larguraCelula: Float, alturaCelula: Float) {
            val bmp = BitmapFactory.decodeFile(caminho) ?: return
            val ratio = minOf(larguraCelula / bmp.width, alturaCelula / bmp.height)
            val w = bmp.width * ratio
            val h = bmp.height * ratio
            val dx = x + (larguraCelula - w) / 2f
            val dy = yTopo + (alturaCelula - h) / 2f
            val destino = Rect(dx.toInt(), dy.toInt(), (dx + w).toInt(), (dy + h).toInt())
            canvas().drawBitmap(bmp, null, destino, imgPaint)
            bmp.recycle()
        }

        /** Desenha um bitmap (ex.: imagem do projeto) ocupando a largura útil. */
        fun imagem(bmp: Bitmap) {
            val larg = LARGURA - 2 * MARGEM
            val alt = larg * bmp.height / bmp.width.coerceAtLeast(1)
            garantirEspaco(alt + 6f)
            val destino = Rect(MARGEM.toInt(), y.toInt(), (MARGEM + larg).toInt(), (y + alt).toInt())
            canvas().drawBitmap(bmp, null, destino, imgPaint)
            // moldura
            canvas().drawRect(destino, Paint().apply { color = CINZA; style = Paint.Style.STROKE; strokeWidth = 1f })
            y += alt + 8f
        }

        /** Legenda de cores das linhas/fluidos. */
        fun legendaCores(itens: List<Pair<String, Int>>) {
            garantirEspaco(20f)
            var x = MARGEM
            itens.forEach { (rotulo, cor) ->
                val largura = textoPaint.measureText(rotulo) + 22f
                if (x + largura > LARGURA - MARGEM) { x = MARGEM; y += 16f; garantirEspaco(16f) }
                canvas().drawRect(x, y - 8f, x + 12f, y + 2f, Paint().apply { color = cor })
                canvas().drawText(rotulo, x + 16f, y, textoPaint)
                x += largura + 8f
            }
            y += 14f
        }

        /** Grade de fotos (2 por linha). */
        fun fotos(titulo: String, caminhos: List<String>) {
            val existentes = caminhos.filter { File(it).exists() }
            if (existentes.isEmpty()) return
            secao(titulo)
            val larguraFoto = (LARGURA - 2 * MARGEM - 12f) / 2f
            val alturaFoto = larguraFoto * 0.75f
            var i = 0
            while (i < existentes.size) {
                garantirEspaco(alturaFoto + 10f)
                for (col in 0..1) {
                    if (i + col >= existentes.size) break
                    desenharFoto(existentes[i + col], MARGEM + col * (larguraFoto + 12f), y, larguraFoto, alturaFoto)
                }
                y += alturaFoto + 10f
                i += 2
            }
        }

        /** Campos de assinatura lado a lado, com imagem quando capturada na tela. */
        fun assinaturas(itens: List<Pair<String, String>>) {
            if (itens.isEmpty()) return
            garantirEspaco(90f)
            y += 10f
            val largura = (LARGURA - 2 * MARGEM - 24f) / itens.size
            itens.forEachIndexed { indice, (rotulo, caminho) ->
                val x = MARGEM + indice * (largura + 24f)
                if (caminho.isNotBlank() && File(caminho).exists()) {
                    BitmapFactory.decodeFile(caminho)?.let { bmp ->
                        val destino = Rect(x.toInt(), y.toInt(), (x + largura).toInt(), (y + 50f).toInt())
                        canvas().drawBitmap(bmp, null, destino, imgPaint)
                        bmp.recycle()
                    }
                }
                canvas().drawLine(x, y + 55f, x + largura, y + 55f, Paint().apply { color = Color.BLACK })
                val rotuloLargura = textoPaint.measureText(rotulo)
                canvas().drawText(rotulo, x + (largura - rotuloLargura) / 2f, y + 70f, textoPaint)
            }
            y += 85f
        }

        /** Fotos com legenda (observação) abaixo de cada imagem. */
        fun fotosComLegenda(titulo: String, itens: List<Pair<String, String>>) {
            val existentes = itens.filter { File(it.first).exists() }
            if (existentes.isEmpty()) return
            secao(titulo)
            val larguraFoto = (LARGURA - 2 * MARGEM - 12f) / 2f
            val alturaFoto = larguraFoto * 0.75f
            var i = 0
            while (i < existentes.size) {
                garantirEspaco(alturaFoto + 34f)
                val linha = existentes.subList(i, minOf(i + 2, existentes.size))
                linha.forEachIndexed { col, (caminho, _) ->
                    desenharFoto(caminho, MARGEM + col * (larguraFoto + 12f), y, larguraFoto, alturaFoto)
                }
                // Legendas abaixo
                val yLegenda = y + alturaFoto + 11f
                linha.forEachIndexed { col, (_, obs) ->
                    if (obs.isNotBlank()) {
                        val x = MARGEM + col * (larguraFoto + 12f)
                        val texto = if (obs.length > 45) obs.take(44) + "…" else obs
                        canvas().drawText(texto, x, yLegenda, pequenoPaint)
                    }
                }
                y += alturaFoto + 24f
                i += 2
            }
        }
    }

    private fun dadosCliente(cliente: Cliente?) = listOf(
        "Cliente" to (cliente?.nome ?: ""),
        "CPF/CNPJ" to (cliente?.cpfCnpj ?: ""),
        "Telefone" to (cliente?.telefone ?: ""),
        "E-mail" to (cliente?.email ?: ""),
        "Endereço" to (cliente?.endereco ?: ""),
        "Cidade/UF" to listOfNotNull(
            cliente?.cidade?.takeIf { it.isNotBlank() },
            cliente?.estado?.takeIf { it.isNotBlank() }
        ).joinToString(" / "),
    )

    private fun dadosEquipamento(equip: Equipamento?) = listOf(
        "Equipamento" to (equip?.tipo ?: ""),
        "Marca / Modelo" to listOfNotNull(
            equip?.marca?.takeIf { it.isNotBlank() },
            equip?.modelo?.takeIf { it.isNotBlank() }
        ).joinToString(" / "),
        "Nº de série" to (equip?.numeroSerie ?: ""),
        "Fluido refrigerante" to (equip?.fluido ?: ""),
        "Tensão / Potência" to listOfNotNull(
            equip?.tensao?.takeIf { it.isNotBlank() },
            equip?.potencia?.takeIf { it.isNotBlank() }
        ).joinToString(" / "),
        "Local de instalação" to (equip?.localInstalacao ?: ""),
    )

    /** Gera o PDF de uma Ordem de Serviço e retorna o arquivo. */
    fun gerarOrdemServico(
        context: Context,
        os: OrdemServico,
        cliente: Cliente?,
        equipamento: Equipamento?,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)

        b.cabecalho("ORDEM DE SERVIÇO", os.numero)

        b.secao("Dados gerais")
        b.camposDuasColunas(
            listOf(
                "Data/Hora" to formatoData.format(Date(os.dataHora)),
                "Status" to os.status,
                "Técnico responsável" to os.tecnico,
            )
        )

        b.secao("Cliente")
        b.camposDuasColunas(dadosCliente(cliente))

        if (equipamento != null) {
            b.secao("Equipamento")
            b.camposDuasColunas(dadosEquipamento(equipamento))
        }

        if (os.defeitoInformado.isNotBlank()) { b.secao("Defeito informado pelo cliente"); b.paragrafo(os.defeitoInformado) }
        if (os.diagnostico.isNotBlank()) { b.secao("Diagnóstico técnico"); b.paragrafo(os.diagnostico) }
        if (os.servicosExecutados.isNotBlank()) { b.secao("Serviços executados"); b.paragrafo(os.servicosExecutados) }
        if (os.pecasUtilizadas.isNotBlank()) { b.secao("Peças utilizadas"); b.paragrafo(os.pecasUtilizadas) }

        b.secao("Valores")
        b.tabela(
            listOf(
                "Mão de obra" to formatoMoeda.format(os.valorMaoDeObra),
                "Peças" to formatoMoeda.format(os.valorPecas),
                "TOTAL" to formatoMoeda.format(os.valorTotal),
            )
        )

        b.fotos("Fotos — antes", Arquivos.textoParaLista(os.fotosAntes))
        b.fotos("Fotos — durante", Arquivos.textoParaLista(os.fotosDurante))
        b.fotos("Fotos — depois", Arquivos.textoParaLista(os.fotosDepois))

        b.assinaturas(
            listOf(
                (context.nomeTecnico.ifBlank { os.tecnico }.ifBlank { "Técnico" } +
                    context.registroTecnico.let { if (it.isNotBlank()) " — $it" else "" }) to "",
                (cliente?.nome ?: "Cliente") to os.assinaturaCliente,
            )
        )

        b.fecharPagina()
        val arquivo = File(Arquivos.pastaPdfs(context), "${os.numero}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /** Gera o PDF de um Relatório Técnico completo e retorna o arquivo. */
    fun gerarRelatorio(
        context: Context,
        rel: Relatorio,
        cliente: Cliente?,
        equipamento: Equipamento?,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)

        b.cabecalho("RELATÓRIO TÉCNICO", rel.numero)

        b.secao("Dados gerais")
        b.camposDuasColunas(
            listOf(
                "Data/Hora" to formatoData.format(Date(rel.dataHora)),
                "Técnico" to context.nomeTecnico,
                "Registro profissional" to context.registroTecnico,
            )
        )

        b.secao("Cliente")
        b.camposDuasColunas(dadosCliente(cliente))

        if (equipamento != null) {
            b.secao("Equipamento")
            b.camposDuasColunas(dadosEquipamento(equipamento))
        }

        if (rel.motivoVisita.isNotBlank()) { b.secao("Motivo da visita"); b.paragrafo(rel.motivoVisita) }
        if (rel.diagnostico.isNotBlank()) { b.secao("Diagnóstico"); b.paragrafo(rel.diagnostico) }

        // Medições técnicas só no relatório de refrigeração (o "Geral" é simples)
        if (rel.tipo == br.com.refrigeracaopro.data.TipoRelatorio.REFRIGERACAO) {
            b.secao("Medições")
            b.tabela(
                listOf(
                    "Fluido refrigerante" to rel.fluido,
                    "Pressão de sucção" to rel.pressaoSuccao.comUnidade("psi"),
                    "Pressão de descarga" to rel.pressaoDescarga.comUnidade("psi"),
                    "Temperatura linha de sucção" to rel.tempLinhaSuccao.comUnidade("°C"),
                    "Temperatura linha de líquido" to rel.tempLinhaLiquido.comUnidade("°C"),
                    "Temperatura ambiente" to rel.tempAmbiente.comUnidade("°C"),
                    "Temperatura interna" to rel.tempInterna.comUnidade("°C"),
                    "Temperatura de evaporação" to rel.tempEvaporacao.comUnidade("°C"),
                    "Temperatura de condensação" to rel.tempCondensacao.comUnidade("°C"),
                    "Corrente elétrica" to rel.correnteEletrica.comUnidade("A"),
                    "Tensão elétrica" to rel.tensaoEletrica.comUnidade("V"),
                    "SUPERAQUECIMENTO" to rel.superaquecimento.comUnidade("K"),
                    "SUBRESFRIAMENTO" to rel.subresfriamento.comUnidade("K"),
                )
            )
        }

        if (rel.diagnosticoIA.isNotBlank()) { b.secao("Diagnóstico assistido por IA"); b.paragrafo(rel.diagnosticoIA) }
        if (rel.servicosRealizados.isNotBlank()) { b.secao("Serviços realizados"); b.paragrafo(rel.servicosRealizados) }
        if (rel.pecasSubstituidas.isNotBlank()) { b.secao("Peças substituídas"); b.paragrafo(rel.pecasSubstituidas) }
        if (rel.recomendacoes.isNotBlank()) { b.secao("Recomendações técnicas"); b.paragrafo(rel.recomendacoes) }

        b.fotos("Fotos — antes", Arquivos.textoParaLista(rel.fotosAntes))
        b.fotos("Fotos — durante", Arquivos.textoParaLista(rel.fotosDurante))
        b.fotos("Fotos — depois", Arquivos.textoParaLista(rel.fotosDepois))

        if (rel.conclusao.isNotBlank()) { b.secao("Conclusão técnica"); b.paragrafo(rel.conclusao) }

        // Avisos técnicos/segurança no corpo do relatório
        b.secao("Avisos técnicos e de segurança")
        b.paragrafo(br.com.refrigeracaopro.data.Avisos.SEGURANCA.joinToString("\n") { "• $it" })

        b.assinaturas(
            listOf(
                (context.nomeTecnico.ifBlank { "Técnico" } +
                    context.registroTecnico.let { if (it.isNotBlank()) " — $it" else "" }) to rel.assinaturaTecnico,
                (cliente?.nome ?: "Cliente") to rel.assinaturaCliente,
            )
        )

        b.fecharPagina()
        val arquivo = File(Arquivos.pastaPdfs(context), "${rel.numero}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /** Gera o PDF do extrato financeiro de um mês. */
    fun gerarExtrato(
        context: Context,
        tituloMes: String,
        lancamentos: List<br.com.refrigeracaopro.data.Lancamento>,
        receita: Double,
        despesa: Double,
        saldo: Double,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)
        val formatoDia = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

        b.cabecalho("EXTRATO FINANCEIRO", tituloMes)

        b.secao("Resumo do período")
        b.tabela(
            listOf(
                "Total de receitas" to formatoMoeda.format(receita),
                "Total de despesas" to formatoMoeda.format(despesa),
                "SALDO DO PERÍODO" to formatoMoeda.format(saldo),
            )
        )

        b.secao("Lançamentos (${lancamentos.size})")
        if (lancamentos.isEmpty()) {
            b.paragrafo("Nenhum lançamento no período.")
        } else {
            b.tabela(
                lancamentos.sortedBy { it.data }.map { l ->
                    val sinal = if (l.tipo == br.com.refrigeracaopro.data.TipoLancamento.RECEITA) "+ " else "− "
                    val rotulo = "${formatoDia.format(Date(l.data))}  ${l.descricao}" +
                        if (l.categoria.isNotBlank()) " (${l.categoria})" else ""
                    rotulo to (sinal + formatoMoeda.format(l.valor))
                }
            )
        }

        b.fecharPagina()
        val arquivo = File(Arquivos.pastaPdfs(context), "extrato-${tituloMes.replace(" ", "_")}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /** Gera o PDF do relatório de inspeção de compressor de ar comprimido (checklist). */
    fun gerarRelatorioArComprimido(
        context: Context,
        rel: Relatorio,
        cliente: Cliente?,
        equipamento: Equipamento?,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)
        val valores = br.com.refrigeracaopro.data.RelatorioArComprimido.parse(rel.dadosExtra)

        b.cabecalho("INSPEÇÃO – AR COMPRIMIDO", rel.numero)

        b.secao("Dados gerais")
        b.camposDuasColunas(
            listOf(
                "Data/Hora" to formatoData.format(Date(rel.dataHora)),
                "Técnico" to context.nomeTecnico,
            )
        )
        b.secao("Cliente")
        b.camposDuasColunas(dadosCliente(cliente))
        if (equipamento != null) {
            b.secao("Equipamento")
            b.camposDuasColunas(dadosEquipamento(equipamento))
        }

        // Cada seção do checklist vira uma tabela rótulo/valor (somente preenchidos)
        br.com.refrigeracaopro.data.RelatorioArComprimido.SECOES.forEach { secao ->
            val linhas = secao.campos.mapNotNull { campo ->
                val valor = br.com.refrigeracaopro.data.RelatorioArComprimido.valorTexto(campo, valores)
                val marcador = valores[br.com.refrigeracaopro.data.RelatorioArComprimido.chaveMarcador(campo)].orEmpty()
                val texto = when {
                    valor.isNotBlank() && marcador.isNotBlank() -> "$valor   [$marcador]"
                    marcador.isNotBlank() -> "[$marcador]"
                    else -> valor
                }
                if (texto.isBlank()) null else campo.rotulo to texto
            }
            if (linhas.isNotEmpty()) {
                b.secao(secao.titulo)
                b.tabela(linhas)
            }
        }

        if (rel.recomendacoes.isNotBlank()) { b.secao("Recomendações técnicas"); b.paragrafo(rel.recomendacoes) }
        if (rel.conclusao.isNotBlank()) { b.secao("Conclusão"); b.paragrafo(rel.conclusao) }

        // Fotos com observação por imagem
        val fotosComObs = Arquivos.textoParaLista(rel.fotosAntes).map { it to (valores["foto::$it"].orEmpty()) }
        b.fotosComLegenda("Fotos da inspeção", fotosComObs)

        b.secao("Avisos técnicos e de segurança")
        b.paragrafo(br.com.refrigeracaopro.data.Avisos.SEGURANCA.joinToString("\n") { "• $it" })

        // Contato informado pelo cliente
        val contatoTipo = valores["contato_tipo"].orEmpty()
        val contatoValor = valores["contato_valor"].orEmpty()
        if (contatoValor.isNotBlank()) {
            b.secao("Contato do cliente")
            b.camposDuasColunas(listOf((contatoTipo.ifBlank { "Contato" }) to contatoValor))
        }

        b.assinaturas(
            listOf(
                (context.nomeTecnico.ifBlank { "Técnico" }) to rel.assinaturaTecnico,
                (cliente?.nome ?: "Cliente") to rel.assinaturaCliente,
            )
        )

        b.fecharPagina()
        val arquivo = File(Arquivos.pastaPdfs(context), "${rel.numero}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /** Gera o PDF do projeto isométrico (imagem + cálculos + materiais). */
    fun gerarProjetoIso(
        context: Context,
        projeto: br.com.refrigeracaopro.data.Projeto,
        cliente: Cliente?,
        imagem: Bitmap,
        calc: br.com.refrigeracaopro.data.CalculosProjeto.Resultado,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)

        b.cabecalho("PROJETO ISOMÉTRICO", projeto.nome)

        b.secao("Dados do projeto")
        b.camposDuasColunas(
            listOf(
                "Cliente" to (cliente?.nome ?: ""),
                "Equipamento" to projeto.equipamento,
                "Local" to projeto.local,
                "Responsável" to projeto.responsavel,
                "Data" to projeto.data,
                "Pressão de trabalho" to projeto.pressaoTrabalho,
                "Fluido" to projeto.fluido,
                "Vazão" to projeto.vazao,
                "Temperatura" to projeto.temperatura,
            )
        )

        b.secao("Desenho isométrico")
        b.imagem(imagem)
        b.legendaCores(
            br.com.refrigeracaopro.data.FluidosLinha.TODOS.map { it to br.com.refrigeracaopro.data.FluidosLinha.cor(it).toInt() }
        )

        b.secao("Cálculos (aproximados)")
        b.tabela(
            listOf(
                "Comprimento total" to "%.1f m".format(calc.comprimentoTotal),
                "Comprimento equivalente" to "%.1f m".format(calc.comprimentoEquivalente),
                "Curvas" to calc.qtdCurvas.toString(),
                "Tees" to calc.qtdTees.toString(),
                "Válvulas" to calc.qtdValvulas.toString(),
                "Registros" to calc.qtdRegistros.toString(),
                "Filtros" to calc.qtdFiltros.toString(),
                "Conexões" to calc.qtdConexoes.toString(),
                "Velocidade do fluido" to (calc.velocidade?.let { "%.2f m/s".format(it) } ?: ""),
                "Perda de carga aprox." to (calc.perdaCarga?.let { "%.2f bar".format(it) } ?: ""),
                "Pressão estimada" to (calc.pressaoEstimada?.let { "%.2f bar".format(it) } ?: ""),
                "Sugestão de diâmetro" to calc.sugestaoDiametro,
            )
        )

        b.secao("Lista de materiais")
        b.tabela(calc.materiais.map { ("${it.quantidade}× ${it.descricao}") to it.detalhe })

        if (projeto.observacoes.isNotBlank()) { b.secao("Observações"); b.paragrafo(projeto.observacoes) }

        b.assinaturas(listOf((projeto.responsavel.ifBlank { "Responsável" }) to ""))

        b.fecharPagina()
        val arquivo = File(Arquivos.pastaPdfs(context), "projeto-${projeto.id}.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /**
     * Gera o PDF do ensaio de isolação (megômetro), incluindo o histórico do
     * equipamento para leitura da tendência.
     *
     * [historico] deve vir do mais recente para o mais antigo, já contendo o
     * ensaio atual.
     */
    fun gerarEnsaioIsolacao(
        context: Context,
        ensaio: EnsaioIsolacao,
        cliente: Cliente?,
        equipamento: Equipamento?,
        resultado: Megohmetro.Resultado,
        historico: List<EnsaioIsolacao>,
    ): File {
        val doc = PdfDocument()
        val rodape = "Gerado por Gestão Pro em ${formatoData.format(Date())}"
        val b = Construtor(context, doc, rodape)

        b.cabecalho("ENSAIO DE ISOLAÇÃO", ensaio.numero)

        b.secao("Dados gerais")
        b.camposDuasColunas(
            listOf(
                "Data/Hora" to formatoData.format(Date(ensaio.dataHora)),
                "Técnico" to context.nomeTecnico,
                "Registro profissional" to context.registroTecnico,
                "Instrumento" to "Megômetro (megger)",
            )
        )

        b.secao("Cliente")
        b.camposDuasColunas(dadosCliente(cliente))

        if (equipamento != null) {
            b.secao("Equipamento")
            b.camposDuasColunas(dadosEquipamento(equipamento))
        }

        b.secao("Leituras do ensaio")
        b.tabela(
            listOf(
                "Tensão de teste aplicada" to numero(ensaio.tensaoV, 0).comUnidade("V"),
                "Leitura de 30 segundos (R30s)" to numero(ensaio.r30s).comUnidade("MΩ"),
                "Leitura de 1 minuto (R60s)" to numero(ensaio.r60s).comUnidade("MΩ"),
                "Leitura de 10 minutos (R10min)" to numero(ensaio.r10min).comUnidade("MΩ"),
                "Temperatura do equipamento" to (ensaio.tempC?.let { numero(it, 1) + " °C" } ?: "Não informada"),
                "Temperatura base de correção" to "${numero(ensaio.tempBase, 0)} °C",
            )
        )

        b.secao("Resultados")
        b.tabela(
            listOf(
                "Isolação puntual (60 s)" to (resultado.puntual?.let { numero(it) + " MΩ" } ?: ""),
                "Isolação corrigida para ${numero(ensaio.tempBase, 0)} °C" to
                    (resultado.puntualCorrigido?.let { numero(it) + " MΩ" } ?: ""),
                "Fator de correção aplicado" to (resultado.fatorTemperatura?.let { "× " + numero(it, 2) } ?: ""),
                "Índice de Absorção (DAR = R60s ÷ R30s)" to
                    (resultado.dar?.let { numero(it, 2) + "  —  " + resultado.classeDar } ?: ""),
                "Índice de Polarização (PI = R10min ÷ R1min)" to
                    (resultado.pi?.let { numero(it, 2) + "  —  " + resultado.classePi } ?: ""),
                "Mínimo de referência (kV + 1)" to (resultado.minimoRecomendado?.let {
                    numero(it, 1) + " MΩ  —  " + when (resultado.atendeMinimo) {
                        true -> "atendido"
                        false -> "NÃO atendido"
                        else -> "-"
                    }
                } ?: ""),
            )
        )

        resultado.condicao?.let { condicao ->
            b.secao("Diagnóstico — condição: ${condicao.rotulo.uppercase()}")
            b.paragrafo(resultado.diagnostico)
            if (resultado.recomendacao.isNotBlank()) b.paragrafo("Recomendação: ${resultado.recomendacao}")
            if (resultado.avisos.isNotEmpty()) {
                b.paragrafo(resultado.avisos.joinToString("\n") { "• $it" })
            }
        }

        if (ensaio.observacoes.isNotBlank()) {
            b.secao("Observações")
            b.paragrafo(ensaio.observacoes)
        }

        // Histórico do equipamento: base da análise de tendência
        if (historico.size > 1) {
            b.secao("Histórico e tendência")
            val formatoDia = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            b.tabela(
                historico.map { e ->
                    val valor = e.puntualCorrigido ?: e.r60s
                    val indices = listOfNotNull(
                        e.pi?.let { "PI " + numero(it, 2) },
                        e.dar?.let { "DAR " + numero(it, 2) },
                        e.condicao.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")
                    "${formatoDia.format(Date(e.dataHora))} — ${numero(valor)} MΩ" to indices
                }
            )
            val pontos = historico.sortedBy { it.dataHora }
                .map { Megohmetro.Ponto(it.dataHora, it.puntualCorrigido ?: it.r60s) }
            Megohmetro.tendencia(pontos)?.let { b.paragrafo(it.texto) }
            b.paragrafo(
                "Observação: pela IEEE 43 a tendência das leituras corrigidas pesa mais que o " +
                    "valor absoluto de um ensaio isolado."
            )
        }

        b.secao("Critérios de avaliação (IEEE 43)")
        b.tabela(
            listOf(
                "PI menor que 1,0" to "Perigoso",
                "PI de 1,0 a 2,0" to "Pobre",
                "PI de 2,0 a 4,0" to "Bom",
                "PI acima de 4,0" to "Excelente",
                "DAR menor que 1,25" to "Inadequado",
                "DAR de 1,25 a 1,6" to "Aceitável",
                "DAR acima de 1,6" to "Excelente",
            )
        )
        b.paragrafo(Megohmetro.CORRECAO_FORMULA + " — a resistência de isolamento cai pela metade a cada 10 °C de aumento de temperatura.")

        b.secao("Avisos técnicos e de segurança")
        b.paragrafo(
            (Megohmetro.PASSO_A_PASSO_GERAL.joinToString("\n") { "• $it" }) + "\n" +
                "• " + br.com.refrigeracaopro.data.Avisos.CALCULO_AUXILIAR
        )

        b.assinaturas(
            listOf(
                (context.nomeTecnico.ifBlank { "Técnico" } +
                    context.registroTecnico.let { if (it.isNotBlank()) " — $it" else "" }) to "",
            )
        )

        b.fecharPagina()
        val nome = ensaio.numero.ifBlank { "ensaio-isolacao-${ensaio.id}" }
        val arquivo = File(Arquivos.pastaPdfs(context), "$nome.pdf")
        arquivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return arquivo
    }

    /** Número em pt-BR, sem casas desnecessárias. */
    private fun numero(valor: Double, casas: Int = 2): String {
        val texto = when {
            casas == 0 -> "%.0f".format(valor)
            valor >= 1000 -> "%.0f".format(valor)
            valor >= 10 && casas == 2 -> "%.1f".format(valor)
            else -> "%.${casas}f".format(valor)
        }
        return texto.replace('.', ',')
    }

    private fun String.comUnidade(unidade: String): String =
        if (isBlank()) "" else "$this $unidade"
}
