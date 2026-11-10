package com.artistech.colombia.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityRecuperarContrasenaBinding
import com.artistech.colombia.util.Formato
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Recuperación de contraseña en dos pasos (RF-04). */
class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var b: ActivityRecuperarContrasenaBinding
    private var emailSolicitado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecuperarContrasenaBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnEnviarCodigo.setOnClickListener { enviarCodigo() }
        b.btnReenviar.setOnClickListener {
            Snackbar.make(b.root, getString(R.string.recuperar_reenviado, Repositorio.CODIGO_RECUPERACION_DEMO), Snackbar.LENGTH_LONG).show()
        }
        b.btnRestablecer.setOnClickListener { restablecer() }
    }

    private fun enviarCodigo() {
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.error_email)
            return
        }
        b.tilEmail.error = null
        emailSolicitado = email

        // Mensaje genérico: no revela si el correo existe (buena práctica de seguridad)
        b.pasoCorreo.isVisible = false
        b.pasoCodigo.isVisible = true
        b.tvPaso.text = getString(R.string.recuperar_paso_2)
        b.tvInstruccion.text = getString(
            R.string.recuperar_instruccion_2,
            Formato.enmascararEmail(email),
            Repositorio.CODIGO_RECUPERACION_DEMO
        )
    }

    private fun restablecer() {
        val codigo = b.etCodigo.text?.toString().orEmpty()
        val nueva = b.etNuevaClave.text?.toString().orEmpty()
        val confirmar = b.etConfirmar.text?.toString().orEmpty()
        b.tilCodigo.error = null
        b.tilNuevaClave.error = null
        b.tilConfirmar.error = null

        var valido = true
        if (codigo != Repositorio.CODIGO_RECUPERACION_DEMO) { b.tilCodigo.error = getString(R.string.error_codigo); valido = false }
        if (nueva.length < 8) { b.tilNuevaClave.error = getString(R.string.error_clave_corta); valido = false }
        if (confirmar != nueva) { b.tilConfirmar.error = getString(R.string.error_clave_no_coincide); valido = false }
        if (!valido) return

        Repositorio.cambiarContrasena(emailSolicitado, nueva)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.recuperar_exito_titulo)
            .setMessage(R.string.recuperar_exito_mensaje)
            .setCancelable(false)
            .setPositiveButton(R.string.ir_a_ingresar) { _, _ -> finish() }
            .show()
    }
}
