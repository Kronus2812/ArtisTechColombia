package com.artistech.colombia.util

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Resultado de una transacción de ePayco.
 * Códigos de respuesta (x_cod_response): 1 Aceptada, 2 Rechazada, 3 Pendiente, 4 Fallida,
 * 6 Reversada, 7 Retenida, 8 Iniciada, 9 Expirada, 10 Abandonada, 11 Cancelada, 12 Antifraude.
 */
data class ResultadoPago(
    val referencia: String,
    val codigo: Int,
    val estado: String,
    val monto: String,
    val transaccion: String,
    val franquicia: String
) {
    /** El pedido se registra si el pago fue aprobado o quedó pendiente (por ejemplo, PSE en proceso). */
    val aprobadoOPendiente: Boolean get() = codigo == 1 || codigo == 3
}

/**
 * Integración con la pasarela de pagos ePayco (pasarela colombiana certificada PCI DSS).
 *
 * Flujo: la app abre el checkout oficial de ePayco en un WebView (EpaycoActivity); el usuario
 * paga con tarjeta, PSE, Nequi o Daviplata; ePayco redirige a la URL de respuesta con la
 * referencia "ref_payco" y la app CONFIRMA la transacción consultando el servicio de validación
 * de ePayco. La app nunca ve ni guarda los datos de la tarjeta.
 */
object PagoEpayco {

    /** URL de respuesta configurada en el checkout; la app la intercepta, no se carga realmente. */
    const val URL_RESPUESTA = "https://artistech.app/respuesta-pago"
    const val URL_CONFIRMACION = "https://artistech.app/confirmacion-pago"
    private const val URL_VALIDACION = "https://secure.epayco.co/validation/v1/reference/"

    private val hiloFondo = Executors.newSingleThreadExecutor()
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    /**
     * Construye la página HTML que carga checkout.js de ePayco y abre el checkout con los datos
     * de la compra. Cada valor se escapa con JSONObject.quote para evitar inyección de código.
     */
    fun paginaCheckout(
        llavePublica: String,
        pruebas: Boolean,
        factura: String,
        descripcion: String,
        monto: Long,
        email: String,
        nombre: String,
        direccion: String,
        telefono: String,
        uid: String
    ): String {
        /** Convierte un texto en literal JavaScript seguro (con comillas y caracteres escapados). */
        fun q(valor: String) = JSONObject.quote(valor)
        return """
            <!DOCTYPE html>
            <html lang="es"><head>
            <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              body{font-family:sans-serif;background:#FBF6EE;color:#3E2A1E;text-align:center;padding:48px 24px}
              .logo{font-size:22px;font-weight:bold;color:#B5532A}
              .nota{color:#7A5C48;font-size:14px}
              button{background:#B5532A;color:#fff;border:0;border-radius:24px;padding:14px 28px;font-size:16px;margin-top:24px}
            </style>
            <script src="https://checkout.epayco.co/checkout.js"></script>
            </head><body>
            <div class="logo">ArtisTech Colombia</div>
            <h3 id="t">Conectando con ePayco…</h3>
            <p class="nota" id="m">Pago seguro en modo ${if (pruebas) "pruebas (sandbox)" else "producción"}.</p>
            <button onclick="abrir()">Abrir checkout de ePayco</button>
            <script>
              function abrir(){
                try{
                  var handler = ePayco.checkout.configure({ key: ${q(llavePublica)}, test: $pruebas });
                  handler.open({
                    name: "ArtisTech Colombia",
                    description: ${q(descripcion)},
                    invoice: ${q(factura)},
                    currency: "cop",
                    amount: ${q(monto.toString())},
                    tax_base: "0",
                    tax: "0",
                    country: "co",
                    lang: "es",
                    external: "true",
                    response: ${q(URL_RESPUESTA)},
                    confirmation: ${q(URL_CONFIRMACION)},
                    email_billing: ${q(email)},
                    name_billing: ${q(nombre)},
                    address_billing: ${q(direccion)},
                    mobilephone_billing: ${q(telefono)},
                    extra1: ${q(uid)},
                    extra2: ${q(factura)}
                  });
                }catch(e){
                  document.getElementById('t').innerText = 'No fue posible abrir ePayco';
                  Android.reportarError(String(e));
                }
              }
              window.onload = function(){
                if (typeof ePayco === 'undefined') { Android.reportarError('sin_conexion'); }
                else { abrir(); }
              };
            </script>
            </body></html>
        """.trimIndent()
    }

    /**
     * Confirma la transacción consultando el servicio de validación de ePayco con la referencia.
     * Se ejecuta en un hilo de fondo; el resultado llega en el hilo principal. Si la consulta
     * falla, se usan los parámetros que ePayco envió en la URL de respuesta como respaldo.
     */
    fun validar(referencia: String, respaldo: Map<String, String>, alTerminar: (ResultadoPago) -> Unit) {
        hiloFondo.execute {
            val resultado = try {
                val conexion = URL(URL_VALIDACION + referencia).openConnection() as HttpURLConnection
                conexion.requestMethod = "GET"
                conexion.connectTimeout = 15_000
                conexion.readTimeout = 15_000
                conexion.setRequestProperty("Accept", "application/json")
                val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
                conexion.disconnect()
                val datos = JSONObject(cuerpo).optJSONObject("data") ?: throw IllegalStateException("Sin datos")
                ResultadoPago(
                    referencia = datos.optString("x_ref_payco", referencia),
                    codigo = datos.optString("x_cod_response", "3").toIntOrNull() ?: 3,
                    estado = datos.optString("x_response", "Pendiente"),
                    monto = datos.optString("x_amount", ""),
                    transaccion = datos.optString("x_transaction_id", ""),
                    franquicia = datos.optString("x_franchise", "")
                )
            } catch (e: Exception) {
                ResultadoPago(
                    referencia = referencia,
                    codigo = respaldo["x_cod_response"]?.toIntOrNull() ?: 3,
                    estado = respaldo["x_response"] ?: "Pendiente",
                    monto = respaldo["x_amount"].orEmpty(),
                    transaccion = respaldo["x_transaction_id"].orEmpty(),
                    franquicia = respaldo["x_franchise"].orEmpty()
                )
            }
            hiloPrincipal.post { alTerminar(resultado) }
        }
    }
}
