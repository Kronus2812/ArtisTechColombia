package com.artistech.colombia.ui.vendedor

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.EstadoProducto
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.databinding.ActivityProductoFormBinding
import com.artistech.colombia.util.mostrarToast
import com.artistech.colombia.util.pintarInsignia
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Formulario para crear (C) y editar (U) productos, con foto desde la cámara o la galería. */
class ProductoFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityProductoFormBinding
    private var original: Producto? = null
    private var fotoNueva: Bitmap? = null
    private var uriNueva: Uri? = null
    private var categoria: Categoria? = null

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        if (foto != null) {
            fotoNueva = foto
            uriNueva = null
            b.ivFoto.setImageBitmap(foto)
            mostrarVistaPrevia(true)
        }
    }

    private val elegirDeGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uriNueva = uri
            fotoNueva = null
            b.ivFoto.setImageURI(uri)
            mostrarVistaPrevia(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProductoFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        original = Repositorio.productoPorId(intent.getIntExtra(EXTRA_ID, 0))
        b.toolbar.setTitle(if (original == null) R.string.form_producto_nuevo else R.string.form_producto_editar)
        b.toolbar.setNavigationOnClickListener { finish() }

        val etiquetas = Categoria.entries.map { it.etiqueta }
        b.actCategoria.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, etiquetas))
        b.actCategoria.setOnItemClickListener { _, _, posicion, _ ->
            categoria = Categoria.entries[posicion]
            b.tilCategoria.error = null
            actualizarFondoCategoria()
        }

        b.btnTomarFoto.setOnClickListener {
            try {
                tomarFoto.launch(null)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(b.root, R.string.error_sin_camara, Snackbar.LENGTH_LONG).show()
            }
        }
        b.btnGaleria.setOnClickListener { elegirDeGaleria.launch("image/*") }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnEliminar.setOnClickListener { confirmarEliminacion() }

        cargarProducto()
    }

    private fun cargarProducto() {
        val p = original
        if (p == null) {
            b.tvEstado.pintarInsignia(getString(R.string.form_estado_nuevo), R.color.mostaza_claro, R.color.mostaza_oscuro)
            Repositorio.usuarioActual?.region?.let { b.etRegion.setText(it) }
            return
        }
        categoria = p.categoria
        b.etNombre.setText(p.nombre)
        b.etPrecio.setText(p.precio.toString())
        b.etStock.setText(p.stock.toString())
        b.actCategoria.setText(p.categoria.etiqueta, false)
        b.etRegion.setText(p.region)
        b.etTecnica.setText(p.tecnica)
        b.etTiempo.setText(p.tiempoElaboracion)
        b.etDescripcion.setText(p.descripcion)
        b.tvEstado.pintarInsignia(p.estado.etiqueta, p.estado.fondo, p.estado.texto)
        b.btnEliminar.isVisible = true
        actualizarFondoCategoria()
        when {
            p.foto != null -> { b.ivFoto.setImageBitmap(p.foto); mostrarVistaPrevia(true) }
            p.fotoUri != null -> { b.ivFoto.setImageURI(p.fotoUri); mostrarVistaPrevia(true) }
        }
    }

    private fun actualizarFondoCategoria() {
        val c = categoria ?: return
        b.contenedorImagen.setBackgroundResource(c.fondo)
        b.ivIcono.setImageResource(c.icono)
    }

    private fun mostrarVistaPrevia(conFoto: Boolean) {
        b.ivFoto.isVisible = conFoto
        b.ivIcono.isVisible = !conFoto
    }

    private fun texto(campo: com.google.android.material.textfield.TextInputEditText): String =
        campo.text?.toString()?.trim().orEmpty()

    private fun guardar() {
        listOf(b.tilNombre, b.tilPrecio, b.tilStock, b.tilCategoria, b.tilRegion, b.tilDescripcion)
            .forEach { it.error = null }
        val nombre = texto(b.etNombre)
        val precio = texto(b.etPrecio).toLongOrNull()
        val stock = texto(b.etStock).toIntOrNull()
        val region = texto(b.etRegion)
        val descripcion = texto(b.etDescripcion)

        var valido = true
        if (nombre.length < 3) { b.tilNombre.error = getString(R.string.error_nombre_producto); valido = false }
        if (precio == null || precio <= 0) { b.tilPrecio.error = getString(R.string.error_precio); valido = false }
        if (stock == null || stock < 0) { b.tilStock.error = getString(R.string.error_stock); valido = false }
        if (categoria == null) { b.tilCategoria.error = getString(R.string.error_categoria); valido = false }
        if (region.isEmpty()) { b.tilRegion.error = getString(R.string.error_obligatorio); valido = false }
        if (descripcion.length < 10) { b.tilDescripcion.error = getString(R.string.error_descripcion); valido = false }
        if (!valido || precio == null || stock == null) return

        val anterior = original
        val esAdmin = Repositorio.usuarioActual?.rol == Rol.ADMIN
        val estado = when {
            anterior == null -> EstadoProducto.EN_REVISION            // todo producto nuevo pasa por moderación
            stock == 0 -> EstadoProducto.AGOTADO
            anterior.estado == EstadoProducto.AGOTADO -> EstadoProducto.PUBLICADO
            else -> anterior.estado
        }
        Repositorio.guardarProducto(
            Producto(
                id = anterior?.id ?: 0,
                nombre = nombre,
                descripcion = descripcion,
                precio = precio,
                stock = stock,
                categoria = categoria ?: Categoria.TEJIDOS,
                region = region,
                tecnica = texto(b.etTecnica),
                tiempoElaboracion = texto(b.etTiempo),
                vendedorId = anterior?.vendedorId ?: Repositorio.usuarioActual?.id ?: 0,
                estado = estado,
                calificacion = anterior?.calificacion ?: 0.0,
                resenas = anterior?.resenas ?: 0,
                foto = fotoNueva ?: if (uriNueva == null) anterior?.foto else null,
                fotoUri = uriNueva ?: if (fotoNueva == null) anterior?.fotoUri else null
            )
        )
        mostrarToast(
            getString(
                when {
                    anterior != null -> R.string.producto_actualizado
                    esAdmin -> R.string.producto_creado
                    else -> R.string.producto_enviado_revision
                }
            )
        )
        finish()
    }

    private fun confirmarEliminacion() {
        val p = original ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_producto_titulo)
            .setMessage(getString(R.string.eliminar_producto_mensaje, p.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarProducto(p.id)
                mostrarToast(getString(R.string.producto_eliminado))
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "producto_id"
    }
}
