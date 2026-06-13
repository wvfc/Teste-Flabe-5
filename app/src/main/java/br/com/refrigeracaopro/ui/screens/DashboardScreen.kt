package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
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
import br.com.refrigeracaopro.ui.theme.Verde

private data class Modulo(val titulo: String, val icone: ImageVector, val rota: String)

/** Dashboard inicial com atalhos grandes para todos os módulos (uso em campo). */
@Composable
fun DashboardScreen(nav: NavController) {
    val modulos = listOf(
        Modulo("Clientes", Icons.Default.People, "clientes"),
        Modulo("Equipamentos", Icons.Default.Kitchen, "equipamentos"),
        Modulo("Ordens de Serviço", Icons.Default.Engineering, "ordens"),
        Modulo("Relatórios", Icons.Default.Description, "relatorios"),
        Modulo("Agendamentos", Icons.Default.CalendarMonth, "agendamentos"),
        Modulo("Serviços", Icons.Default.Handyman, "servicos"),
        Modulo("Consulta de Gases", Icons.Default.Science, "gases"),
        Modulo("Cálculos", Icons.Default.Calculate, "calculos"),
        Modulo("Assistente IA", Icons.Default.SmartToy, "assistente"),
        Modulo("Configurações", Icons.Default.Settings, "configuracoes"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refrigeração Pro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(modulos) { modulo ->
                Card(
                    onClick = { nav.navigate(modulo.rota) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp),
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
}
