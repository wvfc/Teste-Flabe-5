package br.com.refrigeracaopro

import android.app.Application
import br.com.refrigeracaopro.data.AppDatabase
import br.com.refrigeracaopro.util.AgendadorBackup
import br.com.refrigeracaopro.util.Notificacoes
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefrigeracaoProApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notificacoes.criarCanal(this)
        // Inicializa o PdfBox (extração de texto de PDFs anexados à IA)
        PDFBoxResourceLoader.init(applicationContext)
        // Garante que o agendamento de backup automático reflita as preferências
        AgendadorBackup.aplicar(this)
        // Pré-cadastra o catálogo de serviços no primeiro uso
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(this@RefrigeracaoProApp).servicoDao()
            if (dao.contar() == 0) dao.inserirTodos(AppDatabase.SERVICOS_PADRAO)
        }
    }
}
