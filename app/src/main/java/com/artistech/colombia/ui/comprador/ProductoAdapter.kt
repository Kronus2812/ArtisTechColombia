package com.artistech.colombia.ui.comprador

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.R
import com.artistech.colombia.data.Producto
import com.artistech.colombia.databinding.ItemProductoBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mostrarImagenProducto

/** Adaptador de la cuadrícula de productos (Inicio y Catálogo). */
class ProductoAdapter(
    private var productos: List<Producto>,
    private val alTocar: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    /** Contenedor de las vistas de una fila (patrón ViewHolder con ViewBinding). */
    class ProductoViewHolder(val b: ItemProductoBinding) : RecyclerView.ViewHolder(b.root)

    /** Infla el layout de la fila con ViewBinding. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder =
        ProductoViewHolder(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Número de elementos que muestra la lista. */
    override fun getItemCount(): Int = productos.size

    /** Llena la tarjeta con nombre, región, precio, categoría, calificación y foto. */
    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        val b = holder.b
        b.tvNombre.text = producto.nombre
        b.tvRegion.text = producto.region
        b.tvPrecio.text = Formato.cop(producto.precio)
        b.tvCategoria.text = producto.categoria.etiqueta
        b.tvCalificacion.text = if (producto.resenas > 0) {
            Formato.decimal(producto.calificacion)
        } else {
            b.root.context.getString(R.string.nuevo)
        }
        mostrarImagenProducto(b.contenedorImagen, b.ivIcono, b.ivFoto, producto)
        b.root.setOnClickListener { alTocar(producto) }
    }

    /** Reemplaza la lista mostrada (por ejemplo, tras un filtro o un cambio en Firestore). */
    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Producto>) {
        productos = nuevos
        notifyDataSetChanged()
    }
}
