package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityPasarelaPagoBinding
import com.artistech.colombia.util.Biometria
import com.artistech.colombia.util.Formato
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Pasarela de pagos (RF-13 y RF-14). La interfaz valida los datos; en producción la
 * tokenización la haría el SDK de la pasarela certificada PCI DSS (la app no guarda tarjetas).
 */
class PasarelaPagoActivity : AppCompatActivity() {

    private lateinit var b: ActivityPasarelaPagoBinding
    private val manejador = Handler(Looper.getMainLooper())
    private var direccionElegida = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repositorio.carrito.isEmpty()) {
            finish()
            return
        }
        b = ActivityPasarelaPagoBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        b.btnCambiarDireccion.setOnClickListener { elegirDireccion() }

        val bancos = resources.getStringArray(R.array.bancos_pse)
        b.actBanco.setAdapter(ArrayAdapter(this, R.layout.item_desplegable, bancos))

        b.grupoMetodo.setOnCheckedChangeListener { _, _ -> mostrarFormularioDelMetodo() }
        mostrarFormularioDelMetodo()
        mostrarResumen()

        b.btnPagar.setOnClickListener { if (validar()) procesarPago() }
        b.btnPagarHuella.setOnClickListener { pagarConHuella() }
    }

    private fun mostrarResumen() {
        b.tvEtiquetaProductos.text = resources.getQuantityString(
            R.plurals.productos_resumen, Repositorio.unidadesEnCarrito(), Repositorio.unidadesEnCarrito()
        )
        b.tvSubtotal.text = Formato.cop(Repositorio.subtotal())
        b.tvDescuento.text = getString(R.string.valor_negativo, Formato.cop(Repositorio.descuento()))
        b.tvEnvio.text = Formato.cop(Repositorio.envio())
        b.tvTotal.text = Formato.cop(Repositorio.total())
    }

    private fun mostrarFormularioDelMetodo() {
        val id = b.grupoMetodo.checkedRadioButtonId
        b.formTarjeta.isVisible = id == R.id.rbTarjeta
        b.tilBanco.isVisible = id == R.id.rbPse
        b.tilCelularPago.isVisible = id == R.id.rbNequi
        b.tvInfoEfectivo.isVisible = id == R.id.rbEfectivo
    }

    private fun metodoSeleccionado(): String = when (b.grupoMetodo.checkedRadioButtonId) {
        R.id.rbPse -> getString(R.string.metodo_pse_corto)
        R.id.rbNequi -> getString(R.string.metodo_billetera_corto)
        R.id.rbEfectivo -> getString(R.string.metodo_efectivo_corto)
        else -> getString(R.string.metodo_tarjeta_corto)
    }

    private fun elegirDireccion() {
        val direcciones = resources.getStringArray(R.array.direcciones_envio)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pago_direccion)
            .setSingleChoiceItems(direcciones, direccionElegida) { dialogo, posicion ->
                direccionElegida = posicion
                b.tvDireccion.text = direcciones[posicion]
                dialogo.dismiss()
            }
            .show()
    }

    /** Validación de los campos según el método de pago elegido. */
    private fun validar(): Boolean {
        listOf(b.tilNumeroTarjeta, b.tilTitular, b.tilVencimiento, b.tilCvv, b.tilBanco, b.tilCelularPago)
            .forEach { it.error = null }
        var valido = true
        when (b.grupoMetodo.checkedRadioButtonId) {
            R.id.rbTarjeta -> {
                val numero = b.etNumeroTarjeta.text?.toString().orEmpty().replace(" ", "")
                if (numero.length !in 15..16) { b.tilNumeroTarjeta.error = getString(R.string.error_tarjeta); valido = false }
                if (b.etTitular.text.isNullOrBlank()) { b.tilTitular.error = getString(R.string.error_obligatorio); valido = false }
                val vence = b.etVencimiento.text?.toString().orEmpty()
                if (!Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(vence)) { b.tilVencimiento.error = getString(R.string.error_vencimiento); valido = false }
                if ((b.etCvv.text?.length ?: 0) !in 3..4) { b.tilCvv.error = getString(R.string.error_cvv); valido = false }
            }
            R.id.rbPse -> if (b.actBanco.text.isNullOrBlank()) { b.tilBanco.error = getString(R.string.error_banco); valido = false }
            R.id.rbNequi -> {
                val celular = b.etCelularPago.text?.toString().orEmpty()
                if (celular.length != 10 || !celular.startsWith("3")) { b.tilCelularPago.error = getString(R.string.error_telefono); valido = false }
            }
        }
        if (!b.cbTerminos.isChecked) {
            Snackbar.make(b.root, R.string.error_terminos, Snackbar.LENGTH_LONG).show()
            valido = false
        }
        return valido
    }

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

    /** Simula la respuesta de la pasarela y crea el pedido. */
    private fun procesarPago() {
        b.capaCarga.isVisible = true
        val total = Repositorio.total()
        val metodo = metodoSeleccionado()
        manejador.postDelayed({
            val pedidos = Repositorio.confirmarCompra(metodo)
            startActivity(
                Intent(this, PedidoConfirmadoActivity::class.java)
                    .putExtra(PedidoConfirmadoActivity.EXTRA_CODIGO, pedidos.joinToString(", ") { it.codigo })
                    .putExtra(PedidoConfirmadoActivity.EXTRA_TOTAL, total)
                    .putExtra(PedidoConfirmadoActivity.EXTRA_METODO, metodo)
            )
            finish()
        }, 1800)
    }

    override fun onDestroy() {
        manejador.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
