package br.com.refrigeracaopro

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.refrigeracaopro.data.Prefs.sessaoAtiva
import br.com.refrigeracaopro.ui.screens.AgendamentosScreen
import br.com.refrigeracaopro.ui.screens.AssistenteIAScreen
import br.com.refrigeracaopro.ui.screens.CadastrosScreen
import br.com.refrigeracaopro.ui.screens.CalculosScreen
import br.com.refrigeracaopro.ui.screens.CalculadoraGraxaScreen
import br.com.refrigeracaopro.ui.screens.ClienteFormScreen
import br.com.refrigeracaopro.ui.screens.ClientesScreen
import br.com.refrigeracaopro.ui.screens.ComparacaoScreen
import br.com.refrigeracaopro.ui.screens.CompressoresScreen
import br.com.refrigeracaopro.ui.screens.ConfiguracoesScreen
import br.com.refrigeracaopro.ui.screens.ConversorScreen
import br.com.refrigeracaopro.ui.screens.DadosCompressoresScreen
import br.com.refrigeracaopro.ui.screens.DadosIngersollScreen
import br.com.refrigeracaopro.ui.screens.DashboardScreen
import br.com.refrigeracaopro.ui.screens.FerramentasScreen
import br.com.refrigeracaopro.ui.screens.GestaoScreen
import br.com.refrigeracaopro.ui.screens.ManuaisScreen
import br.com.refrigeracaopro.ui.screens.MegohmetroScreen
import br.com.refrigeracaopro.ui.screens.MarcasCompressoresScreen
import br.com.refrigeracaopro.ui.screens.EquipamentoFormScreen
import br.com.refrigeracaopro.ui.screens.EquipamentosScreen
import br.com.refrigeracaopro.ui.screens.GasesScreen
import br.com.refrigeracaopro.ui.screens.LoginScreen
import br.com.refrigeracaopro.ui.screens.OrdemServicoFormScreen
import br.com.refrigeracaopro.ui.screens.OrdensServicoScreen
import br.com.refrigeracaopro.ui.screens.EditorIsoScreen
import br.com.refrigeracaopro.ui.screens.ProjetoCadastroScreen
import br.com.refrigeracaopro.ui.screens.ProjetosScreen
import br.com.refrigeracaopro.ui.screens.RelatorioArComprimidoFormScreen
import br.com.refrigeracaopro.ui.screens.RelatorioFormScreen
import br.com.refrigeracaopro.ui.screens.RelatoriosScreen
import br.com.refrigeracaopro.ui.screens.SenhasIHMScreen
import br.com.refrigeracaopro.ui.screens.ServicosScreen
import br.com.refrigeracaopro.ui.theme.RefrigeracaoProTheme

/**
 * Activity única: toda a navegação é feita com Navigation Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pede permissão de notificação (Android 13+) para os lembretes de manutenção
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            RefrigeracaoProTheme {
                // Sessão persistente: se já houve login, não pede de novo ao reabrir
                var logado by rememberSaveable { mutableStateOf(this.sessaoAtiva) }
                val nav = rememberNavController()

                if (!logado) {
                    LoginScreen(aoEntrar = {
                        this.sessaoAtiva = true
                        logado = true
                    })
                } else {
                    NavHost(navController = nav, startDestination = "dashboard") {
                        composable("dashboard") { DashboardScreen(nav) }
                        composable("cadastros") { CadastrosScreen(nav) }
                        composable("ferramentas") { FerramentasScreen(nav) }

                        composable("clientes") { ClientesScreen(nav) }
                        composable(
                            "clientes/form?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
                        ) { ClienteFormScreen(nav, it.arguments?.getLong("id") ?: 0L) }

                        composable("equipamentos") { EquipamentosScreen(nav) }
                        composable(
                            "equipamentos/form?id={id}&clienteId={clienteId}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("clienteId") { type = NavType.LongType; defaultValue = 0L },
                            )
                        ) {
                            EquipamentoFormScreen(
                                nav,
                                it.arguments?.getLong("id") ?: 0L,
                                it.arguments?.getLong("clienteId") ?: 0L
                            )
                        }

                        composable("servicos") { ServicosScreen(nav) }

                        composable("ordens") { OrdensServicoScreen(nav) }
                        composable(
                            "ordens/form?id={id}&agendamentoCliente={agendamentoCliente}&agendamentoEquip={agendamentoEquip}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("agendamentoCliente") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("agendamentoEquip") { type = NavType.LongType; defaultValue = 0L },
                            )
                        ) {
                            OrdemServicoFormScreen(
                                nav,
                                it.arguments?.getLong("id") ?: 0L,
                                it.arguments?.getLong("agendamentoCliente") ?: 0L,
                                it.arguments?.getLong("agendamentoEquip") ?: 0L,
                            )
                        }

                        composable("relatorios") { RelatoriosScreen(nav) }
                        composable(
                            "relatorios/form?id={id}&osId={osId}&tipo={tipo}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("osId") { type = NavType.LongType; defaultValue = 0L },
                                navArgument("tipo") { type = NavType.StringType; defaultValue = "refrigeracao" },
                            )
                        ) {
                            RelatorioFormScreen(
                                nav,
                                it.arguments?.getLong("id") ?: 0L,
                                it.arguments?.getLong("osId") ?: 0L,
                                it.arguments?.getString("tipo") ?: "refrigeracao",
                            )
                        }
                        composable(
                            "relatorios/arcomp?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
                        ) {
                            RelatorioArComprimidoFormScreen(nav, it.arguments?.getLong("id") ?: 0L)
                        }

                        composable("agendamentos") { AgendamentosScreen(nav) }
                        composable("gases") { GasesScreen(nav) }
                        composable("calculos") { CalculosScreen(nav) }
                        composable("compressores") { CompressoresScreen(nav) }
                        composable("comparacao") { ComparacaoScreen(nav) }
                        composable("manuais") { ManuaisScreen(nav) }
                        composable("gestao") { GestaoScreen(nav) }
                        composable("conversor") { ConversorScreen(nav) }
                        composable("graxa") { CalculadoraGraxaScreen(nav) }
                        composable("megohmetro") { MegohmetroScreen(nav) }
                        composable("compressores_marcas") { MarcasCompressoresScreen(nav) }
                        composable("dados_compressores") { DadosCompressoresScreen(nav) }
                        composable("dados_ingersoll") { DadosIngersollScreen(nav) }
                        composable("senhas_ihm") { SenhasIHMScreen(nav) }

                        composable("projetos") { ProjetosScreen(nav) }
                        composable(
                            "projetos/cadastro?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
                        ) { ProjetoCadastroScreen(nav, it.arguments?.getLong("id") ?: 0L) }
                        composable(
                            "projetos/editor?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
                        ) { EditorIsoScreen(nav, it.arguments?.getLong("id") ?: 0L) }

                        composable("assistente") { AssistenteIAScreen(nav) }
                        composable("configuracoes") { ConfiguracoesScreen(nav) }
                    }
                }
            }
        }
    }
}
