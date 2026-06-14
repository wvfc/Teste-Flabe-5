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
| **Motores/Compressores** | Pesquisa de compressores e **sugestão de equivalentes**, com alerta de diferenças críticas (fluido, óleo, tensão, fase, capacidade, aplicação). |
| **Comparar Componentes** | Comparação lado a lado de compressores, condensadoras, evaporadores, ventiladores, controladores, válvulas, pressostatos e filtros. |
| **Cálculo de capacitor** | Estimativa de capacitor permanente e de partida (µF) e tensão mínima, por potência/tensão/aplicação. |
| **Cálculo de carga de fluido** | Estimativa de carga adicional por comprimento de linha + registro de peso do cilindro (carga por balança). |
| **Buscar Manual** | Busca de manuais técnicos no navegador (priorizando fabricante) e **anexo de PDF** ao equipamento para consulta offline. |
| **Assistente IA** | Chat técnico e geração de conclusão/diagnóstico, fundamentado na **base técnica interna (JSON offline)**, com formato estruturado (diagnóstico, causas, testes, riscos, correção, segurança, grau de confiança). |
| **Configurações** | Dados da empresa/técnico, logo, chave OpenAI (criptografada), modelo de IA, teste de conexão e **backup/restauração**. |

| **Gestão Financeira** | Receitas e despesas por mês, com saldo, categorias, e **importação automática de receita das OS concluídas**. |
| **Conversor de Unidades** | Conversão entre temperatura, pressão, comprimento, massa, volume, área, velocidade, potência, energia, vazão, tempo, torque e ângulo. |

### Pressões em psi
Todas as pressões do app (cálculos de superaquecimento/subresfriamento, campos do relatório, PDF e tabela de gases) são tratadas em **psi**.

### Assistente IA
Aceita **entrada de imagens** (visão — ex.: foto da plaqueta do compressor) e **arquivos**. O texto de **PDFs** é extraído no próprio aparelho (PdfBox-Android) e enviado à IA, permitindo resumir/analisar o documento; PDFs digitalizados (imagem) avisam que não há texto extraível. As respostas vêm em **texto puro** (sem marcações Markdown como `**`) e no formato estruturado de diagnóstico.

### Fotos
As fotos (equipamentos, OS e relatórios) são adicionadas **pela galeria**. As imagens são copiadas para o armazenamento interno, têm a rotação corrigida (EXIF) e são **comprimidas** antes de salvar/exportar.

### Organização (dashboard)
O painel principal agrupa os módulos: **Cadastros** (clientes, equipamentos, serviços), Ordens de Serviço, **Relatórios**, Agendamentos, **Ferramentas** (consulta de gases, cálculos, motores/compressores, comparar componentes, conversor de unidades, buscar manual), Gestão Financeira, Assistente IA e Configurações.

### Tipos de relatório
Ao criar um relatório, escolhe-se entre três tipos:
1. **Técnico de refrigeração** — completo (pressões, temperaturas, superaquecimento/subresfriamento, diagnóstico IA).
2. **Compressor de ar comprimido** — checklist de inspeção (horímetro, óleo, mecânica, elétrica, pressões e temperaturas), baseado no modelo do fabricante.
3. **Geral (simples)** — dados, serviços, recomendações, fotos e assinaturas.

### Clientes — importar/exportar
Na tela de Clientes há botões para **exportar** todos os clientes em **CSV** (compartilhável) e **importar** clientes de um arquivo CSV.

### Login e sessão
O acesso principal é **direto pela conta Google** (que de quebra já concede o escopo de backup no Drive). Há ainda um **login local opcional** (offline) com usuário/senha. Após o primeiro login, a sessão fica ativa e o app **não pede login novamente** ao reabrir.

### Backup automático
Em Configurações é possível ligar o **backup automático** no Google Drive, com frequência **diária** ou **semanal** (executado em segundo plano via WorkManager, somente com internet). O backup manual ("Enviar backup") continua disponível.

### Backup no Google Drive (appDataFolder)
O app pode guardar o banco de dados na **pasta privada do app no Google Drive** (`appDataFolder`, invisível ao usuário). Em **Configurações → Backup na nuvem** há login com a conta Google e botões de enviar/restaurar.

Para funcionar, é preciso um **cliente OAuth Android** no Google Cloud com:
- **Nome do pacote:** `com.soutech.refrigeracao` (igual ao `applicationId`)
- **SHA‑1 do certificado:** `F8:AD:B6:B3:7A:23:3D:35:C3:1B:E2:67:3A:15:37:63:DF:59:EA:16`
- **Escopo OAuth:** `https://www.googleapis.com/auth/drive.appdata` (adicionado na tela de consentimento)

> O app é assinado por um **keystore fixo versionado** (`app/signing/refrigeracaopro.jks`), garantindo que o SHA‑1 acima seja sempre o mesmo — tanto em builds locais quanto no APK gerado pelo GitHub Actions.

### Base técnica interna para a IA
Em `app/src/main/assets/base_tecnica/` há arquivos JSON consultados **offline** antes de chamar a IA: `gases_refrigerantes`, `compressores_referencia`, `diagnosticos_refrigeracao`, `falhas_comuns`, `procedimentos_tecnicos`, `tabela_capacitores`, `tabela_carga_fluido`. O contexto do relatório (medições, fluido, superaquecimento/subresfriamento, defeito, serviços) é combinado a essa base para gerar diagnósticos fundamentados.

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
