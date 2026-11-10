package com.artistech.colombia.util

import java.text.NumberFormat
import java.util.Locale

/** Formatos regionales para Colombia (RNF-10): pesos sin decimales y punto como separador de miles. */
object Formato {
    private val localeColombia = Locale.forLanguageTag("es-CO")
    private val numeros: NumberFormat = NumberFormat.getIntegerInstance(localeColombia)

    /** 185000 -> "$185.000" */
    fun cop(valor: Long): String = "$" + numeros.format(valor)

    /** Versión corta para gráficos: 1250000 -> "1,3M", 420000 -> "420k" */
    fun copCorto(valor: Long): String = when {
        valor >= 1_000_000 -> String.format(localeColombia, "%.1fM", valor / 1_000_000.0)
        valor >= 1_000 -> "${valor / 1_000}k"
        else -> valor.toString()
    }

    fun decimal(valor: Double): String = String.format(localeColombia, "%.1f", valor)

    /** Oculta parte del correo: laura@correo.co -> l•••@correo.co */
    fun enmascararEmail(email: String): String {
        val arroba = email.indexOf('@')
        if (arroba <= 1) return email
        return "${email.first()}•••${email.substring(arroba)}"
    }
}
