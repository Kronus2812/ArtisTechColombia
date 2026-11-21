package com.artistech.colombia.ui.comun

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
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
import com.artistech.colombia.util.mensajeError
import com.artistech.colombia.util.mostrarFotoUsuario
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * CRUD de Perfil compartido por los tres roles (documento usuarios/{uid} en Firestore):
 * consultar y actualizar datos, foto con la cámara (Storage), activar la huella, ver el
 * historial de pedidos (Comprador), cerrar sesión y eliminar la cuenta.
 */
class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    private var _b: FragmentPerfilBinding? = null
    private val b get() = _b!!
    private lateinit var adaptadorPedidos: PedidoAdapter

    /** Captura de foto con la cámara del dispositivo; se sube a Firebase Storage. */
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        if (foto != null) {
            _b?.ivFotoPerfil?.setImageBitmap(foto)
            _b?.ivFotoPerfil?.isVisible = true
            Repositorio.actualizarFotoPerfil(foto) { resultado ->
                val vista = _b ?: return@actualizarFotoPerfil
                resultado.onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
            }
        }
    }

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
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
        b.swHuella.isChecked = PreferenciasSesion.emailHuella(requireContext()) == usuario.email.lowercase()
        mostrarFoto()

        // Secciones según el rol
        b.seccionTaller.isVisible = usuario.rol == Rol.VENDEDOR
        b.etTaller.setText(usuario.nombreTaller)
        b.etRut.setText(usuario.rut)
        b.etRegion.setText(usuario.region)
        b.etOficio.setText(usuario.oficio)

        b.seccionPedidos.isVisible = usuario.rol == Rol.COMPRADOR
        if (usuario.rol == Rol.COMPRADOR) {
            adaptadorPedidos = PedidoAdapter(emptyList(), vistaVendedor = false)
            b.rvMisPedidos.layoutManager = LinearLayoutManager(requireContext())
            b.rvMisPedidos.adapter = adaptadorPedidos
            mostrarPedidos()
            // Historial de compras en tiempo real desde la colección "pedidos"
            Repositorio.observar(viewLifecycleOwner) { mostrarPedidos(); mostrarFoto() }
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
            if (!activado) {
                PreferenciasSesion.borrarCredenciales(requireContext())
                return@setOnCheckedChangeListener
            }
            if (PreferenciasSesion.emailHuella(requireContext()) == usuario.email.lowercase()) return@setOnCheckedChangeListener
            if (Biometria.disponible(requireContext())) {
                pedirClaveParaHuella()
            } else {
                boton.isChecked = false
                Snackbar.make(b.root, R.string.huella_no_disponible, Snackbar.LENGTH_LONG).show()
            }
        }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesionYSalir() }
        b.btnEliminarCuenta.setOnClickListener { confirmarEliminacion() }
    }

    /** Lista de "Mis pedidos" del Comprador. */
    private fun mostrarPedidos() {
        val vista = _b ?: return
        val uid = Repositorio.usuarioActual?.id ?: return
        val misPedidos = Repositorio.pedidosDelComprador(uid)
        vista.tvTituloPedidos.text = getString(R.string.perfil_mis_pedidos_conteo, misPedidos.size)
        adaptadorPedidos.actualizar(misPedidos)
        vista.tvSinPedidos.isVisible = misPedidos.isEmpty()
    }

    /** Foto de perfil desde Storage o, si no hay, las iniciales. */
    private fun mostrarFoto() {
        val vista = _b ?: return
        val usuario = Repositorio.usuarioActual ?: return
        vista.tvIniciales.text = usuario.iniciales
        mostrarFotoUsuario(vista.ivFotoPerfil, usuario)
    }

    /**
     * Para activar la huella se confirma la contraseña con Firebase (reautenticación) y luego
     * se guarda cifrada en el dispositivo.
     */
    private fun pedirClaveParaHuella() {
        val usuario = Repositorio.usuarioActual ?: return
        val campo = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val contenedor = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.campo_clave)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            addView(campo)
        }
        val marco = FrameLayout(requireContext()).apply {
            val m = resources.getDimensionPixelSize(R.dimen.espacio_l)
            setPadding(m, m / 2, m, 0)
            addView(contenedor)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_huella_confirmar)
            .setView(marco)
            .setCancelable(false)
            .setNegativeButton(R.string.cancelar) { _, _ -> _b?.swHuella?.isChecked = false }
            .setPositiveButton(R.string.activar) { _, _ ->
                val clave = campo.text?.toString().orEmpty()
                Repositorio.verificarClave(clave) { resultado ->
                    val vista = _b ?: return@verificarClave
                    resultado
                        .onSuccess {
                            PreferenciasSesion.guardarCredenciales(requireContext(), usuario.email, clave)
                            Snackbar.make(vista.root, R.string.perfil_huella_activada, Snackbar.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            vista.swHuella.isChecked = false
                            Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show()
                        }
                }
            }
            .show()
    }

    /** Actualización (U) del perfil con validación; se guarda en Firestore. */
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

        val actualizado = usuario.copy(
            nombres = nombres,
            telefono = telefono,
            notificaciones = b.swNotificaciones.isChecked
        )
        if (usuario.rol == Rol.VENDEDOR) {
            actualizado.nombreTaller = b.etTaller.text?.toString()?.trim().orEmpty()
            actualizado.rut = b.etRut.text?.toString()?.trim().orEmpty()
            actualizado.region = b.etRegion.text?.toString()?.trim().orEmpty()
            actualizado.oficio = b.etOficio.text?.toString()?.trim().orEmpty()
        }
        b.btnGuardar.isEnabled = false
        Repositorio.actualizarUsuario(actualizado) { resultado ->
            val vista = _b ?: return@actualizarUsuario
            vista.btnGuardar.isEnabled = true
            resultado
                .onSuccess {
                    vista.tvNombre.text = actualizado.nombres
                    mostrarFoto()
                    Snackbar.make(vista.root, R.string.perfil_actualizado, Snackbar.LENGTH_SHORT).show()
                }
                .onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
        }
    }

    /** Eliminación (D) de la cuenta con confirmación (derecho de supresión, Ley 1581). */
    private fun confirmarEliminacion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_eliminar_titulo)
            .setMessage(R.string.perfil_eliminar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                val actividad = requireActivity()
                PreferenciasSesion.borrarCredenciales(actividad)
                Repositorio.eliminarCuentaPropia { resultado ->
                    resultado
                        .onSuccess { actividad.cerrarSesionYSalir() }
                        .onFailure { error -> _b?.let { Snackbar.make(it.root, actividad.mensajeError(error), Snackbar.LENGTH_LONG).show() } }
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
