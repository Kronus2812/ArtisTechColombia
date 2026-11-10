package com.artistech.colombia.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.artistech.colombia.R

/**
 * Autenticación biométrica con BiometricPrompt (RF-03).
 * La huella nunca sale del dispositivo: la app solo recibe "éxito" o "error".
 */
object Biometria {

    fun disponible(contexto: Context): Boolean =
        BiometricManager.from(contexto).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun autenticar(
        actividad: FragmentActivity,
        titulo: String,
        subtitulo: String,
        alAutenticar: () -> Unit,
        alFallar: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                alAutenticar()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Cancelar o tocar "Usar contraseña" no se considera un error para el usuario
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    alFallar(errString.toString())
                }
            }
        }
        val prompt = BiometricPrompt(actividad, ContextCompat.getMainExecutor(actividad), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setNegativeButtonText(actividad.getString(R.string.huella_usar_clave))
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}

/** Preferencias locales mínimas: correo asociado al ingreso con huella. */
object PreferenciasSesion {
    private const val ARCHIVO = "artistech_preferencias"
    private const val CLAVE_EMAIL_HUELLA = "email_huella"

    private fun prefs(contexto: Context) = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun emailHuella(contexto: Context): String? = prefs(contexto).getString(CLAVE_EMAIL_HUELLA, null)

    fun guardarEmailHuella(contexto: Context, email: String) {
        prefs(contexto).edit().putString(CLAVE_EMAIL_HUELLA, email).apply()
    }

    fun borrarEmailHuella(contexto: Context) {
        prefs(contexto).edit().remove(CLAVE_EMAIL_HUELLA).apply()
    }
}
