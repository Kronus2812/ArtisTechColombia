package com.artistech.colombia.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.databinding.FragmentUsuariosBinding
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** CRUD de usuarios del Administrador sobre Firestore: listado, búsqueda, filtro por rol, creación, edición y eliminación. */
class UsuariosFragment : Fragment(R.layout.fragment_usuarios) {

    private var _b: FragmentUsuariosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: UsuarioAdapter

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentUsuariosBinding.bind(view)

        adaptador = UsuarioAdapter(emptyList(), alEditar = { abrirFormulario(it) }, alEliminar = { confirmarEliminacion(it) })
        b.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        b.rvUsuarios.adapter = adaptador

        b.etBuscar.doAfterTextChanged { refrescar() }
        b.grupoRol.setOnCheckedStateChangeListener { _, _ -> refrescar() }
        b.fabNuevoUsuario.setOnClickListener { abrirFormulario(null) }

        // La colección "usuarios" se escucha en tiempo real (registros nuevos aparecen al instante)
        Repositorio.observar(viewLifecycleOwner) { refrescar() }
    }

    /** Al volver a la pantalla se recalculan los datos visibles. */
    override fun onResume() {
        super.onResume()
        refrescar()
    }

    /** Aplica búsqueda (nombre, correo o documento) y filtro por rol. */
    private fun refrescar() {
        val vista = _b ?: return
        val texto = vista.etBuscar.text?.toString().orEmpty().trim()
        val rol = when (vista.grupoRol.checkedChipId) {
            R.id.chipCompradores -> Rol.COMPRADOR
            R.id.chipVendedores -> Rol.VENDEDOR
            R.id.chipAdmins -> Rol.ADMIN
            else -> null
        }
        val lista = Repositorio.usuarios.filter { u ->
            (rol == null || u.rol == rol) &&
                (texto.isEmpty() || u.nombres.contains(texto, true) || u.email.contains(texto, true) || u.numeroDocumento.contains(texto))
        }
        adaptador.actualizar(lista)
        vista.tvConteo.text = getString(R.string.usuarios_conteo, lista.size, Repositorio.usuarios.size)
    }

    /** Abre el formulario vacío (crear) o con un usuario (editar). */
    private fun abrirFormulario(usuario: Usuario?) {
        val intent = Intent(requireContext(), UsuarioFormActivity::class.java)
        if (usuario != null) intent.putExtra(UsuarioFormActivity.EXTRA_ID, usuario.id)
        startActivity(intent)
    }

    /** Eliminación (D): borra el perfil en Firestore; la cuenta queda bloqueada para ingresar. */
    private fun confirmarEliminacion(usuario: Usuario) {
        if (usuario.id == Repositorio.usuarioActual?.id) {
            Snackbar.make(b.root, R.string.error_eliminar_propio, Snackbar.LENGTH_LONG).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.eliminar_usuario_titulo)
            .setMessage(getString(R.string.eliminar_usuario_mensaje, usuario.nombres))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarUsuario(usuario.id) { resultado ->
                    val vista = _b ?: return@eliminarUsuario
                    resultado
                        .onSuccess { Snackbar.make(vista.root, R.string.usuario_eliminado, Snackbar.LENGTH_SHORT).show() }
                        .onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
                }
            }
            .show()
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
