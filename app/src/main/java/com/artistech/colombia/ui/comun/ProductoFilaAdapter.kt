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

    /** Contenedor de las vistas de una fila (patrón ViewHolder con ViewBinding). */
    class FilaViewHolder(val b: ItemProductoFilaBinding) : RecyclerView.ViewHolder(b.root)

    /** Infla el layout de la fila con ViewBinding. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilaViewHolder =
        FilaViewHolder(ItemProductoFilaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Número de elementos que muestra la lista. */
    override fun getItemCount(): Int = productos.size

    /** Llena la fila (nombre, precio, vendedor, estado, stock, foto) y conecta las acciones CRUD. */
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

    /** Reemplaza la lista de productos mostrada. */
    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Producto>) {
        productos = nuevos
        notifyDataSetChanged()
    }
}
