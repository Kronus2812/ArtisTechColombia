package com.artistech.colombia.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.R
import com.artistech.colombia.data.ErrorNegocio
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.databinding.ActivityLoginBinding
import com.artistech.colombia.util.Biometria
import com.artistech.colombia.util.PreferenciasSesion
import com.artistech.colombia.util.irAInicioSegunRol
import com.artistech.colombia.util.mensajeError
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * Inicio de sesión (RF-02 y RF-03) con Firebase Authentication:
 * - Correo y contraseña.
 * - Huella dactilar: BiometricPrompt valida la huella y luego se usan las credenciales cifradas.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
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

        // Accesos rápidos a las cuentas de demostración de los tres roles
        b.chipDemoComprador.setOnClickListener { llenarDemo("comprador@artistech.co") }
        b.chipDemoVendedor.setOnClickListener { llenarDemo("vendedor@artistech.co") }
        b.chipDemoAdmin.setOnClickListener { llenarDemo("admin@artistech.co") }
    }

    /** Actualiza el texto de la tarjeta de huella según si hay credenciales guardadas. */
    override fun onResume() {
        super.onResume()
        val email = PreferenciasSesion.emailHuella(this)
        b.tvHuellaDetalle.text = if (email != null) {
            getString(R.string.login_huella_lista, email)
        } else {
            getString(R.string.login_huella_inactiva)
        }
    }

    /** Rellena el formulario con una cuenta de demostración. */
    private fun llenarDemo(email: String) {
        b.etEmail.setText(email)
        b.etClave.setText(Repositorio.CLAVE_DEMO)
        b.tilEmail.error = null
        b.tilClave.error = null
    }

    /** Valida el formulario y autentica con Firebase usando correo y contraseña. */
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

        mostrarCargando(true)
        Repositorio.iniciarSesion(email, clave) { resultado ->
            mostrarCargando(false)
            resultado
                .onSuccess { usuario ->
                    // Cada ingreso con contraseña deja lista (y cifrada) la huella para el siguiente
                    PreferenciasSesion.guardarCredenciales(this, email, clave)
                    entrar(usuario)
                }
                .onFailure { error -> Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show() }
        }
    }

    /** Lee la huella con BiometricPrompt y, si coincide, inicia sesión con las credenciales cifradas. */
    private fun ingresarConHuella() {
        val email = PreferenciasSesion.emailHuella(this)
        val clave = PreferenciasSesion.claveHuella(this)
        if (email == null || clave == null) {
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
                mostrarCargando(true)
                Repositorio.iniciarSesion(email, clave) { resultado ->
                    mostrarCargando(false)
                    resultado
                        .onSuccess { entrar(it) }
                        .onFailure { error ->
                            // Si la contraseña cambió (p. ej. tras recuperarla), la huella se desactiva
                            if (error is FirebaseAuthInvalidCredentialsException || error is FirebaseAuthInvalidUserException ||
                                (error is ErrorNegocio && error.mensajeRes == R.string.error_perfil_inexistente)
                            ) {
                                PreferenciasSesion.borrarCredenciales(this)
                                onResume()
                            }
                            Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show()
                        }
                }
            },
            alFallar = { mensaje -> Snackbar.make(b.root, mensaje, Snackbar.LENGTH_LONG).show() }
        )
    }

    /** Bloquea el botón mientras Firebase responde, para evitar dobles envíos. */
    private fun mostrarCargando(cargando: Boolean) {
        b.btnIngresar.isEnabled = !cargando
        b.cardHuella.isEnabled = !cargando
        b.btnIngresar.setText(if (cargando) R.string.ingresando else R.string.login_ingresar)
    }

    /** Abre la pantalla de inicio correspondiente al rol del usuario. */
    private fun entrar(usuario: Usuario) {
        irAInicioSegunRol(usuario)
    }
}
