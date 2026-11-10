package com.artistech.colombia.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.artistech.colombia.R

/** Roles definidos en la Actividad 2: Comprador, Vendedor y Administrador. */
enum class Rol(val etiqueta: String) {
    COMPRADOR("Comprador"),
    VENDEDOR("Vendedor"),
    ADMIN("Administrador")
}

/** Estado de la cuenta. Cada estado define los colores de su insignia (colors.xml). */
enum class EstadoUsuario(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    ACTIVO("Activo", R.color.verde_selva_claro, R.color.verde_selva),
    PENDIENTE("Pendiente", R.color.mostaza_claro, R.color.mostaza_oscuro),
    SUSPENDIDO("Suspendido", R.color.terracota_claro, R.color.terracota_oscuro)
}

/** Categorías del catálogo. Cada una tiene su degradado (drawable) y su ícono vectorial. */
enum class Categoria(val etiqueta: String, @DrawableRes val fondo: Int, @DrawableRes val icono: Int) {
    TEJIDOS("Tejidos", R.drawable.bg_producto_tejidos, R.drawable.ic_cat_tejidos),
    CERAMICA("Cerámica", R.drawable.bg_producto_ceramica, R.drawable.ic_cat_ceramica),
    CESTERIA("Cestería", R.drawable.bg_producto_cesteria, R.drawable.ic_cat_cesteria),
    JOYERIA("Joyería", R.drawable.bg_producto_joyeria, R.drawable.ic_cat_joyeria),
    MADERA("Madera", R.drawable.bg_producto_madera, R.drawable.ic_cat_madera),
    DISENO("Diseño independiente", R.drawable.bg_producto_diseno, R.drawable.ic_cat_diseno)
}

/** Ciclo de vida de un producto en el catálogo (RF-06 y RF-23). */
enum class EstadoProducto(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    PUBLICADO("Publicado", R.color.verde_selva_claro, R.color.verde_selva),
    EN_REVISION("En revisión", R.color.mostaza_claro, R.color.mostaza_oscuro),
    AGOTADO("Agotado", R.color.terracota_claro, R.color.terracota_oscuro),
    RECHAZADO("Rechazado", R.color.arena, R.color.cafe_secundario)
}

/** Estado logístico de un pedido. */
enum class EstadoPedido(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    POR_DESPACHAR("Por despachar", R.color.mostaza_claro, R.color.mostaza_oscuro),
    EN_TRANSITO("En tránsito", R.color.azul_rio_claro, R.color.azul_rio),
    ENTREGADO("Entregado", R.color.verde_selva_claro, R.color.verde_selva),
    CANCELADO("Cancelado", R.color.terracota_claro, R.color.terracota_oscuro)
}

data class Usuario(
    val id: Int,
    var nombres: String,
    var email: String,
    var telefono: String,
    var tipoDocumento: String,
    var numeroDocumento: String,
    var rol: Rol,
    var estado: EstadoUsuario,
    var contrasena: String,
    var nombreTaller: String = "",
    var rut: String = "",
    var region: String = "",
    var oficio: String = "",
    var notificaciones: Boolean = true,
    var foto: Bitmap? = null
) {
    /** Iniciales para el avatar circular (máximo dos letras). */
    val iniciales: String
        get() = nombres.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}

data class Producto(
    val id: Int,
    var nombre: String,
    var descripcion: String,
    var precio: Long,
    var stock: Int,
    var categoria: Categoria,
    var region: String,
    var tecnica: String,
    var tiempoElaboracion: String,
    var vendedorId: Int,
    var estado: EstadoProducto,
    var calificacion: Double = 0.0,
    var resenas: Int = 0,
    var foto: Bitmap? = null,
    var fotoUri: Uri? = null
)

data class ItemCarrito(val producto: Producto, var cantidad: Int) {
    val subtotal: Long get() = producto.precio * cantidad
}

data class Pedido(
    val codigo: String,
    val compradorId: Int,
    val compradorNombre: String,
    val vendedorId: Int,
    val detalle: String,
    val total: Long,
    val fecha: String,
    val metodoPago: String,
    val categoria: Categoria,
    var estado: EstadoPedido
)
