package com.artistech.colombia.ui.comun

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.databinding.FragmentPerfilBinding
import com.artistech.colombia.util.Biometria
import com.artistech.colombia.util.PreferenciasSesion
import com.artistech.colombia.util.cerrarSesionYSalir
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * CRUD de Perfil compartido por los tres roles:
 * consultar y actualizar datos, foto con la cámara, preferencias, cerrar sesión y eliminar cuenta.
 */
class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    private var _b: FragmentPerfilBinding? = null
    private val b get() = _b!!

    /** Captura de foto con la cámara del dispositivo (devuelve una miniatura en Bitmap). */
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        if (foto != null) {
            Repositorio.usuarioActual?.foto = foto
            mostrarFoto()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentPerfilBinding.bind(view)
        val usuario = Repositorio.usuarioActual ?: return

        // Lectura (R) del perfil
        b.tvNombre.text = usuario.nombres
        b.tvRol.text = usuario.rol.etiqueta
        b.etNombres.setText(usuario.nombres)
        b.etEmail.setText(usuario.email)
        b.etTelefono.setText(usuario.telefono)
        b.etDocumento.setText(getString(R.string.perfil_documento_valor, usuario.tipoDocumento, usuario.numeroDocumento))
        b.swNotificaciones.isChecked = usuario.notificaciones
        b.swHuella.isChecked = PreferenciasSesion.emailHuella(requireContext()) == usuario.email
        mostrarFoto()

        // Secciones según el rol
        b.seccionTaller.isVisible = usuario.rol == Rol.VENDEDOR
        b.etTaller.setText(usuario.nombreTaller)
        b.etRut.setText(usuario.rut)
        b.etRegion.setText(usuario.region)
        b.etOficio.setText(usuario.oficio)

        b.seccionPedidos.isVisible = usuario.rol == Rol.COMPRADOR
        if (usuario.rol == Rol.COMPRADOR) {
            val misPedidos = Repositorio.pedidosDelComprador(usuario.id)
            b.tvTituloPedidos.text = getString(R.string.perfil_mis_pedidos_conteo, misPedidos.size)
            b.rvMisPedidos.layoutManager = LinearLayoutManager(requireContext())
            b.rvMisPedidos.adapter = PedidoAdapter(misPedidos, vistaVendedor = false)
            b.tvSinPedidos.isVisible = misPedidos.isEmpty()
        }
        b.btnEliminarCuenta.isVisible = usuario.rol != Rol.ADMIN

        // Acciones
        b.fabFoto.setOnClickListener {
            try {
                tomarFoto.launch(null)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(b.root, R.string.error_sin_camara, Snackbar.LENGTH_LONG).show()
            }
        }
        b.swHuella.setOnCheckedChangeListener { boton, activado ->
            if (activado) {
                if (Biometria.disponible(requireContext())) {
                    PreferenciasSesion.guardarEmailHuella(requireContext(), usuario.email)
                    Snackbar.make(b.root, R.string.perfil_huella_activada, Snackbar.LENGTH_SHORT).show()
                } else {
                    boton.isChecked = false
                    Snackbar.make(b.root, R.string.huella_no_disponible, Snackbar.LENGTH_LONG).show()
                }
            } else {
                PreferenciasSesion.borrarEmailHuella(requireContext())
            }
        }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesionYSalir() }
        b.btnEliminarCuenta.setOnClickListener { confirmarEliminacion() }
    }

    private fun mostrarFoto() {
        val vista = _b ?: return
        val usuario = Repositorio.usuarioActual ?: return
        vista.tvIniciales.text = usuario.iniciales
        val foto = usuario.foto
        vista.ivFotoPerfil.isVisible = foto != null
        if (foto != null) vista.ivFotoPerfil.setImageBitmap(foto)
    }

    /** Actualización (U) del perfil con validación. */
    private fun guardar() {
        val usuario = Repositorio.usuarioActual ?: return
        val nombres = b.etNombres.text?.toString()?.trim().orEmpty()
        val telefono = b.etTelefono.text?.toString()?.trim().orEmpty()
        b.tilNombres.error = null
        b.tilTelefono.error = null
        var valido = true
        if (nombres.length < 3) { b.tilNombres.error = getString(R.string.error_nombres); valido = false }
        if (telefono.length != 10) { b.tilTelefono.error = getString(R.string.error_telefono); valido = false }
        if (!valido) return

        usuario.nombres = nombres
        usuario.telefono = telefono
        usuario.notificaciones = b.swNotificaciones.isChecked
        if (usuario.rol == Rol.VENDEDOR) {
            usuario.nombreTaller = b.etTaller.text?.toString()?.trim().orEmpty()
            usuario.rut = b.etRut.text?.toString()?.trim().orEmpty()
            usuario.region = b.etRegion.text?.toString()?.trim().orEmpty()
            usuario.oficio = b.etOficio.text?.toString()?.trim().orEmpty()
        }
        Repositorio.guardarUsuario(usuario)
        b.tvNombre.text = usuario.nombres
        mostrarFoto()
        Snackbar.make(b.root, R.string.perfil_actualizado, Snackbar.LENGTH_SHORT).show()
    }

    /** Eliminación (D) de la cuenta con confirmación (derecho de supresión, Ley 1581). */
    private fun confirmarEliminacion() {
        val usuario = Repositorio.usuarioActual ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_eliminar_titulo)
            .setMessage(R.string.perfil_eliminar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarUsuario(usuario.id)
                PreferenciasSesion.borrarEmailHuella(requireContext())
                requireActivity().cerrarSesionYSalir()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
