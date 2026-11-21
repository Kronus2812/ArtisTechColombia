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
import com.artistech.colombia.util.mensajeError
import com.artistech.colombia.util.mostrarToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthUserCollisionException

/**
 * Formulario de creación (C) y edición (U) de usuarios por parte del Administrador.
 * Crear registra la cuenta en Firebase Authentication y el perfil en Firestore; editar
 * actualiza el perfil (rol, estado y datos). El correo de una cuenta existente no se cambia
 * desde aquí porque es la credencial de acceso en Authentication.
 */
class UsuarioFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityUsuarioFormBinding
    private var original: Usuario? = null

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUsuarioFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        original = intent.getStringExtra(EXTRA_ID)?.let { Repositorio.usuarioPorId(it) }
        b.toolbar.setTitle(if (original == null) R.string.usuario_nuevo else R.string.usuario_editar)
        b.toolbar.setNavigationOnClickListener { finish() }

        b.actTipoDoc.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, resources.getStringArray(R.array.tipos_documento)))
        b.actRol.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, Rol.entries.map { it.etiqueta }))
        b.actEstado.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, EstadoUsuario.entries.map { it.etiqueta }))

        cargarUsuario()
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnEliminar.setOnClickListener { confirmarEliminacion() }
    }

    /** Modo creación: valores por defecto. Modo edición: datos actuales del usuario. */
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
        b.etEmail.isEnabled = false
        b.tilEmail.helperText = getString(R.string.usuario_email_fijo)
        b.etTelefono.setText(u.telefono)
        b.actTipoDoc.setText(u.tipoDocumento, false)
        b.etDocumento.setText(u.numeroDocumento)
        b.actRol.setText(u.rol.etiqueta, false)
        b.actEstado.setText(u.estado.etiqueta, false)
        b.tilClave.isVisible = false // la contraseña nunca se muestra ni se edita desde aquí
        b.btnEliminar.isVisible = u.id != Repositorio.usuarioActual?.id
    }

    /** Valida el formulario y crea o actualiza el usuario en Firebase. */
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
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.error = getString(R.string.error_email); valido = false }
        if (telefono.length != 10) { b.tilTelefono.error = getString(R.string.error_telefono); valido = false }
        if (documento.length < 5) { b.tilDocumento.error = getString(R.string.error_documento); valido = false }
        if (anterior == null && clave.length < 8) { b.tilClave.error = getString(R.string.error_clave_corta); valido = false }
        if (!valido) return

        val rol = Rol.entries.firstOrNull { it.etiqueta == b.actRol.text.toString() } ?: Rol.COMPRADOR
        val estado = EstadoUsuario.entries.firstOrNull { it.etiqueta == b.actEstado.text.toString() } ?: EstadoUsuario.ACTIVO

        mostrarGuardando(true)
        if (anterior == null) {
            val nuevo = Usuario("", nombres, email, telefono, b.actTipoDoc.text.toString(), documento, rol, estado)
            Repositorio.crearUsuarioComoAdmin(this, nuevo, clave) { resultado ->
                mostrarGuardando(false)
                resultado
                    .onSuccess { mostrarToast(getString(R.string.usuario_creado)); finish() }
                    .onFailure { error ->
                        if (error is FirebaseAuthUserCollisionException) b.tilEmail.error = getString(R.string.error_email_existe)
                        else Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show()
                    }
            }
        } else {
            val actualizado = anterior.copy(
                nombres = nombres,
                telefono = telefono,
                tipoDocumento = b.actTipoDoc.text.toString(),
                numeroDocumento = documento,
                rol = rol,
                estado = estado
            )
            Repositorio.actualizarUsuario(actualizado) { resultado ->
                mostrarGuardando(false)
                resultado
                    .onSuccess { mostrarToast(getString(R.string.usuario_actualizado)); finish() }
                    .onFailure { Snackbar.make(b.root, mensajeError(it), Snackbar.LENGTH_LONG).show() }
            }
        }
    }

    /** Bloquea el botón mientras Firebase responde. */
    private fun mostrarGuardando(guardando: Boolean) {
        b.btnGuardar.isEnabled = !guardando
    }

    /** Eliminación (D) del perfil con confirmación. */
    private fun confirmarEliminacion() {
        val u = original ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_usuario_titulo)
            .setMessage(getString(R.string.eliminar_usuario_mensaje, u.nombres))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarUsuario(u.id) { resultado ->
                    resultado
                        .onSuccess { mostrarToast(getString(R.string.usuario_eliminado)); finish() }
                        .onFailure { Snackbar.make(b.root, mensajeError(it), Snackbar.LENGTH_LONG).show() }
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "usuario_id"
    }
}
