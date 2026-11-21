package com.artistech.colombia.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.artistech.colombia.R

/**
 * Autenticación biométrica con BiometricPrompt (RF-03).
 * La huella nunca sale del dispositivo: Android solo le informa a la app "éxito" o "error".
 */
object Biometria {

    /** Indica si el teléfono tiene sensor biométrico y al menos una huella registrada. */
    fun disponible(contexto: Context): Boolean =
        BiometricManager.from(contexto).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Muestra el diálogo del sistema para leer la huella.
     * @param alAutenticar se ejecuta cuando la huella coincide.
     * @param alFallar recibe el mensaje de error del sistema (bloqueo por intentos, sensor sucio, etc.).
     */
    fun autenticar(
        actividad: FragmentActivity,
        titulo: String,
        subtitulo: String,
        alAutenticar: () -> Unit,
        alFallar: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            /** La huella coincide con una registrada en el teléfono. */
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                alAutenticar()
            }

            /** Error del sensor o bloqueo por demasiados intentos. */
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

/**
 * Credenciales para el ingreso con huella, guardadas CIFRADAS con EncryptedSharedPreferences
 * (AES-256 con una llave maestra protegida por el Android Keystore). Solo se usan después de que
 * BiometricPrompt confirma la huella, para iniciar sesión en Firebase sin escribir la contraseña.
 */
object PreferenciasSesion {
    private const val TAG = "PreferenciasSesion"
    private const val ARCHIVO = "artistech_credenciales_seguras"
    private const val CLAVE_EMAIL = "email_huella"
    private const val CLAVE_CONTRASENA = "clave_huella"

    /** Abre el archivo cifrado. Si el Keystore falla, devuelve null y la huella queda deshabilitada. */
    private fun prefs(contexto: Context): SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            ARCHIVO,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            contexto.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Almacenamiento cifrado no disponible", e)
        null
    }

    /** Correo asociado al ingreso con huella, o null si no está activado. */
    fun emailHuella(contexto: Context): String? = prefs(contexto)?.getString(CLAVE_EMAIL, null)

    /** Contraseña cifrada asociada a la huella, o null si no está activada. */
    fun claveHuella(contexto: Context): String? = prefs(contexto)?.getString(CLAVE_CONTRASENA, null)

    /** Activa el ingreso con huella guardando las credenciales cifradas. */
    fun guardarCredenciales(contexto: Context, email: String, clave: String) {
        prefs(contexto)?.edit()?.putString(CLAVE_EMAIL, email.trim().lowercase())?.putString(CLAVE_CONTRASENA, clave)?.apply()
    }

    /** Desactiva el ingreso con huella borrando las credenciales. */
    fun borrarCredenciales(contexto: Context) {
        prefs(contexto)?.edit()?.clear()?.apply()
    }
}
