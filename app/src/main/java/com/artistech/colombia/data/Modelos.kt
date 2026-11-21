package com.artistech.colombia.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.artistech.colombia.R
import com.google.firebase.firestore.DocumentSnapshot

/*
 * Modelos de dominio de ArtisTech Colombia.
 *
 * Cada modelo sabe convertirse a un Map<String, Any?> para guardarse en Cloud Firestore
 * (aMapa) y reconstruirse a partir de un documento (desdeDocumento). El mapeo se hace a mano,
 * campo por campo, para controlar los nombres en la base de datos y guardar los enum como texto.
 */

/** Roles definidos en la Actividad 2: Comprador, Vendedor y Administrador. */
enum class Rol(val etiqueta: String) {
    COMPRADOR("Comprador"),
    VENDEDOR("Vendedor"),
    ADMIN("Administrador");

    companion object {
        /** Convierte el texto guardado en Firestore en un Rol; si no lo reconoce, devuelve Comprador. */
        fun desde(valor: String?): Rol = entries.firstOrNull { it.name == valor } ?: COMPRADOR
    }
}

/** Estado de la cuenta. Cada estado define los colores de su insignia (colors.xml). */
enum class EstadoUsuario(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    ACTIVO("Activo", R.color.verde_selva_claro, R.color.verde_selva),
    PENDIENTE("Pendiente", R.color.mostaza_claro, R.color.mostaza_oscuro),
    SUSPENDIDO("Suspendido", R.color.terracota_claro, R.color.terracota_oscuro);

    companion object {
        /** Convierte el texto guardado en Firestore en un estado; por defecto Activo. */
        fun desde(valor: String?): EstadoUsuario = entries.firstOrNull { it.name == valor } ?: ACTIVO
    }
}

/** Categorías del catálogo. Cada una tiene su degradado (drawable) y su ícono vectorial. */
enum class Categoria(val etiqueta: String, @DrawableRes val fondo: Int, @DrawableRes val icono: Int) {
    TEJIDOS("Tejidos", R.drawable.bg_producto_tejidos, R.drawable.ic_cat_tejidos),
    CERAMICA("Cerámica", R.drawable.bg_producto_ceramica, R.drawable.ic_cat_ceramica),
    CESTERIA("Cestería", R.drawable.bg_producto_cesteria, R.drawable.ic_cat_cesteria),
    JOYERIA("Joyería", R.drawable.bg_producto_joyeria, R.drawable.ic_cat_joyeria),
    MADERA("Madera", R.drawable.bg_producto_madera, R.drawable.ic_cat_madera),
    DISENO("Diseño independiente", R.drawable.bg_producto_diseno, R.drawable.ic_cat_diseno);

    companion object {
        /** Convierte el texto guardado en Firestore en una categoría; por defecto Tejidos. */
        fun desde(valor: String?): Categoria = entries.firstOrNull { it.name == valor } ?: TEJIDOS
    }
}

/** Ciclo de vida de un producto en el catálogo (RF-06 y RF-23). */
enum class EstadoProducto(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    PUBLICADO("Publicado", R.color.verde_selva_claro, R.color.verde_selva),
    EN_REVISION("En revisión", R.color.mostaza_claro, R.color.mostaza_oscuro),
    AGOTADO("Agotado", R.color.terracota_claro, R.color.terracota_oscuro),
    RECHAZADO("Rechazado", R.color.arena, R.color.cafe_secundario);

    companion object {
        /** Convierte el texto guardado en Firestore en un estado; por defecto En revisión. */
        fun desde(valor: String?): EstadoProducto = entries.firstOrNull { it.name == valor } ?: EN_REVISION
    }
}

/** Estado logístico de un pedido. */
enum class EstadoPedido(val etiqueta: String, @ColorRes val fondo: Int, @ColorRes val texto: Int) {
    POR_DESPACHAR("Por despachar", R.color.mostaza_claro, R.color.mostaza_oscuro),
    EN_TRANSITO("En tránsito", R.color.azul_rio_claro, R.color.azul_rio),
    ENTREGADO("Entregado", R.color.verde_selva_claro, R.color.verde_selva),
    CANCELADO("Cancelado", R.color.terracota_claro, R.color.terracota_oscuro);

