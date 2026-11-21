// Archivo de construcción raíz: declara los plugins que usan los módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Plugin que lee app/google-services.json y configura Firebase en tiempo de compilación
    alias(libs.plugins.google.services) apply false
}
