package br.com.refrigeracaopro.ia

import br.com.refrigeracaopro.data.Equipamento
import br.com.refrigeracaopro.data.Relatorio

/**
 * Monta o contexto técnico textual enviado à IA, conforme exigido pelo escopo:
 * tipo de equipamento, fluido, pressões, temperaturas, superaquecimento,
 * subresfriamento, corrente, defeito informado e serviços realizados.
 */
object IAContexto {

    fun deRelatorio(rel: Relatorio, equip: Equipamento?): String = buildString {
        appendLinha("Tipo de equipamento", equip?.tipo)
        appendLinha("Marca/Modelo", listOfNotNull(equip?.marca, equip?.modelo).filter { it.isNotBlank() }.joinToString(" "))
        appendLinha("Fluido refrigerante", rel.fluido.ifBlank { equip?.fluido })
        appendLinha("Defeito/motivo informado", rel.motivoVisita)
        appendLinha("Diagnóstico parcial", rel.diagnostico)
        appendLinha("Pressão de sucção", rel.pressaoSuccao, "bar")
        appendLinha("Pressão de descarga", rel.pressaoDescarga, "bar")
        appendLinha("Temp. linha de sucção", rel.tempLinhaSuccao, "°C")
        appendLinha("Temp. linha de líquido", rel.tempLinhaLiquido, "°C")
        appendLinha("Temp. evaporação", rel.tempEvaporacao, "°C")
        appendLinha("Temp. condensação", rel.tempCondensacao, "°C")
        appendLinha("Temp. ambiente", rel.tempAmbiente, "°C")
        appendLinha("Temp. interna", rel.tempInterna, "°C")
        appendLinha("Superaquecimento", rel.superaquecimento, "K")
        appendLinha("Subresfriamento", rel.subresfriamento, "K")
        appendLinha("Corrente elétrica", rel.correnteEletrica, "A")
        appendLinha("Tensão elétrica", rel.tensaoEletrica, "V")
        appendLinha("Serviços realizados", rel.servicosRealizados)
        appendLinha("Peças substituídas", rel.pecasSubstituidas)
    }

    private fun StringBuilder.appendLinha(rotulo: String, valor: String?, unidade: String = "") {
        if (!valor.isNullOrBlank()) {
            append("- ").append(rotulo).append(": ").append(valor)
            if (unidade.isNotBlank()) append(" ").append(unidade)
            append("\n")
        }
    }
}
