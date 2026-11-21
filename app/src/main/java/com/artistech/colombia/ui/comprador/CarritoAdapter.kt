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

/**
 * Adaptador del carrito: muestra cada ítem y permite actualizar la cantidad (+/−) y eliminarlo.
 * Cada acción se guarda en Firestore a través del Repositorio.
 */
class CarritoAdapter(
    private val alCambiar: () -> Unit,
    private val alSuperarStock: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    /** Contenedor de las vistas de una fila (patrón ViewHolder con ViewBinding). */
    class CarritoViewHolder(val b: ItemCarritoBinding) : RecyclerView.ViewHolder(b.root)

    /** Infla el layout de la fila con ViewBinding. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder =
        CarritoViewHolder(ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Número de elementos que muestra la lista. */
    override fun getItemCount(): Int = Repositorio.carrito.size

    /** Llena la fila con los datos del ítem y conecta los botones de cantidad y eliminar. */
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

    /** Redibuja la lista completa y avisa al fragmento para recalcular el resumen. */
    @SuppressLint("NotifyDataSetChanged")
    fun refrescar() {
        notifyDataSetChanged()
        alCambiar()
    }
}
