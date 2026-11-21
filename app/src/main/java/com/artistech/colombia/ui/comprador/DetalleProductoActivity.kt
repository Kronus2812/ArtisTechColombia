package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.R
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityDetalleProductoBinding
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mostrarImagenProducto
import com.google.android.material.snackbar.Snackbar

/** Detalle del producto (lectura desde Firestore) con selector de cantidad y botón de agregar al carrito (RF-10). */
class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var b: ActivityDetalleProductoBinding
    private lateinit var producto: Producto
    private var cantidad = 1
    private var favorito = false

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val encontrado = Repositorio.productoPorId(intent.getStringExtra(EXTRA_ID).orEmpty())
        if (encontrado == null) {
            finish()
            return
        }
        producto = encontrado
        b = ActivityDetalleProductoBinding.inflate(layoutInflater)
        setContentView(b.root)

        mostrarProducto()
        b.btnAtras.setOnClickListener { finish() }
        b.btnFavorito.setOnClickListener { alternarFavorito() }
        b.btnMenos.setOnClickListener { cambiarCantidad(-1) }
        b.btnMas.setOnClickListener { cambiarCantidad(+1) }
        b.btnAgregar.setOnClickListener { agregarAlCarrito() }
    }

    /** Pinta todos los datos del producto; si está agotado deshabilita la compra. */
    private fun mostrarProducto() {
        mostrarImagenProducto(b.contenedorImagen, b.ivIcono, b.ivFoto, producto)
        b.tvCategoria.text = getString(R.string.detalle_categoria, producto.categoria.etiqueta)
        b.tvNombre.text = producto.nombre
        b.tvPrecio.text = Formato.cop(producto.precio)
        b.tvCalificacion.text = if (producto.resenas > 0) {
            getString(R.string.detalle_calificacion, Formato.decimal(producto.calificacion), producto.resenas)
        } else {
            getString(R.string.nuevo)
        }
        // El nombre del taller viene desnormalizado en el producto (el comprador no lee perfiles ajenos)
        val nombreTaller = Repositorio.nombreVendedor(producto.vendedorId)
        b.tvAvatarVendedor.text = nombreTaller.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
        b.tvVendedor.text = nombreTaller
        b.tvRegionVendedor.text = getString(R.string.detalle_vendedor_region, producto.region)
        b.tvDescripcion.text = producto.descripcion
        b.tvOrigen.text = producto.region
        b.tvTecnica.text = producto.tecnica
        b.tvElaboracion.text = producto.tiempoElaboracion
        b.tvStock.text = resources.getQuantityString(R.plurals.unidades, producto.stock, producto.stock)

        if (producto.stock == 0) {
            b.btnAgregar.isEnabled = false
            b.btnAgregar.setText(R.string.detalle_agotado)
            b.btnMas.isEnabled = false
            b.btnMenos.isEnabled = false
        }
    }

    /** Marca o desmarca el producto como favorito (estado visual). */
    private fun alternarFavorito() {
        favorito = !favorito
        b.btnFavorito.setImageResource(if (favorito) R.drawable.ic_corazon_lleno else R.drawable.ic_corazon)
        Snackbar.make(
            b.root,
            if (favorito) R.string.detalle_favorito_si else R.string.detalle_favorito_no,
            Snackbar.LENGTH_SHORT
        ).setAnchorView(b.barraInferior).show()
    }

    /** Cambia la cantidad elegida respetando el mínimo (1) y el stock disponible. */
    private fun cambiarCantidad(delta: Int) {
        val nueva = cantidad + delta
        when {
            nueva < 1 -> return
            nueva > producto.stock ->
                Snackbar.make(b.root, getString(R.string.detalle_max_stock, producto.stock), Snackbar.LENGTH_SHORT)
                    .setAnchorView(b.barraInferior).show()
            else -> {
                cantidad = nueva
                b.tvCantidad.text = cantidad.toString()
            }
        }
    }

    /** Guarda el producto en el carrito de Firestore y ofrece ir al carrito. */
    private fun agregarAlCarrito() {
        Repositorio.agregarAlCarrito(producto, cantidad)
        Snackbar.make(b.root, resources.getQuantityString(R.plurals.agregados, cantidad, cantidad), Snackbar.LENGTH_LONG)
            .setAnchorView(b.barraInferior)
            .setAction(R.string.ver_carrito) {
                startActivity(
                    Intent(this, CompradorActivity::class.java)
                        .putExtra(CompradorActivity.EXTRA_TAB, R.id.nav_carrito)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "producto_id"
    }
}
