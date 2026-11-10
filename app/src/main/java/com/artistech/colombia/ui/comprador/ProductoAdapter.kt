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

    class ProductoViewHolder(val b: ItemProductoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder =
        ProductoViewHolder(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = productos.size

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

    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Producto>) {
        productos = nuevos
        notifyDataSetChanged()
    }
}
