package com.artistech.colombia.ui.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoProducto
import com.artistech.colombia.data.EstadoUsuario
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.databinding.FragmentDashboardAdminBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Dashboard principal del Administrador con indicadores calculados en tiempo real a partir de Firestore. */
class DashboardAdminFragment : Fragment(R.layout.fragment_dashboard_admin) {

    private var _b: FragmentDashboardAdminBinding? = null
    private val b get() = _b!!

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentDashboardAdminBinding.bind(view)
        val admin = activity as? AdminActivity

        b.tvSaludo.text = getString(R.string.dashboard_saludo, Repositorio.usuarioActual?.nombres?.substringBefore(" ").orEmpty())

        b.alertaModeracion.setOnClickListener { admin?.navegarA(R.id.nav_productos_admin) }
        b.alertaVerificacion.setOnClickListener { admin?.navegarA(R.id.nav_usuarios) }
        b.btnIrUsuarios.setOnClickListener { admin?.navegarA(R.id.nav_usuarios) }
        b.btnIrProductos.setOnClickListener { admin?.navegarA(R.id.nav_productos_admin) }
        b.btnIrReporte.setOnClickListener { admin?.navegarA(R.id.nav_reporte) }
        b.btnIrPerfil.setOnClickListener { admin?.navegarA(R.id.nav_perfil_admin) }
        b.btnCargarCatalogo.setOnClickListener { elegirVendedorParaCatalogo() }

        // Indicadores en tiempo real: se recalculan con cada cambio de usuarios, productos o pedidos
        Repositorio.observar(viewLifecycleOwner) { actualizarIndicadores() }
    }

    /** Al volver a la pantalla se recalculan los datos visibles. */
    override fun onResume() {
        super.onResume()
        actualizarIndicadores()
    }

    /** Calcula los KPI del dashboard a partir de los datos sincronizados desde Firestore. */
    private fun actualizarIndicadores() {
        val b = _b ?: return
        val enRevision = Repositorio.productos.count { it.estado == EstadoProducto.EN_REVISION }
        val vendedoresPendientes = Repositorio.usuarios.count {
            it.rol == Rol.VENDEDOR && it.estado == EstadoUsuario.PENDIENTE
        }
        b.tvKpiUsuarios.text = Repositorio.usuarios.size.toString()
        b.tvKpiVendedores.text = Repositorio.usuarios.count { it.rol == Rol.VENDEDOR }.toString()
        b.tvKpiVentas.text = Formato.cop(Repositorio.ventasTotales())
        b.tvKpiPendientes.text = (enRevision + vendedoresPendientes).toString()
        b.tvAlertaModeracion.text = resources.getQuantityString(R.plurals.alerta_moderacion, enRevision, enRevision)
        b.tvAlertaVerificacion.text =
            resources.getQuantityString(R.plurals.alerta_verificacion, vendedoresPendientes, vendedoresPendientes)
    }

    /**
     * Carga el catálogo de ejemplo en Firestore asignándolo a un vendedor elegido por el
     * Administrador (útil para poblar una base de datos recién creada).
     */
    private fun elegirVendedorParaCatalogo() {
        val vendedores = Repositorio.usuarios.filter { it.rol == Rol.VENDEDOR }
        if (vendedores.isEmpty()) {
            Snackbar.make(b.root, R.string.dashboard_sin_vendedores, Snackbar.LENGTH_LONG).show()
            return
        }
        val nombres = vendedores.map { it.nombreTaller.ifBlank { it.nombres } }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dashboard_elegir_vendedor)
            .setItems(nombres) { _, posicion ->
                Repositorio.cargarCatalogoDemo(vendedores[posicion]) { resultado ->
                    val vista = _b ?: return@cargarCatalogoDemo
                    resultado
                        .onSuccess { cantidad -> Snackbar.make(vista.root, getString(R.string.dashboard_catalogo_cargado, cantidad), Snackbar.LENGTH_LONG).show() }
                        .onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
