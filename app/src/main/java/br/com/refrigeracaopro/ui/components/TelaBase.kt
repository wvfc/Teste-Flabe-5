package br.com.refrigeracaopro.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController

/** Scaffold padrão das telas internas: barra superior azul, voltar e FAB opcional. */
@Composable
fun TelaBase(
    nav: NavController,
    titulo: String,
    aoAdicionar: (() -> Unit)? = null,
    snackbar: SnackbarHostState? = null,
    acoes: @Composable () -> Unit = {},
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = { acoes() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                )
            )
        },
        floatingActionButton = {
            if (aoAdicionar != null) {
                FloatingActionButton(
                    onClick = aoAdicionar,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ) { Icon(Icons.Default.Add, "Adicionar") }
            }
        },
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        content = conteudo,
    )
}
