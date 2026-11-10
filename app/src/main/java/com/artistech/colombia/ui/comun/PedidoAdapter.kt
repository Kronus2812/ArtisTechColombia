package com.artistech.colombia.ui.comun

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.R
import com.artistech.colombia.data.Pedido
import com.artistech.colombia.databinding.ItemPedidoBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.pintarInsignia

/**
 * Adaptador de pedidos reutilizado por el Vendedor (pedidos recibidos)
 * y por el Comprador (mis pedidos en el perfil).
 */
class PedidoAdapter(
    private var pedidos: List<Pedido>,
    private val vistaVendedor: Boolean,
    private val alTocar: ((Pedido) -> Unit)? = null
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(val b: ItemPedidoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder =
        PedidoViewHolder(ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = pedidos.size

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        val b = holder.b
        val contexto = b.root.context
        b.tvCodigo.text = pedido.codigo
        b.tvEstado.pintarInsignia(pedido.estado.etiqueta, pedido.estado.fondo, pedido.estado.texto)
        b.tvDetalle.text = pedido.detalle
        b.tvInfo.text = if (vistaVendedor) {
            contexto.getString(R.string.pedido_info_vendedor, pedido.compradorNombre, pedido.fecha)
        } else {
            contexto.getString(R.string.pedido_info_comprador, pedido.fecha, pedido.metodoPago)
        }
        b.tvTotal.text = Formato.cop(pedido.total)
        b.root.setOnClickListener { alTocar?.invoke(pedido) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Pedido>) {
        pedidos = nuevos
        notifyDataSetChanged()
    }
}