    companion object {
        /** Convierte el texto guardado en Firestore en un estado; por defecto Por despachar. */
        fun desde(valor: String?): EstadoPedido = entries.firstOrNull { it.name == valor } ?: POR_DESPACHAR
    }
}

/**
 * Perfil de un usuario. El [id] es el UID que Firebase Authentication asigna a la cuenta,
 * y es también el identificador del documento en la colección "usuarios" de Firestore.
 * La contraseña NO se guarda aquí: la administra Firebase Authentication.
 */
data class Usuario(
    val id: String,
    var nombres: String,
    var email: String,
    var telefono: String,
    var tipoDocumento: String,
    var numeroDocumento: String,
    var rol: Rol,
    var estado: EstadoUsuario,
    var nombreTaller: String = "",
    var rut: String = "",
    var region: String = "",
    var oficio: String = "",
    var notificaciones: Boolean = true,
    var direccion: String = "",
    var latitud: Double? = null,
    var longitud: Double? = null,
    var fotoUrl: String = "",
    var fotoBase64: String = "",
    var creadoEn: Long = System.currentTimeMillis()
) {
    /** Iniciales para el avatar circular (máximo dos letras). */
    val iniciales: String
        get() = nombres.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    /** Foto de perfil decodificada cuando se guardó como respaldo Base64 (sin Firebase Storage). */
    val fotoRespaldo: Bitmap? by lazy { decodificarBase64(fotoBase64) }

    /** Convierte el perfil en el mapa de campos que se escribe en Firestore. */
    fun aMapa(): Map<String, Any?> = mapOf(
        "nombres" to nombres,
        "email" to email.lowercase(),
        "telefono" to telefono,
        "tipoDocumento" to tipoDocumento,
        "numeroDocumento" to numeroDocumento,
        "rol" to rol.name,
        "estado" to estado.name,
        "nombreTaller" to nombreTaller,
        "rut" to rut,
        "region" to region,
        "oficio" to oficio,
        "notificaciones" to notificaciones,
        "direccion" to direccion,
        "latitud" to latitud,
        "longitud" to longitud,
        "fotoUrl" to fotoUrl,
        "fotoBase64" to fotoBase64,
        "creadoEn" to creadoEn
    )

    companion object {
        /** Reconstruye un usuario a partir de un documento de la colección "usuarios". */
        fun desdeDocumento(doc: DocumentSnapshot): Usuario = Usuario(
            id = doc.id,
            nombres = doc.getString("nombres").orEmpty(),
            email = doc.getString("email").orEmpty(),
            telefono = doc.getString("telefono").orEmpty(),
            tipoDocumento = doc.getString("tipoDocumento").orEmpty(),
            numeroDocumento = doc.getString("numeroDocumento").orEmpty(),
            rol = Rol.desde(doc.getString("rol")),
            estado = EstadoUsuario.desde(doc.getString("estado")),
            nombreTaller = doc.getString("nombreTaller").orEmpty(),
            rut = doc.getString("rut").orEmpty(),
            region = doc.getString("region").orEmpty(),
            oficio = doc.getString("oficio").orEmpty(),
            notificaciones = doc.getBoolean("notificaciones") ?: true,
            direccion = doc.getString("direccion").orEmpty(),
            latitud = doc.getDouble("latitud"),
            longitud = doc.getDouble("longitud"),
            fotoUrl = doc.getString("fotoUrl").orEmpty(),
            fotoBase64 = doc.getString("fotoBase64").orEmpty(),
            creadoEn = doc.getLong("creadoEn") ?: 0L
        )
    }
}

/**
 * Producto del catálogo, guardado en la colección "productos".
 * [vendedorNombre] se guarda duplicado (desnormalizado) para que el Comprador vea el nombre
 * del taller sin tener permiso de leer el perfil completo del Vendedor.
 */
