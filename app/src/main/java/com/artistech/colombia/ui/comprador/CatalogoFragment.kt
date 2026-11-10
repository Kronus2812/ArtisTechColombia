package com.artistech.colombia.ui.comprador

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentCatalogoBinding

/** Catálogo de productos con búsqueda en tiempo real y filtro por categoría. */
class CatalogoFragment : Fragment(R.layout.fragment_catalogo) {

    private var _b: FragmentCatalogoBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ProductoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentCatalogoBinding.bind(view)

        adaptador = ProductoAdapter(emptyList()) { producto ->
            startActivity(
                Intent(requireContext(), DetalleProductoActivity::class.java)
                    .putExtra(DetalleProductoActivity.EXTRA_ID, producto.id)
            )
        }
        b.rvCatalogo.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvCatalogo.adapter = adaptador

        // Aplica el filtro que llegó desde el Inicio
        val contenedor = activity as? CompradorActivity
        contenedor?.categoriaPendiente?.let { b.grupoFiltro.check(idChipDe(it)) }
        if (contenedor?.enfocarBusqueda == true) {
            b.etBuscar.requestFocus()
            val teclado = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            b.etBuscar.post { teclado.showSoftInput(b.etBuscar, InputMethodManager.SHOW_IMPLICIT) }
        }
        contenedor?.categoriaPendiente = null
        contenedor?.enfocarBusqueda = false

        b.grupoFiltro.setOnCheckedStateChangeListener { _, _ -> filtrar() }
        b.etBuscar.doAfterTextChanged { filtrar() }
        filtrar()
    }

    private fun categoriaSeleccionada(): Categoria? = when (b.grupoFiltro.checkedChipId) {
        R.id.chipTejidos -> Categoria.TEJIDOS
        R.id.chipCeramica -> Categoria.CERAMICA
        R.id.chipCesteria -> Categoria.CESTERIA
        R.id.chipJoyeria -> Categoria.JOYERIA
        R.id.chipMadera -> Categoria.MADERA
        R.id.chipDiseno -> Categoria.DISENO
        else -> null
    }

    private fun idChipDe(categoria: Categoria): Int = when (categoria) {
        Categoria.TEJIDOS -> R.id.chipTejidos
        Categoria.CERAMICA -> R.id.chipCeramica
        Categoria.CESTERIA -> R.id.chipCesteria
        Categoria.JOYERIA -> R.id.chipJoyeria
        Categoria.MADERA -> R.id.chipMadera
        Categoria.DISENO -> R.id.chipDiseno
    }

    private fun filtrar() {
        val resultados = Repositorio.buscarProductos(b.etBuscar.text?.toString().orEmpty(), categoriaSeleccionada())
        adaptador.actualizar(resultados)
        b.tvResultados.text = resources.getQuantityString(R.plurals.resultados, resultados.size, resultados.size)
        b.vistaVacia.isVisible = resultados.isEmpty()
        b.rvCatalogo.isVisible = resultados.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
