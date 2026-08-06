package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

data class Modulo(val titulo: String, val icone: ImageVector, val rota: String)

/** Dashboard inicial com os grupos principais. */
@Composable
fun DashboardScreen(nav: NavController) {
    val modulos = listOf(
        Modulo("Cadastros", Icons.Default.FolderShared, "cadastros"),
        Modulo("Ordens de Serviço", Icons.Default.Engineering, "ordens"),
        Modulo("Relatórios", Icons.Default.Description, "relatorios"),
        Modulo("Agendamentos", Icons.Default.CalendarMonth, "agendamentos"),
        Modulo("Ferramentas", Icons.Default.Build, "ferramentas"),
        Modulo("Gestão Financeira", Icons.Default.MonetizationOn, "gestao"),
        Modulo("Assistente IA", Icons.Default.SmartToy, "assistente"),
        Modulo("Configurações", Icons.Default.Settings, "configuracoes"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão Pro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                )
            )
        }
    ) { padding ->
        GradeModulos(modulos, nav, padding)
    }
}

/** Sub-dashboard "Cadastros": clientes, equipamentos e serviços. */
@Composable
fun CadastrosScreen(nav: NavController) {
    val modulos = listOf(
        Modulo("Clientes", Icons.Default.People, "clientes"),
        Modulo("Equipamentos", Icons.Default.Kitchen, "equipamentos"),
        Modulo("Serviços", Icons.Default.Handyman, "servicos"),
    )
    TelaBase(nav, "Cadastros") { padding -> GradeModulos(modulos, nav, padding) }
}

/** Sub-dashboard "Ferramentas": gases, cálculos, compressores, comparação, conversor e manuais. */
@Composable
fun FerramentasScreen(nav: NavController) {
    val modulos = listOf(
        Modulo("Consulta de Gases", Icons.Default.Science, "gases"),
        Modulo("Cálculos", Icons.Default.Calculate, "calculos"),
        Modulo("Dados de Compressores", Icons.Default.PrecisionManufacturing, "compressores_marcas"),
        Modulo("Conversor de Unidades", Icons.Default.SwapHoriz, "conversor"),
        Modulo("Calculadora de Graxa", Icons.Default.Opacity, "graxa"),
        Modulo("Diagnóstico de Megômetro", Icons.Default.ElectricBolt, "megohmetro"),
        Modulo("Análise Termográfica", Icons.Default.Thermostat, "termografia"),
        Modulo("Senhas IHM", Icons.Default.Password, "senhas_ihm"),
        Modulo("Buscar Manual", Icons.AutoMirrored.Filled.MenuBook, "manuais"),
    )
    TelaBase(nav, "Ferramentas") { padding -> GradeModulos(modulos, nav, padding) }
}

/**
 * Marca de compressor. Pode abrir uma tela interna de dados (rota) ou uma
 * pasta do OneDrive (url) — neste caso abre numa aba dentro do app.
 */
data class MarcaCompressor(
    val nome: String,
    val logo: Int? = null,
    val icone: ImageVector? = null,
    val rota: String? = null,
    val url: String? = null,
)

/** Sub-dashboard "Dados de Compressores": marcas (Atlas, Ingersoll e futuras). */
@Composable
fun MarcasCompressoresScreen(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val marcas = listOf(
        MarcaCompressor("Atlas Copco", logo = br.com.refrigeracaopro.R.drawable.logo_atlas_copco, rota = "dados_compressores"),
        MarcaCompressor("Ingersoll Rand", logo = br.com.refrigeracaopro.R.drawable.logo_ingersoll_rand, rota = "dados_ingersoll"),
        MarcaCompressor("Chicago Pneumatic", icone = Icons.Default.FolderOpen, url = "https://1drv.ms/f/c/f1f044930beab71d/IgDsWXxPynDMQrdAxBFxrXnYAQJrxzEG0N12MNi9VrbhNC4"),
        MarcaCompressor("Kaeser", icone = Icons.Default.FolderOpen, url = "https://1drv.ms/f/c/f1f044930beab71d/IgCE7FrnRBmXTIWG3G80fro_AdXFyQfM7F-VXuMfrrfgeok"),
    )
    TelaBase(nav, "Dados de Compressores") { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(marcas) { marca ->
                Card(
                    onClick = {
                        when {
                            marca.rota != null -> nav.navigate(marca.rota)
                            marca.url != null -> abrirPasta(context, marca.url)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (marca.logo != null) {
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier.size(96.dp),
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(marca.logo),
                                    contentDescription = marca.nome,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                )
                            }
                        } else {
                            Icon(marca.icone ?: Icons.Default.FolderOpen, null, tint = Verde, modifier = Modifier.size(56.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            marca.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        if (marca.url != null) {
                            Text("Pasta OneDrive", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Abre uma pasta/URL numa aba dentro do app (Custom Tab); cai no navegador se preciso. */
private fun abrirPasta(context: android.content.Context, url: String) {
    val uri = android.net.Uri.parse(url)
    runCatching {
        androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }.onFailure {
        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
            .onFailure { android.widget.Toast.makeText(context, "Não foi possível abrir a pasta.", android.widget.Toast.LENGTH_SHORT).show() }
    }
}

/** Grade reutilizável de cartões grandes (uso em campo). */
@Composable
fun GradeModulos(modulos: List<Modulo>, nav: NavController, padding: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(modulos) { modulo ->
            Card(
                onClick = { nav.navigate(modulo.rota) },
                modifier = Modifier.fillMaxWidth().aspectRatio(1.25f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(modulo.icone, null, tint = Verde, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        modulo.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