data class Producto(
    val id: String,
    var nombre: String,
    var descripcion: String,
    var precio: Long,
    var stock: Int,
    var categoria: Categoria,
    var region: String,
    var tecnica: String,
    var tiempoElaboracion: String,
    var vendedorId: String,
    var vendedorNombre: String,
    var estado: EstadoProducto,
    var calificacion: Double = 0.0,
    var resenas: Int = 0,
    var fotoUrl: String = "",
    var fotoBase64: String = "",
    var creadoEn: Long = System.currentTimeMillis()
) {
    /** Foto decodificada cuando se guardó como respaldo Base64 dentro del documento. */
    val fotoRespaldo: Bitmap? by lazy { decodificarBase64(fotoBase64) }

    /** Indica si el producto tiene alguna foto real (Storage o respaldo). */
    val tieneFoto: Boolean get() = fotoUrl.isNotBlank() || fotoBase64.isNotBlank()

    /** Convierte el producto en el mapa de campos que se escribe en Firestore. */
    fun aMapa(): Map<String, Any?> = mapOf(
        "nombre" to nombre,
        "descripcion" to descripcion,
        "precio" to precio,
        "stock" to stock,
        "categoria" to categoria.name,
        "region" to region,
        "tecnica" to tecnica,
        "tiempoElaboracion" to tiempoElaboracion,
        "vendedorId" to vendedorId,
        "vendedorNombre" to vendedorNombre,
        "estado" to estado.name,
        "calificacion" to calificacion,
        "resenas" to resenas,
        "fotoUrl" to fotoUrl,
        "fotoBase64" to fotoBase64,
        "creadoEn" to creadoEn
    )

    companion object {
        /** Reconstruye un producto a partir de un documento de la colección "productos". */
        fun desdeDocumento(doc: DocumentSnapshot): Producto = Producto(
            id = doc.id,
            nombre = doc.getString("nombre").orEmpty(),
            descripcion = doc.getString("descripcion").orEmpty(),
            precio = doc.getLong("precio") ?: 0L,
            stock = (doc.getLong("stock") ?: 0L).toInt(),
            categoria = Categoria.desde(doc.getString("categoria")),
            region = doc.getString("region").orEmpty(),
            tecnica = doc.getString("tecnica").orEmpty(),
            tiempoElaboracion = doc.getString("tiempoElaboracion").orEmpty(),
            vendedorId = doc.getString("vendedorId").orEmpty(),
            vendedorNombre = doc.getString("vendedorNombre").orEmpty(),
            estado = EstadoProducto.desde(doc.getString("estado")),
            calificacion = doc.getDouble("calificacion") ?: 0.0,
            resenas = (doc.getLong("resenas") ?: 0L).toInt(),
            fotoUrl = doc.getString("fotoUrl").orEmpty(),
            fotoBase64 = doc.getString("fotoBase64").orEmpty(),
            creadoEn = doc.getLong("creadoEn") ?: 0L
        )
    }
}

/**
 * Ítem del carrito. En Firestore solo se guarda {productoId, cantidad} en la subcolección
 * usuarios/{uid}/carrito; el producto completo se toma del catálogo en memoria.
 */
data class ItemCarrito(val producto: Producto, var cantidad: Int) {
    /** Precio del producto multiplicado por la cantidad elegida. */
    val subtotal: Long get() = producto.precio * cantidad
}

/** Línea de detalle de un pedido (copia del producto al momento de la compra). */
data class LineaPedido(val productoId: String, val nombre: String, val cantidad: Int, val precio: Long) {
    /** Mapa que se guarda dentro del arreglo "items" del pedido. */
    fun aMapa(): Map<String, Any> = mapOf(
        "productoId" to productoId, "nombre" to nombre, "cantidad" to cantidad, "precio" to precio
    )
}

