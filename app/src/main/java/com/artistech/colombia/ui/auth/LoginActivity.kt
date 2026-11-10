package com.artistech.colombia.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoUsuario
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.databinding.ActivityLoginBinding
import com.artistech.colombia.util.Biometria
import com.artistech.colombia.util.PreferenciasSesion
import com.artistech.colombia.util.irAInicioSegunRol
import com.google.android.material.snackbar.Snackbar

/** Autenticación con correo/contraseña o con huella (BiometricPrompt). */
class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnIngresar.setOnClickListener { ingresarConClave() }
        b.cardHuella.setOnClickListener { ingresarConHuella() }
        b.tvOlvido.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
        b.btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // Accesos de demostración para revisar los tres roles
        b.chipDemoComprador.setOnClickListener { llenarDemo("comprador@artistech.co") }
        b.chipDemoVendedor.setOnClickListener { llenarDemo("vendedor@artistech.co") }
        b.chipDemoAdmin.setOnClickListener { llenarDemo("admin@artistech.co") }
    }

    override fun onResume() {
        super.onResume()
        val email = PreferenciasSesion.emailHuella(this)
        b.tvHuellaDetalle.text = if (email != null) {
            getString(R.string.login_huella_lista, email)
        } else {
            getString(R.string.login_huella_inactiva)
        }
    }

    private fun llenarDemo(email: String) {
        b.etEmail.setText(email)
        b.etClave.setText(Repositorio.CLAVE_DEMO)
        b.tilEmail.error = null
        b.tilClave.error = null
    }

    private fun ingresarConClave() {
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val clave = b.etClave.text?.toString().orEmpty()
        b.tilEmail.error = null
        b.tilClave.error = null

        var valido = true
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.error_email)
            valido = false
        }
        if (clave.isEmpty()) {
            b.tilClave.error = getString(R.string.error_clave_vacia)
            valido = false
        }
        if (!valido) return

        val usuario = Repositorio.validarCredenciales(email, clave)
        when {
            usuario == null -> Snackbar.make(b.root, R.string.error_credenciales, Snackbar.LENGTH_LONG).show()
            usuario.estado == EstadoUsuario.SUSPENDIDO ->
                Snackbar.make(b.root, R.string.error_suspendido, Snackbar.LENGTH_LONG).show()
            else -> {
                // Se recuerda el correo para habilitar el siguiente ingreso con huella
                PreferenciasSesion.guardarEmailHuella(this, usuario.email)
                entrar(usuario)
            }
        }
    }

    private fun ingresarConHuella() {
        val email = PreferenciasSesion.emailHuella(this)
        if (email == null) {
            Snackbar.make(b.root, R.string.login_huella_primero, Snackbar.LENGTH_LONG).show()
            return
        }
        if (!Biometria.disponible(this)) {
            Snackbar.make(b.root, R.string.huella_no_disponible, Snackbar.LENGTH_LONG).show()
            return
        }
        Biometria.autenticar(
            actividad = this,
            titulo = getString(R.string.huella_titulo),
            subtitulo = getString(R.string.huella_subtitulo_login),
            alAutenticar = {
                val usuario = Repositorio.usuarioPorEmail(email)
                if (usuario != null && usuario.estado != EstadoUsuario.SUSPENDIDO) {
                    entrar(usuario)
                } else {
                    Snackbar.make(b.root, R.string.error_credenciales, Snackbar.LENGTH_LONG).show()
                }
            },
            alFallar = { mensaje -> Snackbar.make(b.root, mensaje, Snackbar.LENGTH_LONG).show() }
        )
    }

    private fun entrar(usuario: Usuario) {
        Repositorio.abrirSesion(usuario)
        irAInicioSegunRol(usuario)
    }
}
