plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Conecta la app con el proyecto de Firebase (requiere app/google-services.json)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.artistech.colombia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.artistech.colombia"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        // Llave pública de ePayco (modo pruebas). Se puede reemplazar en gradle.properties
        // con la llave propia del comercio sin tocar el código fuente.
        val llaveEpayco = (project.findProperty("EPAYCO_PUBLIC_KEY") as String?) ?: "491d6a0b6e992cf924edd8d3d088aff1"
        buildConfigField("String", "EPAYCO_PUBLIC_KEY", "\"$llaveEpayco\"")
        buildConfigField("boolean", "EPAYCO_PRUEBAS", "true")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        // ViewBinding genera una clase por layout XML para acceder a las vistas sin findViewById
        viewBinding = true
        // BuildConfig expone la llave de ePayco definida arriba
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)

    // Backend: Firebase Authentication, Cloud Firestore y Cloud Storage (versiones alineadas por el BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Geolocalización con el proveedor fusionado de Google Play Services
    implementation(libs.play.services.location)

    // Carga de imágenes desde URL (fotos de productos en Firebase Storage)
    implementation(libs.coil)

    // Almacenamiento cifrado de las credenciales usadas por el ingreso con huella
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
