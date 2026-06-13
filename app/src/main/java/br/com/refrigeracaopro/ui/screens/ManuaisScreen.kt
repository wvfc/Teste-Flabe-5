package br.com.refrigeracaopro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.ui.components.AvisoCard
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.EquipamentosViewModel
import java.net.URLEncoder

/**
 * Busca de manuais técnicos. Não depende de servidor próprio: monta consultas
 * prontas e abre no navegador, priorizando fontes oficiais. Permite anexar um
 * PDF baixado ao cadastro do equipamento (para consulta offline).
 */
@Composable
fun ManuaisScreen(nav: NavController, vm: EquipamentosViewModel = viewModel()) {
    val context = LocalContext.current
    val equipamentos by vm.equipamentos.collectAsState()

    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(EquipamentosViewModel.TIPOS.first()) }
    var equipamentoId by remember { mutableStateOf(0L) }

    fun abrirBusca(template: String) {
        if (marca.isBlank() && modelo.isBlank()) {
            Toast.makeText(context, "Informe ao menos marca ou modelo.", Toast.LENGTH_SHORT).show()
            return
        }
        val consulta = template
            .replace("[tipo]", tipo)
            .replace("[marca]", marca)
            .replace("[modelo]", modelo)
            .trim()
        val url = "https://www.google.com/search?q=" + URLEncoder.encode(consulta, "UTF-8")
        Arquivos.abrirUrl(context, url)
    }

    val anexarPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (equipamentoId <= 0) {
            Toast.makeText(context, "Selecione um equipamento para anexar.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val caminho = Arquivos.copiarParaManuais(context, uri, "$marca-$modelo")
        val equip = equipamentos.find { it.id == equipamentoId }
        if (caminho != null && equip != null) {
            val novos = Arquivos.textoParaLista(equip.manuais) + caminho
            vm.salvar(equip.copy(manuais = Arquivos.listaParaTexto(novos)))
            Toast.makeText(context, "Manual anexado ao equipamento.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Não foi possível anexar o manual.", Toast.LENGTH_LONG).show()
        }
    }

    TelaBase(nav, "Buscar manual técnico") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            CampoTexto(marca, { marca = it }, "Marca")
            CampoTexto(modelo, { modelo = it }, "Modelo")
            SeletorOpcoes("Tipo de equipamento", EquipamentosViewModel.TIPOS, tipo, { tipo = it })

            TituloSecao("Pesquisar na internet")
            Text("Abre o navegador com a busca pronta, priorizando o site do fabricante.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            BotaoBusca("Manual técnico (PDF)", "manual técnico [tipo] [marca] [modelo] PDF", ::abrirBusca)
            BotaoBusca("Datasheet (inglês)", "datasheet [marca] [modelo] refrigeration PDF", ::abrirBusca)
            BotaoBusca("Manual no site oficial", "manual [tipo] [marca] [modelo] site oficial fabricante PDF", ::abrirBusca)

            AvisoCard(
                "Atenção: nem todo resultado vem de fonte oficial. Prefira o site do fabricante e " +
                    "desconfie de páginas que pedem cadastro ou cobram pelo manual."
            )

            TituloSecao("Anexar manual ao equipamento")
            SeletorOpcoes(
                "Equipamento", equipamentos.map { "${it.tipo} ${it.marca} ${it.modelo}".trim() },
                equipamentos.find { it.id == equipamentoId }?.let { "${it.tipo} ${it.marca} ${it.modelo}".trim() } ?: "",
                aoSelecionar = { texto ->
                    equipamentoId = equipamentos.find { e -> "${e.tipo} ${e.marca} ${e.modelo}".trim() == texto }?.id ?: 0L
                }
            )
            Text("Baixe o PDF no navegador e depois selecione-o aqui para guardar offline no equipamento.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { anexarPdf.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp),
                enabled = equipamentoId > 0
            ) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Anexar PDF baixado")
            }

            // Lista manuais já anexados ao equipamento selecionado
            val equip = equipamentos.find { it.id == equipamentoId }
            val manuais = equip?.let { Arquivos.textoParaLista(it.manuais) } ?: emptyList()
            if (manuais.isNotEmpty()) {
                TituloSecao("Manuais anexados (${manuais.size})")
                manuais.forEach { caminho ->
                    Card(
                        onClick = { Arquivos.abrirPdf(context, java.io.File(caminho)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(Modifier.padding(10.dp)) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(java.io.File(caminho).name.substringAfter("-"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BotaoBusca(rotulo: String, template: String, aoBuscar: (String) -> Unit) {
    OutlinedButton(
        onClick = { aoBuscar(template) },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
    ) {
        Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(rotulo)
    }
}
