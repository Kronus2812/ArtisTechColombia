package com.artistech.colombia.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoUsuario
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.databinding.ActivityRegistroBinding
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthUserCollisionException

/**
 * Registro de Compradores y Vendedores (RF-01) con autorización de tratamiento de datos
 * (Ley 1581 de 2012). Crea la cuenta en Firebase Authentication y el perfil en Firestore.
 */
class RegistroActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegistroBinding

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        val tipos = resources.getStringArray(R.array.tipos_documento)
        b.actTipoDoc.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, tipos))
        b.cbDatos.setOnCheckedChangeListener { _, marcado -> if (marcado) b.tvErrorDatos.isVisible = false }
        b.btnCrearCuenta.setOnClickListener { crearCuenta() }
    }

    /** Valida todos los campos del formulario y, si son correctos, crea la cuenta en Firebase. */
    private fun crearCuenta() {
        val nombres = b.etNombres.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val telefono = b.etTelefono.text?.toString()?.trim().orEmpty()
        val documento = b.etDocumento.text?.toString()?.trim().orEmpty()
        val clave = b.etClave.text?.toString().orEmpty()
        val confirmar = b.etConfirmar.text?.toString().orEmpty()

        listOf(b.tilNombres, b.tilEmail, b.tilTelefono, b.tilDocumento, b.tilClave, b.tilConfirmar)
            .forEach { it.error = null }

        var valido = true
        if (nombres.length < 3) { b.tilNombres.error = getString(R.string.error_nombres); valido = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.error = getString(R.string.error_email); valido = false }
        if (telefono.length != 10) { b.tilTelefono.error = getString(R.string.error_telefono); valido = false }
        if (documento.length < 5) { b.tilDocumento.error = getString(R.string.error_documento); valido = false }
        if (clave.length < 8) { b.tilClave.error = getString(R.string.error_clave_corta); valido = false }
        if (confirmar != clave) { b.tilConfirmar.error = getString(R.string.error_clave_no_coincide); valido = false }
        if (!b.cbDatos.isChecked) { b.tvErrorDatos.isVisible = true; valido = false }
        if (!valido) return

        val esVendedor = b.grupoRol.checkedButtonId == R.id.btnRolVendedor
        val datos = Usuario(
            id = "",
            nombres = nombres,
            email = email,
            telefono = telefono,
            tipoDocumento = b.actTipoDoc.text.toString(),
            numeroDocumento = documento,
            rol = if (esVendedor) Rol.VENDEDOR else Rol.COMPRADOR,
            // Los vendedores quedan pendientes hasta que el Administrador verifique sus documentos
            estado = if (esVendedor) EstadoUsuario.PENDIENTE else EstadoUsuario.ACTIVO
        )

        b.btnCrearCuenta.isEnabled = false
        b.btnCrearCuenta.setText(R.string.creando_cuenta)
        Repositorio.registrar(datos, clave) { resultado ->
            b.btnCrearCuenta.isEnabled = true
            b.btnCrearCuenta.setText(R.string.registro_crear)
            resultado
                .onSuccess { mostrarExito(esVendedor) }
                .onFailure { error ->
                    // El correo repetido se marca directamente en su campo
                    if (error is FirebaseAuthUserCollisionException) {
                        b.tilEmail.error = getString(R.string.error_email_existe)
                    } else {
                        Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show()
                    }
                }
        }
    }

    /** Diálogo de confirmación; al cerrarlo vuelve al inicio de sesión. */
    private fun mostrarExito(esVendedor: Boolean) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.registro_exito_titulo)
            .setMessage(if (esVendedor) R.string.registro_exito_vendedor else R.string.registro_exito_comprador)
            .setCancelable(false)
            .setPositiveButton(R.string.entendido) { _, _ -> finish() }
            .show()
    }
}
