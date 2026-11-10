package com.artistech.colombia.ui.comprador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentInicioBinding
import com.google.android.material.snackbar.Snackbar

/** Pantalla de Inicio del Comprador. */
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    private var _b: FragmentInicioBinding? = null
    private val b get() = _b!!

    /** Solicitud del permiso de ubicación en tiempo de ejecución (RF-17). */
    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        actualizarTarjetaGps(concedido)
        if (!concedido) {
            Snackbar.make(b.root, R.string.gps_denegado, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentInicioBinding.bind(view)
        val contenedor = activity as? CompradorActivity

        val nombre = Repositorio.usuarioActual?.nombres?.substringBefore(" ").orEmpty()
        b.tvSaludo.text = getString(R.string.inicio_saludo, nombre)

        b.barraBusqueda.setOnClickListener { contenedor?.abrirCatalogo(null, enfocar = true) }
        b.btnVerTodo.setOnClickListener { contenedor?.abrirCatalogo(null) }
        b.btnNotificaciones.setOnClickListener {
            Snackbar.make(b.root, R.string.inicio_sin_notificaciones, Snackbar.LENGTH_SHORT).show()
        }

        // Chips de categoría: abren el catálogo ya filtrado
        mapOf(
            b.chipTejidos to Categoria.TEJIDOS,
            b.chipCeramica to Categoria.CERAMICA,
            b.chipCesteria to Categoria.CESTERIA,
            b.chipJoyeria to Categoria.JOYERIA,
            b.chipMadera to Categoria.MADERA,
            b.chipDiseno to Categoria.DISENO
        ).forEach { (chip, categoria) ->
            chip.setOnClickListener { contenedor?.abrirCatalogo(categoria) }
        }

        // Geolocalización
        val concedido = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        actualizarTarjetaGps(concedido)
        b.btnActivarGps.setOnClickListener {
            permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Destacados en cuadrícula de 2 columnas
        b.rvDestacados.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvDestacados.adapter = ProductoAdapter(Repositorio.destacados()) { abrirDetalle(it) }
    }

    private fun actualizarTarjetaGps(activo: Boolean) {
        val vista = _b ?: return
        vista.tvGpsTitulo.setText(if (activo) R.string.gps_titulo_activo else R.string.gps_titulo_inactivo)
        vista.tvGpsDetalle.setText(if (activo) R.string.gps_detalle_activo else R.string.gps_detalle_inactivo)
        vista.btnActivarGps.visibility = if (activo) View.GONE else View.VISIBLE
    }

    private fun abrirDetalle(producto: Producto) {
        startActivity(
            Intent(requireContext(), DetalleProductoActivity::class.java)
                .putExtra(DetalleProductoActivity.EXTRA_ID, producto.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
