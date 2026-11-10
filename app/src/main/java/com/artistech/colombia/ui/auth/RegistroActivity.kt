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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Registro de Compradores y Vendedores con autorización de tratamiento de datos (Ley 1581). */
class RegistroActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegistroBinding

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
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.error_email); valido = false
        } else if (!Repositorio.emailDisponible(email)) {
            b.tilEmail.error = getString(R.string.error_email_existe); valido = false
        }
        if (telefono.length != 10) { b.tilTelefono.error = getString(R.string.error_telefono); valido = false }
        if (documento.length < 5) { b.tilDocumento.error = getString(R.string.error_documento); valido = false }
        if (clave.length < 8) { b.tilClave.error = getString(R.string.error_clave_corta); valido = false }
        if (confirmar != clave) { b.tilConfirmar.error = getString(R.string.error_clave_no_coincide); valido = false }
        if (!b.cbDatos.isChecked) { b.tvErrorDatos.isVisible = true; valido = false }
        if (!valido) return

        val esVendedor = b.grupoRol.checkedButtonId == R.id.btnRolVendedor
        Repositorio.guardarUsuario(
            Usuario(
                id = 0,
                nombres = nombres,
                email = email,
                telefono = telefono,
                tipoDocumento = b.actTipoDoc.text.toString(),
                numeroDocumento = documento,
                rol = if (esVendedor) Rol.VENDEDOR else Rol.COMPRADOR,
                // Los vendedores quedan pendientes hasta que el Administrador verifique sus documentos
                estado = if (esVendedor) EstadoUsuario.PENDIENTE else EstadoUsuario.ACTIVO,
                contrasena = clave
            )
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.registro_exito_titulo)
            .setMessage(if (esVendedor) R.string.registro_exito_vendedor else R.string.registro_exito_comprador)
            .setCancelable(false)
            .setPositiveButton(R.string.entendido) { _, _ -> finish() }
            .show()
    }
}
