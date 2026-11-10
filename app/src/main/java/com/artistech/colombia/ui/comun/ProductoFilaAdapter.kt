package com.artistech.colombia.ui.comun

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.R
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ItemProductoFilaBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mostrarImagenProducto
import com.artistech.colombia.util.pintarInsignia

/** Filas de productos con acciones CRUD, compartidas por el Vendedor y el Administrador. */
class ProductoFilaAdapter(
    private var productos: List<Producto>,
    private val mostrarVendedor: Boolean,
    private val alTocar: (Producto) -> Unit,
    private val alEditar: (Producto) -> Unit,
    private val alEliminar: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoFilaAdapter.FilaViewHolder>() {

    class FilaViewHolder(val b: ItemProductoFilaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilaViewHolder =
        FilaViewHolder(ItemProductoFilaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = productos.size

    override fun onBindViewHolder(holder: FilaViewHolder, position: Int) {
        val producto = productos[position]
        val b = holder.b
        val contexto = b.root.context
        b.tvNombre.text = producto.nombre
        b.tvSubtitulo.text = if (mostrarVendedor) {
            contexto.getString(R.string.fila_precio_vendedor, Formato.cop(producto.precio), Repositorio.nombreVendedor(producto.vendedorId))
        } else {
            Formato.cop(producto.precio)
        }
        b.tvEstado.pintarInsignia(producto.estado.etiqueta, producto.estado.fondo, producto.estado.texto)
        b.tvStock.text = contexto.getString(R.string.fila_stock, producto.stock)
        mostrarImagenProducto(b.contenedorImagen, b.ivIcono, b.ivFoto, producto)
        b.fila.setOnClickListener { alTocar(producto) }
        b.btnEditar.setOnClickListener { alEditar(producto) }
        b.btnEliminar.setOnClickListener { alEliminar(producto) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Producto>) {
        productos = nuevos
        notifyDataSetChanged()
    }
}
