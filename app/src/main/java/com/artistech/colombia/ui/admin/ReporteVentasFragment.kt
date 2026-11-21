package com.artistech.colombia.ui.admin

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.FragmentReporteVentasBinding
import com.artistech.colombia.databinding.ItemBarraGraficoBinding
import com.artistech.colombia.databinding.ItemCategoriaVentaBinding
import com.artistech.colombia.databinding.ItemVendedorTopBinding
import com.artistech.colombia.util.Formato
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reporte de ventas del Administrador calculado con los pedidos reales de Firestore.
 * El gráfico de barras se construye inflando item_barra_grafico.xml y asignando la altura
 * de cada barra en proporción al valor.
 */
class ReporteVentasFragment : Fragment(R.layout.fragment_reporte_ventas) {

    private var _b: FragmentReporteVentasBinding? = null
    private val b get() = _b!!

    /** Serie de un periodo: etiquetas del eje, valores, número de pedidos y variación vs. el periodo anterior. */
    private data class Serie(val etiquetas: List<String>, val valores: List<Long>, val pedidos: Int, val variacion: String)

    private val localeColombia = Locale.forLanguageTag("es-CO")

    /** Enlaza la vista del fragmento (ViewBinding), configura listas y eventos y se suscribe a los cambios de Firestore. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentReporteVentasBinding.bind(view)

        b.grupoPeriodo.setOnCheckedStateChangeListener { _, _ -> refrescar() }
        b.btnExportar.setOnClickListener { exportarCsv() }
        refrescar()

        // Cada pedido nuevo registrado en Firestore actualiza el reporte en vivo
        Repositorio.observar(viewLifecycleOwner) { refrescar() }
    }

    /** Vuelve a dibujar todas las secciones del reporte con los pedidos actuales. */
    private fun refrescar() {
        val vista = _b ?: return
        mostrarPeriodo(vista.grupoPeriodo.checkedChipId.takeIf { it != View.NO_ID } ?: R.id.chipSemana)
        mostrarCategorias()
        mostrarMejoresVendedores()
    }

