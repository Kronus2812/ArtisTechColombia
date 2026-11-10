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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Historial de pedidos recibidos por el Vendedor; permite actualizar el estado del envío. */
class PedidosVendedorFragment : Fragment(R.layout.fragment_pedidos_vendedor) {

    private var _b: FragmentPedidosVendedorBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: PedidoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentPedidosVendedorBinding.bind(view)

        adaptador = PedidoAdapter(emptyList(), vistaVendedor = true) { cambiarEstado(it) }
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        b.grupoEstado.setOnCheckedStateChangeListener { _, _ -> refrescar() }
        refrescar()
    }

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

    private fun cambiarEstado(pedido: Pedido) {
        val estados = EstadoPedido.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.pedido_cambiar_estado, pedido.codigo))
            .setSingleChoiceItems(estados.map { it.etiqueta }.toTypedArray(), estados.indexOf(pedido.estado)) { dialogo, posicion ->
                pedido.estado = estados[posicion]
                dialogo.dismiss()
                refrescar()
                Snackbar.make(b.root, getString(R.string.pedido_estado_actualizado, pedido.estado.etiqueta), Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
