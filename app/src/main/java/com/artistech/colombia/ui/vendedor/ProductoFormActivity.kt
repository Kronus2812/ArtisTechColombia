package com.artistech.colombia.ui.vendedor

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import coil.load
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.EstadoProducto
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.databinding.ActivityProductoFormBinding
import com.artistech.colombia.util.ImagenesFirebase
import com.artistech.colombia.util.mensajeError
import com.artistech.colombia.util.mostrarToast
import com.artistech.colombia.util.pintarInsignia
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.File

/**
 * Formulario para crear (C) y editar (U) productos en Firestore, con foto desde la cámara
 * (resolución completa) o la galería. La foto se sube a Firebase Storage al guardar.
 */
class ProductoFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityProductoFormBinding
    private var original: Producto? = null
    private var fotoNueva: Bitmap? = null
    private var categoria: Categoria? = null

    /** Archivo temporal donde la app de cámara guarda la foto a resolución completa. */
    private var uriCamara: Uri? = null

    /** Cámara: TakePicture escribe la foto en el Uri del FileProvider. */
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = uriCamara
        if (exito && uri != null) usarFoto(uri)
    }

    /** Galería: selector de fotos del sistema (no requiere permisos de almacenamiento). */
    private val elegirDeGaleria = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usarFoto(uri)
    }

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProductoFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        original = intent.getStringExtra(EXTRA_ID)?.let { Repositorio.productoPorId(it) }
        b.toolbar.setTitle(if (original == null) R.string.form_producto_nuevo else R.string.form_producto_editar)
        b.toolbar.setNavigationOnClickListener { finish() }

        val etiquetas = Categoria.entries.map { it.etiqueta }
        b.actCategoria.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, etiquetas))
        b.actCategoria.setOnItemClickListener { _, _, posicion, _ ->
            categoria = Categoria.entries[posicion]
            b.tilCategoria.error = null
            actualizarFondoCategoria()
        }

        b.btnTomarFoto.setOnClickListener { abrirCamara() }
        b.btnGaleria.setOnClickListener {
            elegirDeGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.btnGuardar.setOnClickListener { guardar() }
        b.btnEliminar.setOnClickListener { confirmarEliminacion() }

        cargarProducto()
    }

    /** Crea un archivo temporal en la caché, lo comparte con la cámara mediante FileProvider y la abre. */
    private fun abrirCamara() {
        try {
            val carpeta = File(cacheDir, "fotos").apply { mkdirs() }
            val archivo = File(carpeta, "producto_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
            uriCamara = uri
            tomarFoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(b.root, R.string.error_sin_camara, Snackbar.LENGTH_LONG).show()
        }
    }

    /** Decodifica la foto elegida (reducida y enderezada) y la muestra como vista previa. */
    private fun usarFoto(uri: Uri) {
        val imagen = ImagenesFirebase.cargarDesdeUri(this, uri)
        if (imagen == null) {
            Snackbar.make(b.root, R.string.error_foto, Snackbar.LENGTH_LONG).show()
            return
        }
        fotoNueva = imagen
        b.ivFoto.setImageBitmap(imagen)
        mostrarVistaPrevia(true)
    }

    /** Modo edición: llena el formulario con los datos actuales del producto. */
    private fun cargarProducto() {
        val p = original
        if (p == null) {
            b.tvEstado.pintarInsignia(getString(R.string.form_estado_nuevo), R.color.mostaza_claro, R.color.mostaza_oscuro)
            Repositorio.usuarioActual?.region?.takeIf { it.isNotBlank() }?.let { b.etRegion.setText(it) }
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
            p.fotoUrl.isNotBlank() -> { b.ivFoto.load(p.fotoUrl); mostrarVistaPrevia(true) }
            p.fotoRespaldo != null -> { b.ivFoto.setImageBitmap(p.fotoRespaldo); mostrarVistaPrevia(true) }
        }
    }

    /** Muestra el degradado y el ícono de la categoría elegida mientras no haya foto. */
    private fun actualizarFondoCategoria() {
        val c = categoria ?: return
        b.contenedorImagen.setBackgroundResource(c.fondo)
        b.ivIcono.setImageResource(c.icono)
    }

    /** Alterna entre la foto elegida y el ícono de la categoría. */
    private fun mostrarVistaPrevia(conFoto: Boolean) {
        b.ivFoto.isVisible = conFoto
        b.ivIcono.isVisible = !conFoto
    }

    /** Texto limpio (sin espacios a los lados) de un campo del formulario. */
    private fun texto(campo: TextInputEditText): String = campo.text?.toString()?.trim().orEmpty()

    /** Valida los campos y guarda el producto (con su foto) en Firebase. */
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
        val usuario = Repositorio.usuarioActual ?: return
        val esAdmin = usuario.rol == Rol.ADMIN
        // Reglas de estado: lo nuevo pasa por moderación; sin stock queda agotado; al reponer se publica
        val estado = when {
            anterior == null -> if (esAdmin) EstadoProducto.PUBLICADO else EstadoProducto.EN_REVISION
            stock == 0 -> EstadoProducto.AGOTADO
            anterior.estado == EstadoProducto.AGOTADO -> EstadoProducto.PUBLICADO
            else -> anterior.estado
        }
        val producto = Producto(
            id = anterior?.id ?: "",
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            stock = stock,
            categoria = categoria ?: Categoria.TEJIDOS,
            region = region,
            tecnica = texto(b.etTecnica),
            tiempoElaboracion = texto(b.etTiempo),
            vendedorId = anterior?.vendedorId ?: usuario.id,
            vendedorNombre = anterior?.vendedorNombre ?: usuario.nombreTaller.ifBlank { usuario.nombres },
            estado = estado,
            calificacion = anterior?.calificacion ?: 0.0,
            resenas = anterior?.resenas ?: 0,
            fotoUrl = anterior?.fotoUrl.orEmpty(),
            fotoBase64 = anterior?.fotoBase64.orEmpty(),
            creadoEn = anterior?.creadoEn ?: System.currentTimeMillis()
        )

        mostrarGuardando(true)
        Repositorio.guardarProducto(producto, fotoNueva) { resultado ->
            mostrarGuardando(false)
            resultado
                .onSuccess {
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
                .onFailure { error -> Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show() }
        }
    }

    /** Bloquea el botón mientras se sube la foto y se escribe en Firestore. */
    private fun mostrarGuardando(guardando: Boolean) {
        b.btnGuardar.isEnabled = !guardando
        b.btnGuardar.setText(if (guardando) R.string.guardando else R.string.guardar)
    }

    /** Eliminación (D) con confirmación. */
    private fun confirmarEliminacion() {
        val p = original ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar_producto_titulo)
            .setMessage(getString(R.string.eliminar_producto_mensaje, p.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarProducto(p) { resultado ->
                    resultado
                        .onSuccess {
                            mostrarToast(getString(R.string.producto_eliminado))
                            finish()
                        }
                        .onFailure { error -> Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show() }
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "producto_id"
    }
}
