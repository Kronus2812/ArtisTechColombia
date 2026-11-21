package com.artistech.colombia.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.concurrent.Executors

/** Resultado de la geolocalización: coordenadas y dirección legible obtenida por geocodificación inversa. */
data class DatosUbicacion(
    val latitud: Double,
    val longitud: Double,
    val direccion: String,
    val ciudad: String
)

/**
 * Geolocalización con las APIs de Google Play Services (RF-17):
 * - FusedLocationProviderClient obtiene la posición combinando GPS, Wi-Fi y red móvil.
 * - Geocoder convierte las coordenadas en una dirección (geocodificación inversa).
 * - Google Maps muestra el punto de entrega mediante un Intent "geo:".
 */
object Ubicacion {

    /** Hilo de fondo para la geocodificación, que hace una consulta de red. */
    private val hiloFondo = Executors.newSingleThreadExecutor()
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    /** Permisos que se solicitan juntos: el usuario puede conceder ubicación precisa o aproximada. */
    val PERMISOS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** Indica si la app tiene al menos el permiso de ubicación aproximada. */
    fun tienePermiso(contexto: Context): Boolean =
        PERMISOS.any { ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Obtiene la ubicación actual y su dirección. Llama a [alObtener] con los datos o a [alFallar]
     * con un mensaje (por ejemplo, si el GPS del teléfono está apagado).
     */
    @SuppressLint("MissingPermission") // el permiso se verifica con tienePermiso() antes de pedir la ubicación
    fun obtener(contexto: Context, alObtener: (DatosUbicacion) -> Unit, alFallar: (String) -> Unit) {
        if (!tienePermiso(contexto)) {
            alFallar(contexto.getString(com.artistech.colombia.R.string.gps_denegado))
            return
        }
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    geocodificar(contexto, ubicacion, alObtener)
                } else {
                    // Sin lectura nueva: se intenta con la última ubicación conocida
                    cliente.lastLocation
                        .addOnSuccessListener { ultima ->
                            if (ultima != null) geocodificar(contexto, ultima, alObtener)
                            else alFallar(contexto.getString(com.artistech.colombia.R.string.gps_apagado))
                        }
                        .addOnFailureListener { alFallar(contexto.getString(com.artistech.colombia.R.string.gps_apagado)) }
                }
            }
            .addOnFailureListener { alFallar(it.localizedMessage ?: contexto.getString(com.artistech.colombia.R.string.gps_apagado)) }
    }

    /**
     * Convierte coordenadas en dirección en un hilo de fondo y entrega el resultado en el hilo
     * principal. Si el geocodificador no responde, la dirección queda con las coordenadas.
     */
    private fun geocodificar(contexto: Context, ubicacion: Location, alObtener: (DatosUbicacion) -> Unit) {
        val appContext = contexto.applicationContext
        hiloFondo.execute {
            var direccion = String.format(Locale.US, "Lat %.5f, Lng %.5f", ubicacion.latitude, ubicacion.longitude)
            var ciudad = ""
            try {
                if (Geocoder.isPresent()) {
                    @Suppress("DEPRECATION") // la versión síncrona funciona en todas las API desde la 26
                    val resultados = Geocoder(appContext, Locale.forLanguageTag("es-CO"))
                        .getFromLocation(ubicacion.latitude, ubicacion.longitude, 1)
                    resultados?.firstOrNull()?.let { lugar ->
                        direccion = lugar.getAddressLine(0) ?: direccion
                        ciudad = lugar.locality ?: lugar.subAdminArea ?: lugar.adminArea ?: ""
                    }
                }
            } catch (e: Exception) {
                // Sin conexión o servicio no disponible: se conservan las coordenadas
            }
            val datos = DatosUbicacion(ubicacion.latitude, ubicacion.longitude, direccion, ciudad)
            hiloPrincipal.post { alObtener(datos) }
        }
    }

    /** Abre Google Maps (o la app de mapas instalada) centrada en las coordenadas indicadas. */
    fun abrirEnMapa(contexto: Context, latitud: Double, longitud: Double, etiqueta: String) {
        val uri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(etiqueta)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            contexto.startActivity(intent)
        } catch (e: Exception) {
            // Sin app de mapas: se abre Google Maps en el navegador
            contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=$latitud,$longitud")))
        }
    }
}
