package com.artistech.colombia.ui.comprador

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.artistech.colombia.BuildConfig
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityEpaycoBinding
import com.artistech.colombia.util.PagoEpayco
import com.artistech.colombia.util.ResultadoPago
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Checkout de ePayco dentro de la app.
 *
 * Carga la página generada por [PagoEpayco.paginaCheckout] en un WebView. Cuando ePayco termina,
 * redirige a [PagoEpayco.URL_RESPUESTA] con "ref_payco"; esta pantalla intercepta esa URL,
 * confirma la transacción con el servicio de validación de ePayco y devuelve el resultado a
 * PasarelaPagoActivity.
 */
class EpaycoActivity : AppCompatActivity() {

    private lateinit var b: ActivityEpaycoBinding

    /** Evita procesar dos veces la misma respuesta (ePayco puede redirigir más de una vez). */
    private var respuestaProcesada = false

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    @SuppressLint("SetJavaScriptEnabled") // checkout.js de ePayco necesita JavaScript
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEpaycoBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.setNavigationOnClickListener { confirmarSalida() }
        onBackPressedDispatcher.addCallback(this) {
            if (b.webView.canGoBack()) b.webView.goBack() else confirmarSalida()
        }

        // Configuración del WebView para el checkout
        with(b.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false) // las ventanas nuevas se abren en el mismo WebView
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(b.webView, true)
        b.webView.addJavascriptInterface(PuenteJs(), "Android")
        b.webView.webChromeClient = object : WebChromeClient() {
            /** Muestra la barra de progreso mientras carga la página de ePayco. */
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                b.progreso.isVisible = newProgress < 100
            }
        }
        b.webView.webViewClient = object : WebViewClient() {
            /** Cada navegación pasa por interceptar() para detectar la respuesta de ePayco. */
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                interceptar(request.url.toString())

            /** Respaldo: algunas redirecciones solo se detectan al iniciar la carga de la página. */
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (interceptar(url)) view.stopLoading()
            }
        }
        cargarCheckout()
    }

    /** Genera la página del checkout con los datos de la compra y la carga en el WebView. */
    private fun cargarCheckout() {
        val html = PagoEpayco.paginaCheckout(
            llavePublica = BuildConfig.EPAYCO_PUBLIC_KEY,
            pruebas = BuildConfig.EPAYCO_PRUEBAS,
            factura = intent.getStringExtra(EXTRA_FACTURA).orEmpty(),
            descripcion = intent.getStringExtra(EXTRA_DESCRIPCION).orEmpty(),
            monto = intent.getLongExtra(EXTRA_MONTO, 0L),
            email = intent.getStringExtra(EXTRA_EMAIL).orEmpty(),
            nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty(),
            direccion = intent.getStringExtra(EXTRA_DIRECCION).orEmpty(),
            telefono = intent.getStringExtra(EXTRA_TELEFONO).orEmpty(),
            uid = Repositorio.usuarioActual?.id.orEmpty()
        )
        b.webView.loadDataWithBaseURL("https://artistech.app/checkout", html, "text/html", "utf-8", null)
    }

    /**
     * Revisa cada URL que el WebView va a abrir:
     * - URL de respuesta de ePayco: se extrae ref_payco y se valida la transacción.
     * - Enlaces a otras apps (bancos, Nequi, Daviplata): se abren con un Intent.
     * Devuelve true si la app se encargó de la URL.
     */
    private fun interceptar(url: String): Boolean {
        if (url.startsWith(PagoEpayco.URL_RESPUESTA)) {
            if (!respuestaProcesada) {
                respuestaProcesada = true
                val uri = Uri.parse(url)
                val referencia = uri.getQueryParameter("ref_payco") ?: uri.getQueryParameter("x_ref_payco").orEmpty()
                val respaldo = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
                validar(referencia, respaldo)
            }
            return true
        }
        if (!url.startsWith("http")) {
            try {
                startActivity(Intent.parseUri(url, Intent.URI_INTENT_SCHEME))
            } catch (e: Exception) {
                // No hay app para ese enlace: se ignora y el usuario sigue en el checkout
            }
            return true
        }
        return false
    }

    /** Confirma la transacción con ePayco y devuelve el resultado a la pasarela. */
    private fun validar(referencia: String, respaldo: Map<String, String>) {
        b.capaValidando.isVisible = true
        if (referencia.isBlank()) {
            devolver(ResultadoPago("", respaldo["x_cod_response"]?.toIntOrNull() ?: 4,
                respaldo["x_response"] ?: "Fallida", "", "", ""))
            return
        }
        PagoEpayco.validar(referencia, respaldo) { resultado -> devolver(resultado) }
    }

    /** Cierra el checkout entregando los datos de la transacción. */
    private fun devolver(resultado: ResultadoPago) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_CODIGO, resultado.codigo)
                .putExtra(EXTRA_ESTADO, resultado.estado)
                .putExtra(EXTRA_REFERENCIA, resultado.referencia)
                .putExtra(EXTRA_FRANQUICIA, resultado.franquicia)
        )
        finish()
    }

    /** Pregunta antes de abandonar el pago. */
    private fun confirmarSalida() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.epayco_salir_titulo)
            .setMessage(R.string.epayco_salir_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salir) { _, _ ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .show()
    }

    /** Libera los recursos de la pantalla (manejadores pendientes o WebView) al cerrarse. */
    override fun onDestroy() {
        b.webView.destroy()
        super.onDestroy()
    }

    /** Puente JavaScript → Kotlin: la página avisa si no pudo cargar o abrir el checkout. */
    inner class PuenteJs {
        /** Llamado desde JavaScript cuando checkout.js no carga o falla al abrir. */
        @JavascriptInterface
        fun reportarError(detalle: String) {
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                MaterialAlertDialogBuilder(this@EpaycoActivity)
                    .setTitle(R.string.epayco_error_titulo)
                    .setMessage(getString(R.string.epayco_error_mensaje, detalle))
                    .setNegativeButton(R.string.cancelar) { _, _ ->
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setPositiveButton(R.string.reintentar) { _, _ -> cargarCheckout() }
                    .show()
            }
        }
    }

    companion object {
        const val EXTRA_FACTURA = "factura"
        const val EXTRA_DESCRIPCION = "descripcion"
        const val EXTRA_MONTO = "monto"
        const val EXTRA_EMAIL = "email"
        const val EXTRA_NOMBRE = "nombre"
        const val EXTRA_DIRECCION = "direccion"
        const val EXTRA_TELEFONO = "telefono"
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_ESTADO = "estado"
        const val EXTRA_REFERENCIA = "referencia"
        const val EXTRA_FRANQUICIA = "franquicia"
    }
}
