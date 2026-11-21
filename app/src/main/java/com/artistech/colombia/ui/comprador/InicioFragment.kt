package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentInicioBinding
import com.artistech.colombia.util.Ubicacion
import com.google.android.material.snackbar.Snackbar

/**
 * Pantalla de Inicio del Comprador: saludo, búsqueda, tarjeta de geolocalización,
 * categorías y productos destacados (leídos de Firestore en tiempo real).
 */
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    private var _b: FragmentInicioBinding? = null
    private val b get() = _b!!
    private lateinit var adaptadorDestacados: ProductoAdapter

    /** Solicitud de los permisos de ubicación en tiempo de ejecución (RF-17). */
    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        if (respuesta.values.any { it }) {
            detectarUbicacion()
        } else {
            actualizarTarjetaGps()
            _b?.let { Snackbar.make(it.root, R.string.gps_denegado, Snackbar.LENGTH_LONG).show() }
        }
    }

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
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

        // Geolocalización: pedir permiso, detectar la dirección y abrirla en Google Maps
        b.btnActivarGps.setOnClickListener { permisoUbicacion.launch(Ubicacion.PERMISOS) }
        b.tvGpsTitulo.setOnClickListener { abrirMapa() }
        b.tvGpsDetalle.setOnClickListener { abrirMapa() }
        actualizarTarjetaGps()
        if (Ubicacion.tienePermiso(requireContext()) && Repositorio.usuarioActual?.latitud == null) detectarUbicacion()

        // Destacados en cuadrícula de 2 columnas; se refrescan cuando cambia el catálogo
        adaptadorDestacados = ProductoAdapter(Repositorio.destacados()) { abrirDetalle(it) }
        b.rvDestacados.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvDestacados.adapter = adaptadorDestacados
        Repositorio.observar(viewLifecycleOwner) {
            adaptadorDestacados.actualizar(Repositorio.destacados())
            actualizarTarjetaGps()
        }
    }

    /** Obtiene la ubicación actual con el GPS, la convierte en dirección y la guarda en el perfil. */
    private fun detectarUbicacion() {
        _b?.tvGpsDetalle?.setText(R.string.gps_buscando)
        Ubicacion.obtener(
            requireContext().applicationContext,
            alObtener = { datos ->
                Repositorio.guardarUbicacion(datos.direccion, datos.latitud, datos.longitud)
                actualizarTarjetaGps()
            },
            alFallar = { mensaje ->
                actualizarTarjetaGps()
                _b?.let { Snackbar.make(it.root, mensaje, Snackbar.LENGTH_LONG).show() }
            }
        )
    }

    /** Muestra la dirección detectada o invita a activar el GPS. */
    private fun actualizarTarjetaGps() {
        val vista = _b ?: return
        val usuario = Repositorio.usuarioActual
        val conPermiso = Ubicacion.tienePermiso(requireContext())
        if (conPermiso && usuario?.latitud != null && usuario.direccion.isNotBlank()) {
            vista.tvGpsTitulo.text = getString(R.string.gps_titulo_entrega)
            vista.tvGpsDetalle.text = getString(R.string.gps_detalle_direccion, usuario.direccion)
        } else {
            vista.tvGpsTitulo.setText(R.string.gps_titulo_inactivo)
            vista.tvGpsDetalle.setText(R.string.gps_detalle_inactivo)
        }
        vista.btnActivarGps.visibility = if (conPermiso) View.GONE else View.VISIBLE
    }

    /** Abre la ubicación guardada en Google Maps. */
    private fun abrirMapa() {
        val usuario = Repositorio.usuarioActual ?: return
        val lat = usuario.latitud ?: return
        val lng = usuario.longitud ?: return
        Ubicacion.abrirEnMapa(requireContext(), lat, lng, getString(R.string.mapa_mi_ubicacion))
    }

    /** Abre la ficha de detalle del producto tocado. */
    private fun abrirDetalle(producto: Producto) {
        startActivity(
            Intent(requireContext(), DetalleProductoActivity::class.java)
                .putExtra(DetalleProductoActivity.EXTRA_ID, producto.id)
        )
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
