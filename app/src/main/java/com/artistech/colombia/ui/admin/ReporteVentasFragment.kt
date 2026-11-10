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

/**
 * Reporte de ventas del Administrador. El gráfico de barras se construye inflando
 * item_barra_grafico.xml y asignando la altura de cada barra en proporción al valor.
 */
class ReporteVentasFragment : Fragment(R.layout.fragment_reporte_ventas) {

    private var _b: FragmentReporteVentasBinding? = null
    private val b get() = _b!!

    /** Serie de demostración por periodo: etiquetas, valores y variación frente al periodo anterior. */
    private data class Serie(val etiquetas: List<String>, val valores: List<Long>, val pedidos: Int, val variacion: String)

    private val series = mapOf(
        R.id.chipSemana to Serie(
            listOf("L", "M", "M", "J", "V", "S", "D"),
            listOf(420_000L, 610_000L, 480_000L, 750_000L, 580_000L, 920_000L, 660_000L), 27, "▲ 12 %"
        ),
        R.id.chipMes to Serie(
            listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4"),
            listOf(3_200_000L, 4_100_000L, 3_600_000L, 4_900_000L), 112, "▲ 18 %"
        ),
        R.id.chipSemestre to Serie(
            listOf("Jun", "Jul", "Ago", "Sep", "Oct", "Nov"),
            listOf(9_800_000L, 11_200_000L, 12_900_000L, 12_100_000L, 15_800_000L, 17_400_000L), 586, "▲ 34 %"
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentReporteVentasBinding.bind(view)

        b.grupoPeriodo.setOnCheckedStateChangeListener { _, ids ->
            ids.firstOrNull()?.let { mostrarPeriodo(it) }
        }
        mostrarPeriodo(R.id.chipSemana)
        mostrarCategorias()
        mostrarMejoresVendedores()
        b.btnExportar.setOnClickListener { exportarCsv() }
    }

    private fun mostrarPeriodo(idChip: Int) {
        val serie = series[idChip] ?: return
        val total = serie.valores.sum()
        b.tvTotal.text = Formato.cop(total)
        b.tvVariacion.text = getString(R.string.reporte_variacion, serie.variacion)
        b.tvPedidos.text = serie.pedidos.toString()
        b.tvTicket.text = Formato.cop(total / serie.pedidos)
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
            val color = if (valor == maximo) R.color.terracota else R.color.mostaza
            columna.vBarra.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
            contenedor.addView(columna.root)
        }
    }

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
            appendLine("pedido,comprador,vendedor,detalle,fecha,metodo,estado,total")
            Repositorio.pedidos.forEach { p ->
                appendLine("${p.codigo},${p.compradorNombre},${Repositorio.nombreVendedor(p.vendedorId)},\"${p.detalle}\",${p.fecha},${p.metodoPago},${p.estado.etiqueta},${p.total}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reporte_asunto))
            .putExtra(Intent.EXTRA_TEXT, csv)
        startActivity(Intent.createChooser(intent, getString(R.string.reporte_exportar)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
