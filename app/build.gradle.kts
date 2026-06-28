plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "br.com.refrigeracaopro"
    compileSdk = 34

    defaultConfig {
        // Deve ser idêntico ao "Nome do pacote" cadastrado no cliente OAuth do Google
        applicationId = "com.soutech.refrigeracao"
        minSdk = 26 // Android 8.0 ou superior
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // Chave de assinatura FIXA versionada no repositório: garante um SHA-1
    // estável (igual em qualquer build, local ou no GitHub Actions), exigido
    // pelo cliente OAuth do Google (Drive appDataFolder).
    signingConfigs {
        create("comum") {
            storeFile = file("signing/refrigeracaopro.jks")
            storePassword = "refrigeracaopro"
            keyAlias = "refrigeracaopro"
            keyPassword = "refrigeracaopro"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("comum")
        }
        release {
            // MVP: sem minificação para simplificar a geração do APK
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("comum")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Opt-ins globais para APIs experimentais do Compose usadas nas telas
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose (BOM controla as versões)
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    // Aba in-app (Custom Tabs) para abrir pastas/links externos sem sair do app
    implementation("androidx.browser:browser:1.8.0")

    // Room: banco de dados local (funciona offline)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Armazenamento seguro da chave OpenAI
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP para integração com a API da OpenAI
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Carregamento de imagens (fotos de equipamentos/relatórios)
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Correção de orientação (EXIF) das fotos da câmera
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // Extração de texto de PDF (para a IA resumir/analisar arquivos anexados)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // Login Google + token OAuth para backup no Google Drive (appDataFolder)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // Agendamento de backup automático em segundo plano
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
