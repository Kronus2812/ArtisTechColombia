package com.artistech.colombia.ui.vendedor

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoPedido
import com.artistech.colombia.data.Pedido
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentPedidosVendedorBinding
import com.artistech.colombia.ui.comun.PedidoAdapter
import com.artistech.colombia.util.Ubicacion
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Historial de pedidos recibidos por el Vendedor (consulta en Firestore filtrada por vendedorId);
 * permite actualizar el estado del envío y ver la dirección de entrega en el mapa.
 */
class PedidosVendedorFragment : Fragment(R.layout.fragment_pedidos_vendedor) {

    private var _b: FragmentPedidosVendedorBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: PedidoAdapter

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentPedidosVendedorBinding.bind(view)

        adaptador = PedidoAdapter(emptyList(), vistaVendedor = true) { cambiarEstado(it) }
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        b.grupoEstado.setOnCheckedStateChangeListener { _, _ -> refrescar() }
        refrescar()

        // Los pedidos nuevos de los compradores aparecen al instante (escucha de Firestore)
        Repositorio.observar(viewLifecycleOwner) { refrescar() }
    }

    /** Aplica el filtro de estado y actualiza el resumen y la insignia. */
    private fun refrescar() {
        val vista = _b ?: return
        val vendedorId = Repositorio.usuarioActual?.id ?: return
        val todos = Repositorio.pedidosDelVendedor(vendedorId)
        val filtro = when (vista.grupoEstado.checkedChipId) {
            R.id.chipPorDespachar -> EstadoPedido.POR_DESPACHAR
            R.id.chipEnTransito -> EstadoPedido.EN_TRANSITO
            R.id.chipEntregados -> EstadoPedido.ENTREGADO
            else -> null
        }
        val lista = if (filtro == null) todos else todos.filter { it.estado == filtro }
        adaptador.actualizar(lista)
        vista.tvVacio.isVisible = lista.isEmpty()
        vista.tvResumen.text = getString(
            R.string.pedidos_resumen,
            todos.size,
            todos.count { it.estado == EstadoPedido.POR_DESPACHAR }
        )
        (activity as? VendedorActivity)?.actualizarInsigniaPedidos()
    }

    /** Diálogo con el detalle del pedido: dirección de entrega (con mapa) y cambio de estado. */
    private fun cambiarEstado(pedido: Pedido) {
        val estados = EstadoPedido.entries
        var elegido = estados.indexOf(pedido.estado)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.pedido_cambiar_estado, pedido.codigo))
            .setSingleChoiceItems(estados.map { it.etiqueta }.toTypedArray(), elegido) { _, posicion -> elegido = posicion }
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val nuevo = estados[elegido]
                Repositorio.actualizarEstadoPedido(pedido, nuevo) { resultado ->
                    val vista = _b ?: return@actualizarEstadoPedido
                    resultado
                        .onSuccess { Snackbar.make(vista.root, getString(R.string.pedido_estado_actualizado, nuevo.etiqueta), Snackbar.LENGTH_SHORT).show() }
                        .onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
                }
            }
        // Si el pedido tiene coordenadas de entrega, se ofrece abrirlas en Google Maps
        val lat = pedido.latitud
        val lng = pedido.longitud
        if (lat != null && lng != null) {
            dialogo.setNeutralButton(R.string.pedido_ver_mapa) { _, _ ->
                Ubicacion.abrirEnMapa(requireContext(), lat, lng, pedido.codigo)
            }
        }
        dialogo.show()
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
