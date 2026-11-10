package com.artistech.colombia.ui.comprador

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.data.ItemCarrito
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ItemCarritoBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mostrarImagenProducto

/** Adaptador del carrito: actualizar cantidad (+/−) y eliminar ítems. */
class CarritoAdapter(
    private val alCambiar: () -> Unit,
    private val alSuperarStock: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    class CarritoViewHolder(val b: ItemCarritoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder =
        CarritoViewHolder(ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = Repositorio.carrito.size

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = Repositorio.carrito[position]
        val b = holder.b
        b.tvNombre.text = item.producto.nombre
        b.tvVendedor.text = Repositorio.nombreVendedor(item.producto.vendedorId)
        b.tvPrecio.text = Formato.cop(item.subtotal)
        b.tvCantidad.text = item.cantidad.toString()
        mostrarImagenProducto(b.contenedorImagen, b.ivIcono, b.ivFoto, item.producto)

        b.btnMas.setOnClickListener {
            if (Repositorio.cambiarCantidad(item, +1)) refrescar() else alSuperarStock(item)
        }
        b.btnMenos.setOnClickListener {
            Repositorio.cambiarCantidad(item, -1)
            refrescar()
        }
        b.btnEliminar.setOnClickListener {
            Repositorio.quitarDelCarrito(item)
            refrescar()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refrescar() {
        notifyDataSetChanged()
        alCambiar()
    }
}
