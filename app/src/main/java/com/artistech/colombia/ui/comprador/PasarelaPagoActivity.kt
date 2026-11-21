package com.artistech.colombia.ui.comprador

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.DatosPago
import com.artistech.colombia.data.Pedido
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityPasarelaPagoBinding
import com.artistech.colombia.util.Biometria
import com.artistech.colombia.util.Formato
import com.artistech.colombia.util.Ubicacion
import com.artistech.colombia.util.mensajeError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Pasarela de pagos (RF-13 y RF-14).
 * 1. Dirección de entrega: se detecta con GPS (geolocalización) o se escribe manualmente.
 * 2. Método: ePayco (tarjeta, PSE, Nequi, Daviplata) o pago contra entrega.
 * 3. Pago: se abre el checkout de ePayco (EpaycoActivity); con la transacción confirmada se
 *    registran los pedidos en Firestore mediante una transacción que descuenta el inventario.
 */
class PasarelaPagoActivity : AppCompatActivity() {

    private lateinit var b: ActivityPasarelaPagoBinding

    // Dirección y coordenadas de entrega elegidas
    private var direccion = ""
    private var latitud: Double? = null
    private var longitud: Double? = null

    /** Permisos de ubicación; si se conceden, se detecta la dirección de inmediato. */
    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        if (respuesta.values.any { it }) detectarUbicacion()
        else Snackbar.make(b.root, R.string.gps_denegado, Snackbar.LENGTH_LONG).show()
    }

    /** Recibe el resultado del checkout de ePayco (ya validado contra el servicio de ePayco). */
    private val checkoutEpayco = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val datos = r.data
        if (r.resultCode != Activity.RESULT_OK || datos == null) {
            Snackbar.make(b.root, R.string.pago_cancelado, Snackbar.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        val codigo = datos.getIntExtra(EpaycoActivity.EXTRA_CODIGO, 0)
        val estado = datos.getStringExtra(EpaycoActivity.EXTRA_ESTADO).orEmpty()
        val referencia = datos.getStringExtra(EpaycoActivity.EXTRA_REFERENCIA).orEmpty()
        val franquicia = datos.getStringExtra(EpaycoActivity.EXTRA_FRANQUICIA).orEmpty()
        if (codigo == 1 || codigo == 3) {
            // 1 = Aceptada, 3 = Pendiente: se registra el pedido con la referencia de ePayco
            val metodo = if (franquicia.isBlank()) "ePayco" else "ePayco · $franquicia"
            registrarPedidos(metodo, estado, referencia)
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pago_rechazado_titulo)
                .setMessage(getString(R.string.pago_rechazado_mensaje, estado, referencia))
                .setPositiveButton(R.string.entendido, null)
                .show()
        }
    }

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repositorio.carrito.isEmpty()) {
            finish()
            return
        }
        b = ActivityPasarelaPagoBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnCambiarDireccion.setOnClickListener { editarDireccion() }
        b.btnUsarGps.setOnClickListener {
            if (Ubicacion.tienePermiso(this)) detectarUbicacion() else permisoUbicacion.launch(Ubicacion.PERMISOS)
        }
        b.btnVerMapa.setOnClickListener {
            val lat = latitud ?: return@setOnClickListener
            val lng = longitud ?: return@setOnClickListener
            Ubicacion.abrirEnMapa(this, lat, lng, getString(R.string.mapa_entrega))
        }
        b.grupoMetodo.setOnCheckedChangeListener { _, _ -> mostrarInfoMetodo() }

        // Dirección inicial: la última guardada en el perfil; si no hay, se intenta con el GPS
        Repositorio.usuarioActual?.let { u ->
            if (u.direccion.isNotBlank()) mostrarDireccion(u.direccion, u.latitud, u.longitud, desdeGps = u.latitud != null)
        }
        if (direccion.isBlank() && Ubicacion.tienePermiso(this)) detectarUbicacion()

        mostrarInfoMetodo()
        mostrarResumen()
        b.btnPagar.setOnClickListener { if (validar()) procesarPago() }
        b.btnPagarHuella.setOnClickListener { pagarConHuella() }
    }

    /** Totales del carrito (subtotal, descuento, envío y total). */
    private fun mostrarResumen() {
        b.tvEtiquetaProductos.text = resources.getQuantityString(
            R.plurals.productos_resumen, Repositorio.unidadesEnCarrito(), Repositorio.unidadesEnCarrito()
        )
        b.tvSubtotal.text = Formato.cop(Repositorio.subtotal())
        b.tvDescuento.text = getString(R.string.valor_negativo, Formato.cop(Repositorio.descuento()))
        b.tvEnvio.text = Formato.cop(Repositorio.envio())
        b.tvTotal.text = Formato.cop(Repositorio.total())
    }

    /** Texto informativo según el método elegido. */
    private fun mostrarInfoMetodo() {
        b.tvInfoMetodo.setText(
            if (b.grupoMetodo.checkedRadioButtonId == R.id.rbEfectivo) R.string.pago_info_contra_entrega
            else R.string.pago_info_epayco
        )
    }

    /** Geolocalización: obtiene la posición actual y su dirección para la entrega. */
    private fun detectarUbicacion() {
        b.tvDireccionDetalle.setText(R.string.gps_buscando)
        b.btnUsarGps.isEnabled = false
        Ubicacion.obtener(
            applicationContext,
            alObtener = { datos ->
                b.btnUsarGps.isEnabled = true
                mostrarDireccion(datos.direccion, datos.latitud, datos.longitud, desdeGps = true)
                Repositorio.guardarUbicacion(datos.direccion, datos.latitud, datos.longitud)
            },
            alFallar = { mensaje ->
                b.btnUsarGps.isEnabled = true
                b.tvDireccionDetalle.setText(R.string.pago_direccion_detalle)
                Snackbar.make(b.root, mensaje, Snackbar.LENGTH_LONG).show()
            }
        )
    }

    /** Muestra la dirección elegida y habilita el botón del mapa si hay coordenadas. */
    private fun mostrarDireccion(texto: String, lat: Double?, lng: Double?, desdeGps: Boolean) {
        direccion = texto
        latitud = lat
        longitud = lng
        b.tvDireccion.text = texto
        b.tvDireccionDetalle.text = if (desdeGps && lat != null && lng != null) {
            getString(R.string.pago_direccion_gps, Formato.coordenadas(lat, lng))
        } else {
            getString(R.string.pago_direccion_manual)
        }
        b.btnVerMapa.isEnabled = lat != null && lng != null
    }

    /** Diálogo para escribir o corregir la dirección a mano (por ejemplo, el apartamento). */
    private fun editarDireccion() {
        val campo = TextInputEditText(this).apply {
            setText(direccion)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val contenedor = TextInputLayout(this).apply {
            hint = getString(R.string.pago_direccion)
            addView(campo)
        }
        val marco = FrameLayout(this).apply {
            val m = resources.getDimensionPixelSize(R.dimen.espacio_l)
            setPadding(m, m / 2, m, 0)
            addView(contenedor)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pago_editar_direccion)
            .setView(marco)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val texto = campo.text?.toString()?.trim().orEmpty()
                // Se conservan las coordenadas del GPS: el texto solo precisa la dirección
                if (texto.isNotEmpty()) mostrarDireccion(texto, latitud, longitud, desdeGps = false)
            }
            .show()
    }

    /** Valida dirección y aceptación de términos antes de pagar. */
    private fun validar(): Boolean {
        if (direccion.isBlank()) {
            Snackbar.make(b.root, R.string.error_direccion, Snackbar.LENGTH_LONG).show()
            return false
        }
        if (!b.cbTerminos.isChecked) {
            Snackbar.make(b.root, R.string.error_terminos, Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    /** Confirma el pago con la huella (RF-03) antes de continuar. */
    private fun pagarConHuella() {
        if (!validar()) return
        if (!Biometria.disponible(this)) {
            Snackbar.make(b.root, R.string.huella_no_disponible, Snackbar.LENGTH_LONG).show()
            return
        }
        Biometria.autenticar(
            actividad = this,
            titulo = getString(R.string.huella_titulo_pago),
            subtitulo = getString(R.string.huella_subtitulo_pago, Formato.cop(Repositorio.total())),
            alAutenticar = { procesarPago() },
            alFallar = { mensaje -> Snackbar.make(b.root, mensaje, Snackbar.LENGTH_LONG).show() }
        )
    }

    /** Contra entrega registra el pedido directamente; ePayco abre el checkout seguro. */
    private fun procesarPago() {
        if (b.grupoMetodo.checkedRadioButtonId == R.id.rbEfectivo) {
            registrarPedidos(getString(R.string.metodo_contra_entrega_corto), getString(R.string.estado_pago_contra_entrega), "")
            return
        }
        val usuario = Repositorio.usuarioActual ?: return
        val factura = "AT-" + System.currentTimeMillis().toString().takeLast(8)
        val descripcion = Repositorio.carrito.joinToString(", ") { "${it.producto.nombre} x${it.cantidad}" }.take(120)
        checkoutEpayco.launch(
            Intent(this, EpaycoActivity::class.java)
                .putExtra(EpaycoActivity.EXTRA_FACTURA, factura)
                .putExtra(EpaycoActivity.EXTRA_DESCRIPCION, descripcion)
                .putExtra(EpaycoActivity.EXTRA_MONTO, Repositorio.total())
                .putExtra(EpaycoActivity.EXTRA_EMAIL, usuario.email)
                .putExtra(EpaycoActivity.EXTRA_NOMBRE, usuario.nombres)
                .putExtra(EpaycoActivity.EXTRA_DIRECCION, direccion)
                .putExtra(EpaycoActivity.EXTRA_TELEFONO, usuario.telefono)
        )
    }

    /** Registra los pedidos en Firestore (transacción) y abre la pantalla de confirmación. */
    private fun registrarPedidos(metodo: String, estadoPago: String, referencia: String) {
        b.capaCarga.isVisible = true
        val total = Repositorio.total()
        val pago = DatosPago(metodo, estadoPago, referencia, direccion, latitud, longitud)
        Repositorio.confirmarCompra(pago) { resultado ->
            b.capaCarga.isVisible = false
            resultado
                .onSuccess { pedidos -> abrirConfirmacion(pedidos, total, pago) }
                .onFailure { error -> Snackbar.make(b.root, mensajeError(error), Snackbar.LENGTH_LONG).show() }
        }
    }

    /** Paso 3: pantalla de pedido confirmado con los datos del pago. */
    private fun abrirConfirmacion(pedidos: List<Pedido>, total: Long, pago: DatosPago) {
        startActivity(
            Intent(this, PedidoConfirmadoActivity::class.java)
                .putExtra(PedidoConfirmadoActivity.EXTRA_CODIGO, pedidos.joinToString(", ") { it.codigo })
                .putExtra(PedidoConfirmadoActivity.EXTRA_TOTAL, total)
                .putExtra(PedidoConfirmadoActivity.EXTRA_METODO, pago.metodo)
                .putExtra(PedidoConfirmadoActivity.EXTRA_ESTADO_PAGO, pago.estado)
                .putExtra(PedidoConfirmadoActivity.EXTRA_REFERENCIA, pago.referencia)
                .putExtra(PedidoConfirmadoActivity.EXTRA_DIRECCION, pago.direccion)
        )
        finish()
    }
}
