package com.artistech.colombia.ui.admin

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
import com.artistech.colombia.databinding.ActivityUsuarioFormBinding
import com.artistech.colombia.util.mostrarToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Formulario de creación (C) y edición (U) de usuarios por parte del Administrador. */
class UsuarioFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityUsuarioFormBinding
    private var original: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUsuarioFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        original = Repositorio.usuarioPorId(intent.getIntExtra(EXTRA_ID, 0))
        b.toolbar.setTitle(if (original == null) R.string.usuario_nuevo else R.string.usuario_editar)
        b.toolbar.setNavigationOnClickListener { finish() }

        b.actTipoDoc.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, resources.getStringArray(R.array.tipos_documento)))
        b.actRol.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, Rol.entries.map { it.etiqueta }))
        b.actEstado.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, EstadoUsuario.entries.map { it.etiqueta }))

        cargarUsuario()
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnEliminar.setOnClickListener { confirmarEliminacion() }
    }

    private fun cargarUsuario() {
        val u = original
        if (u == null) {
            b.actTipoDoc.setText(getString(R.string.doc_cc), false)
            b.actRol.setText(Rol.COMPRADOR.etiqueta, false)
            b.actEstado.setText(EstadoUsuario.ACTIVO.etiqueta, false)
            return
        }
        b.tvIniciales.text = u.iniciales
        b.etNombres.setText(u.nombres)
        b.etEmail.setText(u.email)
        b.etTelefono.setText(u.telefono)
        b.actTipoDoc.setText(u.tipoDocumento, false)
        b.etDocumento.setText(u.numeroDocumento)
        b.actRol.setText(u.rol.etiqueta, false)
        b.actEstado.setText(u.estado.etiqueta, false)
        b.tilClave.isVisible = false // la contraseña nunca se muestra ni se edita desde aquí
        b.btnEliminar.isVisible = u.id != Repositorio.usuarioActual?.id
    }

    private fun guardar() {
        listOf(b.tilNombres, b.tilEmail, b.tilTelefono, b.tilDocumento, b.tilClave).forEach { it.error = null }
        val nombres = b.etNombres.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val telefono = b.etTelefono.text?.toString()?.trim().orEmpty()
        val documento = b.etDocumento.text?.toString()?.trim().orEmpty()
        val clave = b.etClave.text?.toString().orEmpty()
        val anterior = original

        var valido = true
        if (nombres.length < 3) { b.tilNombres.error = getString(R.string.error_nombres); valido = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.error_email); valido = false
        } else if (!Repositorio.emailDisponible(email, anterior?.id ?: -1)) {
            b.tilEmail.error = getString(R.string.error_email_existe); valido = false
        }
        if (telefono.length != 10) { b.tilTelefono.error = getString(R.string.error_telefono); valido = false }
        if (documento.length < 5) { b.tilDocumento.error = getString(R.string.error_documento); valido = false }
        if (anterior == null && clave.length < 8) { b.tilClave.error = getString(R.string.error_clave_corta); valido = false }
        if (!valido) return

        val rol = Rol.entries.firstOrNull { it.etiqueta == b.actRol.text.toString() } ?: Rol.COMPRADOR
        val estado = EstadoUsuario.entries.firstOrNull { it.etiqueta == b.actEstado.text.toString() } ?: EstadoUsuario.ACTIVO

        if (anterior == null) {
            Repositorio.guardarUsuario(
                Usuario(0, nombres, email, telefono, b.actTipoDoc.text.toString(), documento, rol, estado, clave)
            )
            mostrarToast(getString(R.string.usuario_creado))
        } else {
            anterior.nombres = nombres
            anterior.email = email
            anterior.telefono = telefono
            anterior.tipoDocumento = b.actTipoDoc.text.toString()
            anterior.numeroDocumento = documento
            anterior.rol = rol
            anterior.estado = estado
            Repositorio.guardarUsuario(anterior)
            mostrarToast(getString(R.string.usuario_actualizado))
        }
        finish()
    }

    private fun confirmarEliminacion() {
        val u = original ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_usuario_titulo)
            .setMessage(getString(R.string.eliminar_usuario_mensaje, u.nombres))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarUsuario(u.id)
                mostrarToast(getString(R.string.usuario_eliminado))
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "usuario_id"
    }
}
