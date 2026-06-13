package br.com.refrigeracaopro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Card de aviso técnico/segurança (texto único). */
@Composable
fun AvisoCard(texto: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(
        modifier = modifier.padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Card de aviso com lista de itens (boas práticas de segurança). */
@Composable
fun AvisoListaCard(titulo: String, itens: List<String>, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(
        modifier = modifier.padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(6.dp))
            itens.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
        }
    }
}
