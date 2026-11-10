package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentCarritoBinding
import com.artistech.colombia.util.Formato
import com.google.android.material.snackbar.Snackbar

/** CRUD de Carrito con cupón de descuento y resumen de costos. */
class CarritoFragment : Fragment(R.layout.fragment_carrito) {

    private var _b: FragmentCarritoBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: CarritoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentCarritoBinding.bind(view)

        adaptador = CarritoAdapter(
            alCambiar = { actualizarResumen() },
            alSuperarStock = { item ->
                Snackbar.make(b.root, getString(R.string.detalle_max_stock, item.producto.stock), Snackbar.LENGTH_SHORT).show()
            }
        )
        b.rvCarrito.layoutManager = LinearLayoutManager(requireContext())
        b.rvCarrito.adapter = adaptador

        b.btnAplicarCupon.setOnClickListener { aplicarCupon() }
        b.btnContinuar.setOnClickListener {
            startActivity(Intent(requireContext(), PasarelaPagoActivity::class.java))
        }
        b.btnExplorar.setOnClickListener { (activity as? CompradorActivity)?.abrirCatalogo(null) }
    }

    override fun onResume() {
        super.onResume()
        adaptador.refrescar()
    }

    private fun aplicarCupon() {
        val codigo = b.etCupon.text?.toString().orEmpty()
        if (Repositorio.aplicarCupon(codigo)) {
            b.tilCupon.error = null
            b.tilCupon.helperText = getString(R.string.cupon_aplicado)
        } else {
            b.tilCupon.helperText = null
            b.tilCupon.error = getString(R.string.error_cupon)
        }
        actualizarResumen()
    }

    private fun actualizarResumen() {
        val vista = _b ?: return
        val vacio = Repositorio.carrito.isEmpty()
        vista.vistaVacia.isVisible = vacio
        vista.contenido.isVisible = !vacio
        vista.tvTitulo.text = getString(R.string.carrito_titulo_conteo, Repositorio.unidadesEnCarrito())
        vista.tvSubtotal.text = Formato.cop(Repositorio.subtotal())
        vista.tvDescuento.text = getString(R.string.valor_negativo, Formato.cop(Repositorio.descuento()))
        vista.tvEnvio.text = Formato.cop(Repositorio.envio())
        vista.tvIva.text = Formato.cop(Repositorio.ivaIncluido())
        vista.tvTotal.text = Formato.cop(Repositorio.total())
        (activity as? CompradorActivity)?.actualizarInsigniaCarrito()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
