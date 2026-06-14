package br.com.refrigeracaopro.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Lancamento
import br.com.refrigeracaopro.data.TipoLancamento
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde
import br.com.refrigeracaopro.viewmodel.GestaoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Gestão financeira: receitas e despesas por mês, com saldo, importação de OS
 * concluídas e CRUD de lançamentos.
 */
@Composable
fun GestaoScreen(nav: NavController, vm: GestaoViewModel = viewModel()) {
    val context = LocalContext.current
    val todos by vm.lancamentos.collectAsState()
    val moeda = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val formatoMes = remember { SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR")) }
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    // Mês selecionado (início e fim em millis)
    var mes by remember { mutableStateOf(Calendar.getInstance()) }
    val inicioMes = remember(mes) { (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); zerar() }.timeInMillis }
    val fimMes = remember(mes) {
        (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); fimDoDia() }.timeInMillis
    }

    val doMes = todos.filter { it.data in inicioMes..fimMes }
    val receita = doMes.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valor }
    val despesa = doMes.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.valor }
    val saldo = receita - despesa

    // Saldo TOTAL acumulado (todos os meses)
    val receitaTotal = todos.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valor }
    val despesaTotal = todos.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.valor }
    val saldoTotal = receitaTotal - despesaTotal

    var editar by remember { mutableStateOf<Lancamento?>(null) }
    var novo by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf<Lancamento?>(null) }

    TelaBase(nav, "Gestão financeira", aoAdicionar = { novo = true }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Seletor de mês
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mes = (mes.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                    Icon(Icons.Default.ChevronLeft, "Mês anterior")
                }
                Text(formatoMes.format(mes.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { mes = (mes.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) {
                    Icon(Icons.Default.ChevronRight, "Próximo mês")
                }
            }

            // Cartões de totais
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardTotal("Receitas", moeda.format(receita), Verde, Modifier.weight(1f))
                CardTotal("Despesas", moeda.format(despesa), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (saldo >= 0) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Saldo do mês", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(moeda.format(saldo), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                }
            }

            // Saldo total acumulado (soma de todos os meses)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Saldo total acumulado", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Receitas ${moeda.format(receitaTotal)} • Despesas ${moeda.format(despesaTotal)}",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                    Text(moeda.format(saldoTotal), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }

            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.importarReceitasDeOS { n ->
                        Toast.makeText(context, if (n > 0) "$n receita(s) importada(s) de OS concluídas." else "Nenhuma OS nova para importar.", Toast.LENGTH_LONG).show()
                    } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Importar OS")
                }
                OutlinedButton(
                    onClick = {
                        val titulo = formatoMes.format(mes.time).replaceFirstChar { it.uppercase() }
                        val pdf = PdfGenerator.gerarExtrato(context, titulo, doMes, receita, despesa, saldo)
                        Arquivos.compartilhar(context, pdf, "application/pdf", "Extrato $titulo")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = doMes.isNotEmpty()
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("PDF do mês")
                }
            }

            Text("Lançamentos do mês (${doMes.size})", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(doMes, key = { it.id }) { l ->
                    val receitaItem = l.tipo == TipoLancamento.RECEITA
                    Card(
                        onClick = { editar = l },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (receitaItem) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                null, tint = if (receitaItem) Verde else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(l.descricao, fontWeight = FontWeight.SemiBold)
                                Text(listOf(l.categoria, formatoData.format(Date(l.data))).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text((if (receitaItem) "+" else "−") + moeda.format(l.valor),
                                fontWeight = FontWeight.Bold,
                                color = if (receitaItem) Verde else MaterialTheme.colorScheme.error)
                            IconButton(onClick = { excluir = l }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (novo || editar != null) {
        DialogoLancamento(
            lancamento = editar,
            aoSalvar = { vm.salvar(it); novo = false; editar = null },
            aoFechar = { novo = false; editar = null }
        )
    }
    excluir?.let { l ->
        ConfirmarExclusao("Excluir o lançamento \"${l.descricao}\"?",
            aoConfirmar = { vm.excluir(l); excluir = null }, aoCancelar = { excluir = null })
    }
}

@Composable
private fun CardTotal(titulo: String, valor: String, cor: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

@Composable
private fun DialogoLancamento(lancamento: Lancamento?, aoSalvar: (Lancamento) -> Unit, aoFechar: () -> Unit) {
    val context = LocalContext.current
    var tipo by remember { mutableStateOf(lancamento?.tipo ?: TipoLancamento.RECEITA) }
    var descricao by remember { mutableStateOf(lancamento?.descricao ?: "") }
    var categoria by remember { mutableStateOf(lancamento?.categoria ?: "") }
    var valor by remember { mutableStateOf(lancamento?.valor?.takeIf { it > 0 }?.toString() ?: "") }
    var forma by remember { mutableStateOf(lancamento?.formaPagamento ?: "") }
    var data by remember { mutableStateOf(lancamento?.data ?: System.currentTimeMillis()) }
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val categorias = if (tipo == TipoLancamento.RECEITA) TipoLancamento.CATEGORIAS_RECEITA else TipoLancamento.CATEGORIAS_DESPESA

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (lancamento == null) "Novo lançamento" else "Editar lançamento") },
        text = {
            Column {
                SeletorOpcoes("Tipo", TipoLancamento.TODOS, tipo, { tipo = it; categoria = "" })
                CampoTexto(descricao, { descricao = it }, "Descrição *")
                SeletorOpcoes("Categoria", categorias, categoria, { categoria = it })
                CampoTexto(valor, { valor = it }, "Valor (R$) *")
                CampoTexto(forma, { forma = it }, "Forma de pagamento")
                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = data }
                        DatePickerDialog(context, { _, a, m, d ->
                            c.set(a, m, d); data = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Data: ${formatoData.format(Date(data))}") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = valor.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (descricao.isBlank() || v <= 0) return@TextButton
                    aoSalvar(
                        (lancamento ?: Lancamento(descricao = "")).copy(
                            tipo = tipo, descricao = descricao, categoria = categoria,
                            valor = v, data = data, formaPagamento = forma,
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

// Helpers de Calendar
private fun Calendar.zerar() { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
private fun Calendar.fimDoDia() { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
