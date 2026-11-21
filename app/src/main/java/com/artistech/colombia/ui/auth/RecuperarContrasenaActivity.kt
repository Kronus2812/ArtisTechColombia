package com.artistech.colombia.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityRecuperarContrasenaBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.PreferenciasSesion
import com.artistech.colombia.util.mensajeError
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * Recuperación de contraseña (RF-04) con Firebase Authentication.
 * Paso 1: la persona escribe su correo. Paso 2: Firebase envía un correo con un enlace seguro
 * para crear la nueva contraseña; la app confirma el envío y permite reenviarlo.
 */
class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var b: ActivityRecuperarContrasenaBinding
    private var emailSolicitado = ""

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecuperarContrasenaBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnEnviarCodigo.setOnClickListener { enviarEnlace() }
        b.btnReenviar.setOnClickListener { solicitar(emailSolicitado, reenvio = true) }
        b.btnRestablecer.setOnClickListener { finish() } // vuelve al inicio de sesión
    }

    /** Valida el correo del paso 1 y solicita el enlace. */
    private fun enviarEnlace() {
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.error_email)
            return
        }
        b.tilEmail.error = null
        emailSolicitado = email
        solicitar(email, reenvio = false)
    }

    /** Pide a Firebase el correo de restablecimiento y muestra el paso 2. */
    private fun solicitar(email: String, reenvio: Boolean) {
        b.btnEnviarCodigo.isEnabled = false
        b.btnReenviar.isEnabled = false
        Repositorio.recuperarContrasena(email) { resultado ->
            b.btnEnviarCodigo.isEnabled = true
            b.btnReenviar.isEnabled = true
            resultado
                .onSuccess { mostrarPasoDos(email, reenvio) }
                .onFailure { error ->
                    // Por seguridad no se revela si el correo existe: se muestra el mismo paso 2
                    if (error is FirebaseAuthInvalidUserException) mostrarPasoDos(email, reenvio)
                    else Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show()
                }
        }
    }

    /** Paso 2: instrucciones para revisar el correo. La huella se desactiva porque la clave cambiará. */
    private fun mostrarPasoDos(email: String, reenvio: Boolean) {
        if (PreferenciasSesion.emailHuella(this) == email.lowercase()) PreferenciasSesion.borrarCredenciales(this)
        b.pasoCorreo.isVisible = false
        b.pasoCodigo.isVisible = true
        b.tvPaso.text = getString(R.string.recuperar_paso_2)
        b.tvInstruccion.text = getString(R.string.recuperar_instruccion_2, Formato.enmascararEmail(email))
        if (reenvio) Snackbar.make(b.root, R.string.recuperar_reenviado, Snackbar.LENGTH_LONG).show()
    }
}
