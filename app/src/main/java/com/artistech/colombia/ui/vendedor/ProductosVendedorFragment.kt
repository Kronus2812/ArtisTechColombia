package com.artistech.colombia.ui.vendedor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoProducto
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentProductosVendedorBinding
import com.artistech.colombia.ui.comun.ProductoFilaAdapter
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Panel de productos del Vendedor: CRUD completo sobre la colección "productos" de Firestore
 * (crear, listar, editar y eliminar) con indicadores de ventas y filtros por estado.
 */
class ProductosVendedorFragment : Fragment(R.layout.fragment_productos_vendedor) {

    private var _b: FragmentProductosVendedorBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ProductoFilaAdapter

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentProductosVendedorBinding.bind(view)
        val vendedor = Repositorio.usuarioActual ?: return
        b.tvTaller.text = vendedor.nombreTaller.ifBlank { vendedor.nombres }

        adaptador = ProductoFilaAdapter(
            productos = emptyList(),
            mostrarVendedor = false,
            alTocar = { abrirFormulario(it) },
            alEditar = { abrirFormulario(it) },
            alEliminar = { confirmarEliminacion(it) }
        )
        b.rvProductos.layoutManager = LinearLayoutManager(requireContext())
        b.rvProductos.adapter = adaptador

        // El FAB extendido se contrae al desplazar la lista y se expande al volver arriba
        b.rvProductos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            /** Contrae el FAB al bajar y lo expande al subir. */
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) b.fabNuevo.shrink() else if (dy < 0) b.fabNuevo.extend()
            }
        })

        b.fabNuevo.setOnClickListener { abrirFormulario(null) }
        b.etBuscar.doAfterTextChanged { refrescar() }
        b.grupoEstado.setOnCheckedStateChangeListener { _, _ -> refrescar() }

        // Escucha en tiempo real: la lista cambia cuando Firestore confirma una escritura
        // o cuando el Administrador aprueba/rechaza un producto
        Repositorio.observar(viewLifecycleOwner) { refrescar() }
    }

    /** Al volver a la pantalla se recalculan los datos visibles. */
    override fun onResume() {
        super.onResume()
        refrescar()
    }

    /** Recalcula indicadores y aplica búsqueda y filtro de estado sobre los productos del vendedor. */
    private fun refrescar() {
        val vista = _b ?: return
        val vendedor = Repositorio.usuarioActual ?: return
        val todos = Repositorio.productosDelVendedor(vendedor.id)

        // Indicadores
        vista.tvKpiVentas.text = Formato.cop(Repositorio.ventasDelVendedor(vendedor.id))
        vista.tvKpiProductos.text = todos.size.toString()
        vista.tvKpiRevision.text = todos.count { it.estado == EstadoProducto.EN_REVISION }.toString()

        // Filtros
        val texto = vista.etBuscar.text?.toString().orEmpty().trim()
        val estado = when (vista.grupoEstado.checkedChipId) {
            R.id.chipPublicados -> EstadoProducto.PUBLICADO
            R.id.chipRevision -> EstadoProducto.EN_REVISION
            R.id.chipAgotados -> EstadoProducto.AGOTADO
            else -> null
        }
        val lista = todos.filter {
            (estado == null || it.estado == estado) && (texto.isEmpty() || it.nombre.contains(texto, ignoreCase = true))
        }
        adaptador.actualizar(lista)
        vista.tvVacio.isVisible = lista.isEmpty()
    }

    /** Abre el formulario vacío (crear) o con un producto (editar). */
    private fun abrirFormulario(producto: Producto?) {
        val intent = Intent(requireContext(), ProductoFormActivity::class.java)
        if (producto != null) intent.putExtra(ProductoFormActivity.EXTRA_ID, producto.id)
        startActivity(intent)
    }

    /** Eliminación (D) con diálogo de confirmación. */
    private fun confirmarEliminacion(producto: Producto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.eliminar_producto_titulo)
            .setMessage(getString(R.string.eliminar_producto_mensaje, producto.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarProducto(producto) { resultado ->
                    val vista = _b ?: return@eliminarProducto
                    resultado
                        .onSuccess { Snackbar.make(vista.root, R.string.producto_eliminado, Snackbar.LENGTH_SHORT).show() }
                        .onFailure { Snackbar.make(vista.root, requireContext().mensajeError(it), Snackbar.LENGTH_LONG).show() }
                }
            }
            .show()
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
