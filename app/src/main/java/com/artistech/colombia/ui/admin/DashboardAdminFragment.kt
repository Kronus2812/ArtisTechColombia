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

/** Dashboard principal del Administrador con indicadores calculados a partir de los datos. */
class DashboardAdminFragment : Fragment(R.layout.fragment_dashboard_admin) {

    private var _b: FragmentDashboardAdminBinding? = null
    private val b get() = _b!!

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
    }

    override fun onResume() {
        super.onResume()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
