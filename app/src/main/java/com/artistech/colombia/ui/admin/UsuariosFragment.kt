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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Gestión de usuarios: listado, búsqueda, filtro por rol, creación, edición y eliminación. */
class UsuariosFragment : Fragment(R.layout.fragment_usuarios) {

    private var _b: FragmentUsuariosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: UsuarioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentUsuariosBinding.bind(view)

        adaptador = UsuarioAdapter(emptyList(), alEditar = { abrirFormulario(it) }, alEliminar = { confirmarEliminacion(it) })
        b.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        b.rvUsuarios.adapter = adaptador

        b.etBuscar.doAfterTextChanged { refrescar() }
        b.grupoRol.setOnCheckedStateChangeListener { _, _ -> refrescar() }
        b.fabNuevoUsuario.setOnClickListener { abrirFormulario(null) }
    }

    override fun onResume() {
        super.onResume()
        refrescar()
    }

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

    private fun abrirFormulario(usuario: Usuario?) {
        val intent = Intent(requireContext(), UsuarioFormActivity::class.java)
        if (usuario != null) intent.putExtra(UsuarioFormActivity.EXTRA_ID, usuario.id)
        startActivity(intent)
    }

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
                Repositorio.eliminarUsuario(usuario.id)
                refrescar()
                Snackbar.make(b.root, R.string.usuario_eliminado, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
