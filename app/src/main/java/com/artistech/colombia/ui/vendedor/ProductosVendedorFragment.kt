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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Panel de productos del Vendedor: CRUD completo (crear, listar, editar y eliminar). */
class ProductosVendedorFragment : Fragment(R.layout.fragment_productos_vendedor) {

    private var _b: FragmentProductosVendedorBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ProductoFilaAdapter

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
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) b.fabNuevo.shrink() else if (dy < 0) b.fabNuevo.extend()
            }
        })

        b.fabNuevo.setOnClickListener { abrirFormulario(null) }
        b.etBuscar.doAfterTextChanged { refrescar() }
        b.grupoEstado.setOnCheckedStateChangeListener { _, _ -> refrescar() }
    }

    override fun onResume() {
        super.onResume()
        refrescar()
    }

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

    private fun abrirFormulario(producto: Producto?) {
        val intent = Intent(requireContext(), ProductoFormActivity::class.java)
        if (producto != null) intent.putExtra(ProductoFormActivity.EXTRA_ID, producto.id)
        startActivity(intent)
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