    /**
     * Agrupa los pedidos válidos (no cancelados) de Firestore por día, semana o mes.
     * También suma el periodo anterior de igual duración para calcular la variación.
     */
    private fun calcularSerie(idChip: Int): Serie {
        val pedidos = Repositorio.pedidosValidos()
        val dia = 24L * 60 * 60 * 1000
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        // Lista de intervalos [inicio, fin) y su etiqueta
        val intervalos: List<Triple<Long, Long, String>> = when (idChip) {
            R.id.chipMes -> (3 downTo 0).map { i ->
                val fin = hoy.timeInMillis + dia - i * 7 * dia
                Triple(fin - 7 * dia, fin, getString(R.string.reporte_semana_n, 4 - i))
            }
            R.id.chipSemestre -> (5 downTo 0).map { i ->
                val inicio = (hoy.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -i) }
                val fin = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val etiqueta = SimpleDateFormat("MMM", localeColombia).format(inicio.time)
                    .replace(".", "").replaceFirstChar { it.uppercase() }
                Triple(inicio.timeInMillis, fin.timeInMillis, etiqueta)
            }
            else -> (6 downTo 0).map { i ->
                val inicio = hoy.timeInMillis - i * dia
                val etiqueta = SimpleDateFormat("EEE", localeColombia).format(Date(inicio))
                    .replace(".", "").take(2).replaceFirstChar { it.uppercase() }
                Triple(inicio, inicio + dia, etiqueta)
            }
        }
        val valores = intervalos.map { (inicio, fin, _) -> pedidos.filter { it.fechaMillis in inicio until fin }.sumOf { it.total } }
        val desde = intervalos.first().first
        val hasta = intervalos.last().second
        val duracion = hasta - desde
        val actual = valores.sum()
        val anterior = pedidos.filter { it.fechaMillis in (desde - duracion) until desde }.sumOf { it.total }
        val variacion = when {
            anterior == 0L && actual == 0L -> "—"
            anterior == 0L -> getString(R.string.reporte_nuevo)
            else -> {
                val porcentaje = (actual - anterior) * 100 / anterior
                if (porcentaje >= 0) "▲ $porcentaje %" else "▼ ${-porcentaje} %"
            }
        }
        val conteo = pedidos.count { it.fechaMillis in desde until hasta }
        return Serie(intervalos.map { it.third }, valores, conteo, variacion)
    }

    /** Muestra total, variación, número de pedidos, ticket promedio y el gráfico del periodo. */
    private fun mostrarPeriodo(idChip: Int) {
        val serie = calcularSerie(idChip)
        val total = serie.valores.sum()
        b.tvTotal.text = Formato.cop(total)
        b.tvVariacion.text = getString(R.string.reporte_variacion, serie.variacion)
        b.tvPedidos.text = serie.pedidos.toString()
        b.tvTicket.text = Formato.cop(if (serie.pedidos > 0) total / serie.pedidos else 0L)
        dibujarBarras(serie)
    }

    /** Gráfico de barras construido con vistas: cada columna es un layout inflado. */
    private fun dibujarBarras(serie: Serie) {
        val contenedor = b.contenedorBarras
        contenedor.removeAllViews()
        val alturaMaxima = resources.getDimensionPixelSize(R.dimen.alto_grafico)
        val maximo = serie.valores.maxOrNull()?.takeIf { it > 0 } ?: 1L
        serie.valores.forEachIndexed { i, valor ->
            val columna = ItemBarraGraficoBinding.inflate(layoutInflater, contenedor, false)
            columna.tvValor.text = Formato.copCorto(valor)
            columna.tvEtiqueta.text = serie.etiquetas[i]
            columna.vBarra.layoutParams.height = (alturaMaxima * valor / maximo).toInt().coerceAtLeast(12)
            val color = if (valor == maximo && valor > 0) R.color.terracota else R.color.mostaza
            columna.vBarra.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
            contenedor.addView(columna.root)
        }
    }

    /** Barras de progreso con las ventas por categoría. */
    private fun mostrarCategorias() {
        val datos = Repositorio.ventasPorCategoria()
        val maximo = datos.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1L
        val colores = listOf(R.color.terracota, R.color.verde_selva, R.color.mostaza, R.color.azul_rio, R.color.cafe_secundario)
        b.contenedorCategorias.removeAllViews()
        datos.forEachIndexed { i, (categoria, valor) ->
            val fila = ItemCategoriaVentaBinding.inflate(layoutInflater, b.contenedorCategorias, false)
            fila.tvCategoria.text = categoria.etiqueta
            fila.tvValor.text = Formato.cop(valor)
            fila.progreso.setIndicatorColor(ContextCompat.getColor(requireContext(), colores[i % colores.size]))
            fila.progreso.progress = (valor * 100 / maximo).toInt()
            b.contenedorCategorias.addView(fila.root)
        }
    }

    /** Top 3 de vendedores por ventas. */
    private fun mostrarMejoresVendedores() {
        b.contenedorVendedores.removeAllViews()
        Repositorio.mejoresVendedores().forEachIndexed { i, (nombre, region, valor) ->
            val fila = ItemVendedorTopBinding.inflate(layoutInflater, b.contenedorVendedores, false)
            fila.tvPosicion.text = (i + 1).toString()
            fila.tvNombre.text = nombre
            fila.tvRegion.text = region
            fila.tvValor.text = Formato.cop(valor)
            b.contenedorVendedores.addView(fila.root)
        }
    }

    /** Exporta el reporte como texto CSV y abre el menú de compartir de Android. */
    private fun exportarCsv() {
        val csv = buildString {
            appendLine("pedido,comprador,vendedor,detalle,fecha,metodo,estado_pago,referencia_epayco,estado,direccion,total")
            Repositorio.pedidos.forEach { p ->
                appendLine("${p.codigo},${p.compradorNombre},${p.vendedorNombre},\"${p.detalle}\",${p.fecha},${p.metodoPago},${p.estadoPago},${p.referenciaPago},${p.estado.etiqueta},\"${p.direccionEntrega}\",${p.total}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reporte_asunto))
            .putExtra(Intent.EXTRA_TEXT, csv)
        startActivity(Intent.createChooser(intent, getString(R.string.reporte_exportar)))
    }

    /** Libera la referencia al binding para evitar fugas de memoria del fragmento. */
    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
