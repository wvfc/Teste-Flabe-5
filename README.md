# Refrigeração Pro ❄️

Aplicativo Android profissional para **assistência técnica em refrigeração**,
desenvolvido em **Kotlin** com **Jetpack Compose**, arquitetura **MVVM** e banco
de dados local **Room**. Funciona **offline** para todos os cadastros, ordens de
serviço, relatórios, agendamentos, consultas e cálculos. A integração com IA
(OpenAI) é opcional e só funciona com internet e chave cadastrada pelo usuário.

> Compatível com **Android 8.0 (API 26)** ou superior.

---

## ✨ Funcionalidades

| Módulo | Descrição |
|---|---|
| **Login local** | Usuário/senha offline, criação de administrador no 1º acesso e redefinição de senha por pergunta de segurança. |
| **Clientes** | CRUD completo, pesquisa, equipamentos vinculados e histórico de atendimentos. |
| **Equipamentos** | Tipos de refrigeração, fluido, dados elétricos, local e **fotos**. |
| **Serviços** | Catálogo de serviços com valor e tempo estimado (10 serviços pré-cadastrados). |
| **Ordens de Serviço** | Numeração automática, status, fotos (antes/durante/depois), **assinatura na tela**, **PDF** e compartilhamento. |
| **Relatórios técnicos** | Medições elétricas, de pressão e temperatura, **superaquecimento/subresfriamento calculados automaticamente**, fotos, assinaturas e **PDF** profissional. |
| **Agendamentos** | Manutenção preventiva com periodicidade, **notificações locais**, histórico e geração de OS. |
| **Consulta de gases** | 13 fluidos (R22, R134a, R404A, R407C, R410A, R507, R32, R290, R600a, R1234yf, R1234ze, CO₂/R744, Amônia/R717) com dados técnicos. |
| **Cálculos** | Superaquecimento e subresfriamento via tabela P/T interna aproximada. |
| **Assistente IA** | Chat técnico e geração de conclusão de relatório via OpenAI. |
| **Configurações** | Dados da empresa/técnico, logo, chave OpenAI (criptografada), modelo de IA, teste de conexão e **backup/restauração**. |

---

## 🏗️ Arquitetura

```
app/src/main/java/br/com/refrigeracaopro/
├── data/            # Room: entidades, DAOs, banco, tabela de gases, tabela P/T, Prefs
├── viewmodel/       # ViewModels (MVVM) com StateFlow
├── ui/
│   ├── theme/       # Tema (azul/branco/cinza/verde, claro e escuro)
│   ├── components/  # Componentes reutilizáveis (campos, fotos, assinatura)
│   └── screens/     # Telas Compose (login, dashboard e módulos)
├── ia/              # Cliente OpenAI (OkHttp) e montagem de contexto técnico
├── pdf/             # Geração de PDF nativa (OS e relatório)
├── util/            # Arquivos, segurança (hash), notificações, backup
├── RefrigeracaoProApp.kt   # Application
└── MainActivity.kt         # Activity única + Navigation Compose
```

**Principais tecnologias:** Jetpack Compose · Material 3 · Room · Navigation
Compose · EncryptedSharedPreferences · OkHttp · Coil · PdfDocument nativo.

---

## 🔐 Segurança

- A **chave da OpenAI** é guardada com **EncryptedSharedPreferences (AES-256)** e
  **nunca** fica no código-fonte — é cadastrada pelo próprio usuário.
- Senhas do login local são salvas como **hash SHA-256 + salt**.
- Todos os dados ficam no armazenamento interno do app; o backup é **manual**.

---

## 📲 Como compilar e instalar o APK

### Opção A — Pelo GitHub Actions (recomendado, sem instalar nada)

1. Faça push deste repositório para o GitHub.
2. O workflow **`.github/workflows/build-apk.yml`** roda automaticamente.
3. Abra a aba **Actions** → execução mais recente → seção **Artifacts**.
4. Baixe **`refrigeracao-pro-debug`**, descompacte e instale o `app-debug.apk`
   no celular (ative "Fontes desconhecidas").

Você também pode disparar o build manualmente em **Actions → Build APK → Run workflow**.

### Opção B — Localmente

Pré-requisitos: **JDK 17** e **Android SDK** (platform 34, build-tools 34).

```bash
# 1. Aponte o SDK (crie o arquivo local.properties)
echo "sdk.dir=/caminho/para/Android/Sdk" > local.properties

# 2. Gere o APK de debug
./gradlew assembleDebug

# 3. O APK estará em:
#    app/build/outputs/apk/debug/app-debug.apk

# 4. Instale em um dispositivo conectado (opcional)
./gradlew installDebug
```

### Opção C — Android Studio

Abra a pasta do projeto no **Android Studio** (Hedgehog ou mais recente),
aguarde o sync do Gradle e use **Run ▶** ou **Build → Build APK(s)**.

---

## 🧮 Sobre os cálculos técnicos

As tabelas P/T e os dados de gases são **aproximações para referência de campo**.
A curva de saturação de cada fluido é estimada pela correlação de
Clausius-Clapeyron ajustada pelos pontos de ebulição e crítico.

> ⚠️ **Sempre confirme com o fabricante do equipamento e a tabela P/T oficial do
> fluido antes de qualquer intervenção.**

- **Superaquecimento** = T. da linha de sucção − T. de evaporação saturada
- **Subresfriamento** = T. de condensação saturada − T. da linha de líquido

---

## 🤖 Configurando a IA (opcional)

1. Obtenha uma chave em <https://platform.openai.com/api-keys>.
2. No app: **Configurações → Assistente IA**, cole a chave, escolha o modelo e
   toque em **Testar conexão**.
3. Use o **Assistente IA** para perguntas técnicas ou gere a **conclusão** de um
   relatório automaticamente a partir das medições.

A IA só é ativada com internet disponível e chave válida; todo o restante do app
funciona offline.

---

## 📄 Licença

Projeto entregue como base para customização pelo cliente.