/**
 * Pedido (orden de compra) guardado en la colección "pedidos". Se crea un pedido por cada
 * vendedor del carrito. Incluye la dirección y coordenadas de entrega (geolocalización) y la
 * referencia de la transacción de ePayco para su trazabilidad.
 */
data class Pedido(
    val id: String,
    val codigo: String,
    val compradorId: String,
    val compradorNombre: String,
    val vendedorId: String,
    val vendedorNombre: String,
    val items: List<LineaPedido>,
    val total: Long,
    val fechaMillis: Long,
    val metodoPago: String,
    val categoria: Categoria,
    var estado: EstadoPedido,
    val estadoPago: String = "",
    val referenciaPago: String = "",
    val direccionEntrega: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
) {
    /** Resumen legible de los productos: "Mochila wayúu × 1, Hamaca × 2". */
    val detalle: String get() = items.joinToString(", ") { "${it.nombre} × ${it.cantidad}" }

    /** Fecha del pedido en formato dd/MM/yyyy. */
    val fecha: String get() = com.artistech.colombia.util.Formato.fecha(fechaMillis)

    /** Convierte el pedido en el mapa de campos que se escribe en Firestore. */
    fun aMapa(): Map<String, Any?> = mapOf(
        "codigo" to codigo,
        "compradorId" to compradorId,
        "compradorNombre" to compradorNombre,
        "vendedorId" to vendedorId,
        "vendedorNombre" to vendedorNombre,
        "items" to items.map { it.aMapa() },
        "total" to total,
        "fechaMillis" to fechaMillis,
        "metodoPago" to metodoPago,
        "categoria" to categoria.name,
        "estado" to estado.name,
        "estadoPago" to estadoPago,
        "referenciaPago" to referenciaPago,
        "direccionEntrega" to direccionEntrega,
        "latitud" to latitud,
        "longitud" to longitud
    )

    companion object {
        /** Reconstruye un pedido a partir de un documento de la colección "pedidos". */
        fun desdeDocumento(doc: DocumentSnapshot): Pedido {
            // "items" llega como una lista de mapas genéricos; se convierte a LineaPedido
            val crudos = doc.get("items") as? List<*> ?: emptyList<Any>()
            val lineas = crudos.mapNotNull { elemento ->
                val m = elemento as? Map<*, *> ?: return@mapNotNull null
                LineaPedido(
                    productoId = m["productoId"] as? String ?: "",
                    nombre = m["nombre"] as? String ?: "",
                    cantidad = (m["cantidad"] as? Number)?.toInt() ?: 0,
                    precio = (m["precio"] as? Number)?.toLong() ?: 0L
                )
            }
            return Pedido(
                id = doc.id,
                codigo = doc.getString("codigo").orEmpty(),
                compradorId = doc.getString("compradorId").orEmpty(),
                compradorNombre = doc.getString("compradorNombre").orEmpty(),
                vendedorId = doc.getString("vendedorId").orEmpty(),
                vendedorNombre = doc.getString("vendedorNombre").orEmpty(),
                items = lineas,
                total = doc.getLong("total") ?: 0L,
                fechaMillis = doc.getLong("fechaMillis") ?: 0L,
                metodoPago = doc.getString("metodoPago").orEmpty(),
                categoria = Categoria.desde(doc.getString("categoria")),
                estado = EstadoPedido.desde(doc.getString("estado")),
                estadoPago = doc.getString("estadoPago").orEmpty(),
                referenciaPago = doc.getString("referenciaPago").orEmpty(),
                direccionEntrega = doc.getString("direccionEntrega").orEmpty(),
                latitud = doc.getDouble("latitud"),
                longitud = doc.getDouble("longitud")
            )
        }
    }
}

/**
 * Decodifica una imagen guardada como texto Base64 (respaldo cuando Firebase Storage
 * no está disponible). Devuelve null si el texto está vacío o no es una imagen válida.
 */
fun decodificarBase64(texto: String): Bitmap? {
    if (texto.isBlank()) return null
    return try {
        val bytes = Base64.decode(texto, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: IllegalArgumentException) {
        null
    }
}
