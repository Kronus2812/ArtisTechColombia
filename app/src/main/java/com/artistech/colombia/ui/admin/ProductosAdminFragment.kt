package com.artistech.colombia.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoProducto
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentProductosAdminBinding
import com.artistech.colombia.ui.comun.ProductoFilaAdapter
import com.artistech.colombia.ui.vendedor.ProductoFormActivity
import com.artistech.colombia.util.Formato
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Gestión de productos y moderación del catálogo (RF-23). */
class ProductosAdminFragment : Fragment(R.layout.fragment_productos_admin) {

    private var _b: FragmentProductosAdminBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ProductoFilaAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentProductosAdminBinding.bind(view)

        adaptador = ProductoFilaAdapter(
            productos = emptyList(),
            mostrarVendedor = true,
            alTocar = { moderar(it) },
            alEditar = { editar(it) },
            alEliminar = { confirmarEliminacion(it) }
        )
        b.rvProductos.layoutManager = LinearLayoutManager(requireContext())
        b.rvProductos.adapter = adaptador
        b.grupoEstado.setOnCheckedStateChangeListener { _, _ -> refrescar() }
    }

    override fun onResume() {
        super.onResume()
        refrescar()
    }

    private fun refrescar() {
        val vista = _b ?: return
        val todos = Repositorio.productos
        vista.tvKpiTotal.text = todos.size.toString()
        vista.tvKpiRevision.text = todos.count { it.estado == EstadoProducto.EN_REVISION }.toString()
        vista.tvKpiPublicados.text = todos.count { it.estado == EstadoProducto.PUBLICADO }.toString()

        val filtro = when (vista.grupoEstado.checkedChipId) {
            R.id.chipRevision -> EstadoProducto.EN_REVISION
            R.id.chipPublicados -> EstadoProducto.PUBLICADO
            R.id.chipAgotados -> EstadoProducto.AGOTADO
            R.id.chipRechazados -> EstadoProducto.RECHAZADO
            else -> null
        }
        val lista = if (filtro == null) todos.toList() else todos.filter { it.estado == filtro }
        adaptador.actualizar(lista)
        vista.tvVacio.isVisible = lista.isEmpty()
    }

    /** Diálogo de moderación: aprobar o rechazar la publicación. */
    private fun moderar(producto: Producto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(producto.nombre)
            .setMessage(
                getString(
                    R.string.moderar_mensaje,
                    Repositorio.nombreVendedor(producto.vendedorId),
                    producto.region,
                    Formato.cop(producto.precio),
                    producto.estado.etiqueta
                )
            )
            .setPositiveButton(R.string.aprobar) { _, _ ->
                producto.estado = if (producto.stock == 0) EstadoProducto.AGOTADO else EstadoProducto.PUBLICADO
                refrescar()
                Snackbar.make(b.root, R.string.producto_aprobado, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.rechazar) { _, _ ->
                producto.estado = EstadoProducto.RECHAZADO
                refrescar()
                Snackbar.make(b.root, R.string.producto_rechazado, Snackbar.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.cancelar, null)
            .show()
    }

    private fun editar(producto: Producto) {
        startActivity(
            Intent(requireContext(), ProductoFormActivity::class.java)
                .putExtra(ProductoFormActivity.EXTRA_ID, producto.id)
        )
    }

    private fun confirmarEliminacion(producto: Producto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.eliminar_producto_titulo)
            .setMessage(getString(R.string.eliminar_producto_mensaje, producto.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                Repositorio.eliminarProducto(producto.id)
                refrescar()
                Snackbar.make(b.root, R.string.producto_eliminado, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
