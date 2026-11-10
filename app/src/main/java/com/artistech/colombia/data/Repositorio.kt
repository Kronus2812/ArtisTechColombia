package com.artistech.colombia.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fuente de datos en memoria para la fase de frontend.
 * Simula el backend (Firebase / API REST de la Actividad 2) con datos de demostración
 * y expone las operaciones CRUD de usuarios, productos, carrito y pedidos.
 */
object Repositorio {

    const val CLAVE_DEMO = "Artis2026"
    const val CUPON_VALIDO = "ARTESANO10"
    const val COSTO_ENVIO = 14_900L
    const val CODIGO_RECUPERACION_DEMO = "123456"

    private var siguienteUsuarioId = 100
    private var siguienteProductoId = 100
    private var consecutivoPedido = 10_483

    var usuarioActual: Usuario? = null
        private set

    var cuponAplicado = false
        private set

    // ---------------------------------------------------------------- Datos iniciales
    val usuarios = mutableListOf(
        Usuario(1, "Laura Gómez", "comprador@artistech.co", "3001234567", "CC", "1020456789",
            Rol.COMPRADOR, EstadoUsuario.ACTIVO, CLAVE_DEMO),
        Usuario(2, "Yanama Pushaina", "vendedor@artistech.co", "3109876543", "CC", "40912345",
            Rol.VENDEDOR, EstadoUsuario.ACTIVO, CLAVE_DEMO, nombreTaller = "Taller Yanama",
            rut = "40912345-1", region = "Uribia, La Guajira", oficio = "Tejeduría wayúu"),
        Usuario(3, "Andrea Rincón", "admin@artistech.co", "3205551234", "CC", "52789456",
            Rol.ADMIN, EstadoUsuario.ACTIVO, CLAVE_DEMO),
        Usuario(4, "Carlos Ruiz", "carlos.ruiz@correo.co", "3014567890", "CC", "1067894512",
            Rol.VENDEDOR, EstadoUsuario.PENDIENTE, CLAVE_DEMO, nombreTaller = "Sombreros Tuchín",
            rut = "1067894512-3", region = "Tuchín, Córdoba", oficio = "Trenzado en caña flecha"),
        Usuario(5, "Ana Ospina", "ceramica.chamba@correo.co", "3157778899", "CC", "28456123",
            Rol.VENDEDOR, EstadoUsuario.ACTIVO, CLAVE_DEMO, nombreTaller = "Alfarería La Chamba",
            rut = "28456123-7", region = "El Guamo, Tolima", oficio = "Cerámica negra"),
        Usuario(6, "Sofía Ramírez", "sofia.ramirez@correo.co", "3112223344", "CC", "1032456987",
            Rol.COMPRADOR, EstadoUsuario.ACTIVO, CLAVE_DEMO),
        Usuario(7, "Andrés Pérez", "andres.perez@correo.co", "3186665544", "CE", "784512",
            Rol.COMPRADOR, EstadoUsuario.ACTIVO, CLAVE_DEMO),
        Usuario(8, "Mateo Torres", "mtorres@correo.co", "3009998877", "CC", "1019874563",
            Rol.COMPRADOR, EstadoUsuario.SUSPENDIDO, CLAVE_DEMO)
    )

    val productos = mutableListOf(
        Producto(1, "Mochila wayúu tradicional",
            "Tejida en crochet con hilo acrílico por artesanas wayúu. Diseño de kanaas (figuras geométricas) que representan elementos de la cultura de La Guajira. 25 × 22 cm, con gaza ajustable.",
            185_000, 12, Categoria.TEJIDOS, "Uribia, La Guajira", "Crochet en hilo acrílico", "20 días",
            2, EstadoProducto.PUBLICADO, 4.9, 126),
        Producto(2, "Chinchorro wayúu",
            "Chinchorro tejido a mano con flecos decorativos. Resistente y cómodo, ideal para terrazas y zonas de descanso.",
            420_000, 3, Categoria.TEJIDOS, "Manaure, La Guajira", "Tejido de malla en telar", "30 días",
            2, EstadoProducto.PUBLICADO, 4.8, 41),
        Producto(3, "Sombrero vueltiao 23 vueltas",
            "Sombrero elaborado con fibra de caña flecha trenzada. Símbolo cultural de Colombia, de 23 vueltas y ala ancha.",
            240_000, 7, Categoria.TEJIDOS, "Tuchín, Córdoba", "Trenzado en caña flecha", "15 días",
            4, EstadoProducto.EN_REVISION),
        Producto(4, "Olla de barro La Chamba",
            "Olla de cerámica negra bruñida a mano, apta para cocinar y servir. Producto con denominación de origen.",
            95_000, 9, Categoria.CERAMICA, "El Guamo, Tolima", "Cerámica negra bruñida", "8 días",
            5, EstadoProducto.PUBLICADO, 4.7, 58),
        Producto(5, "Juego de platos en cerámica negra",
            "Set de 4 platos de cerámica negra con acabado brillante, elaborados en torno y quemados a leña.",
            160_000, 5, Categoria.CERAMICA, "El Guamo, Tolima", "Torno y quema a leña", "10 días",
            5, EstadoProducto.PUBLICADO, 4.6, 22),
        Producto(6, "Canasto en palma de iraca",
            "Canasto tejido en palma de iraca con tapa. Perfecto para organizar y decorar espacios.",
            78_000, 15, Categoria.CESTERIA, "Sandoná, Nariño", "Tejido en palma de iraca", "6 días",
            5, EstadoProducto.PUBLICADO, 4.5, 37),
        Producto(7, "Aretes en filigrana momposina",
            "Aretes de plata 950 elaborados con la técnica de filigrana, tradición orfebre de Mompox.",
            130_000, 0, Categoria.JOYERIA, "Mompox, Bolívar", "Filigrana en plata", "10 días",
            2, EstadoProducto.AGOTADO, 5.0, 19),
        Producto(8, "Mola guna tradicional",
            "Mola elaborada con la técnica de capas superpuestas de tela cosidas a mano, inspirada en la fauna del Darién.",
            150_000, 4, Categoria.TEJIDOS, "Necoclí, Antioquia", "Capas superpuestas cosidas a mano", "12 días",
            2, EstadoProducto.PUBLICADO, 4.8, 33),
        Producto(9, "Tabla de cocina en guayacán",
            "Tabla de cortar en madera de guayacán con acabado en aceite mineral grado alimenticio.",
            68_000, 20, Categoria.MADERA, "Pereira, Risaralda", "Tallado y lijado a mano", "4 días",
            5, EstadoProducto.PUBLICADO, 4.4, 15),
        Producto(10, "Hamaca San Jacinto",
            "Hamaca de algodón tejida en telar vertical con los colores tradicionales de los Montes de María.",
            320_000, 6, Categoria.TEJIDOS, "San Jacinto, Bolívar", "Telar vertical en algodón", "18 días",
            2, EstadoProducto.PUBLICADO, 4.9, 64),
        Producto(11, "Lámina ilustrada Fauna de Colombia",
            "Ilustración original impresa en papel de algodón de 300 g. Edición limitada y firmada.",
            55_000, 25, Categoria.DISENO, "Bogotá D.C.", "Ilustración digital e impresión giclée", "3 días",
            5, EstadoProducto.EN_REVISION),
        Producto(12, "Collar en tagua",
            "Collar de semillas de tagua talladas y teñidas con pigmentos naturales del Pacífico colombiano.",
            89_000, 10, Categoria.JOYERIA, "Quibdó, Chocó", "Tallado en tagua", "5 días",
            4, EstadoProducto.PUBLICADO, 4.6, 12)
    )

    val pedidos = mutableListOf(
        Pedido("AT-10482", 1, "Laura Gómez", 2, "Mochila wayúu tradicional × 1", 199_900, "08/11/2026", "Tarjeta", Categoria.TEJIDOS, EstadoPedido.POR_DESPACHAR),
        Pedido("AT-10477", 7, "Andrés Pérez", 2, "Mola guna tradicional × 2", 314_900, "05/11/2026", "PSE", Categoria.TEJIDOS, EstadoPedido.EN_TRANSITO),
        Pedido("AT-10471", 6, "Sofía Ramírez", 2, "Hamaca San Jacinto × 1", 334_900, "01/11/2026", "Nequi", Categoria.TEJIDOS, EstadoPedido.ENTREGADO),
        Pedido("AT-10465", 1, "Laura Gómez", 5, "Olla de barro La Chamba × 1", 109_900, "29/10/2026", "Tarjeta", Categoria.CERAMICA, EstadoPedido.ENTREGADO),
        Pedido("AT-10459", 7, "Andrés Pérez", 2, "Chinchorro wayúu × 1", 434_900, "25/10/2026", "Efectivo", Categoria.TEJIDOS, EstadoPedido.ENTREGADO),
        Pedido("AT-10452", 6, "Sofía Ramírez", 5, "Canasto en palma de iraca × 2", 170_900, "20/10/2026", "PSE", Categoria.CESTERIA, EstadoPedido.CANCELADO),
        Pedido("AT-10448", 1, "Laura Gómez", 5, "Tabla de cocina en guayacán × 1", 82_900, "18/10/2026", "Tarjeta", Categoria.MADERA, EstadoPedido.EN_TRANSITO),
        Pedido("AT-10441", 6, "Sofía Ramírez", 4, "Collar en tagua × 1", 103_900, "15/10/2026", "Nequi", Categoria.JOYERIA, EstadoPedido.ENTREGADO)
    )

    val carrito = mutableListOf<ItemCarrito>()

    // ---------------------------------------------------------------- Sesión
    fun validarCredenciales(email: String, clave: String): Usuario? =
        usuarios.find { it.email.equals(email.trim(), ignoreCase = true) && it.contrasena == clave }

    fun usuarioPorEmail(email: String): Usuario? =
        usuarios.find { it.email.equals(email.trim(), ignoreCase = true) }

    fun abrirSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

    fun cerrarSesion() {
        usuarioActual = null
        carrito.clear()
        cuponAplicado = false
    }

    fun cambiarContrasena(email: String, nueva: String): Boolean {
        val usuario = usuarioPorEmail(email) ?: return false
        usuario.contrasena = nueva
        return true
    }

    // ---------------------------------------------------------------- CRUD Usuarios
    fun usuarioPorId(id: Int): Usuario? = usuarios.find { it.id == id }

    fun emailDisponible(email: String, idPropio: Int = -1): Boolean =
        usuarios.none { it.email.equals(email.trim(), ignoreCase = true) && it.id != idPropio }

    /** Crea (id = 0) o actualiza un usuario y devuelve la versión guardada. */
    fun guardarUsuario(usuario: Usuario): Usuario {
        if (usuario.id == 0) {
            val nuevo = usuario.copy(id = siguienteUsuarioId++)
            usuarios.add(0, nuevo)
            return nuevo
        }
        val indice = usuarios.indexOfFirst { it.id == usuario.id }
        if (indice >= 0) usuarios[indice] = usuario
        if (usuarioActual?.id == usuario.id) usuarioActual = usuario
        return usuario
    }

    fun eliminarUsuario(id: Int) {
        usuarios.removeAll { it.id == id }
    }

    // ---------------------------------------------------------------- CRUD Productos
    fun productoPorId(id: Int): Producto? = productos.find { it.id == id }

    fun productosPublicados(): List<Producto> = productos.filter { it.estado == EstadoProducto.PUBLICADO }

    fun destacados(): List<Producto> = productosPublicados().sortedByDescending { it.calificacion }.take(4)

    fun buscarProductos(texto: String, categoria: Categoria?): List<Producto> {
        val consulta = texto.trim()
        return productosPublicados().filter { p ->
            (categoria == null || p.categoria == categoria) &&
                (consulta.isEmpty() ||
                    p.nombre.contains(consulta, ignoreCase = true) ||
                    p.region.contains(consulta, ignoreCase = true) ||
                    p.tecnica.contains(consulta, ignoreCase = true))
        }
    }

    fun productosDelVendedor(vendedorId: Int): List<Producto> = productos.filter { it.vendedorId == vendedorId }

    fun guardarProducto(producto: Producto): Producto {
        if (producto.id == 0) {
            val nuevo = producto.copy(id = siguienteProductoId++)
            productos.add(0, nuevo)
            return nuevo
        }
        val indice = productos.indexOfFirst { it.id == producto.id }
        if (indice >= 0) productos[indice] = producto
        // Mantiene el carrito sincronizado con la versión editada del producto
        carrito.replaceAll { item -> if (item.producto.id == producto.id) item.copy(producto = producto) else item }
        return producto
    }

    fun eliminarProducto(id: Int) {
        productos.removeAll { it.id == id }
        carrito.removeAll { it.producto.id == id }
    }

    fun nombreVendedor(vendedorId: Int): String {
        val vendedor = usuarioPorId(vendedorId) ?: return "Taller artesanal"
        return vendedor.nombreTaller.ifBlank { vendedor.nombres }
    }

    // ---------------------------------------------------------------- CRUD Carrito
    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val existente = carrito.find { it.producto.id == producto.id }
        if (existente != null) {
            existente.cantidad = (existente.cantidad + cantidad).coerceAtMost(producto.stock)
        } else {
            carrito.add(ItemCarrito(producto, cantidad.coerceAtMost(producto.stock)))
        }
    }

    /** Suma o resta unidades. Devuelve false si se alcanzó el límite de stock. */
    fun cambiarCantidad(item: ItemCarrito, delta: Int): Boolean {
        val nueva = item.cantidad + delta
        if (nueva > item.producto.stock) return false
        if (nueva <= 0) carrito.remove(item) else item.cantidad = nueva
        return true
    }

    fun quitarDelCarrito(item: ItemCarrito) {
        carrito.remove(item)
    }

    fun unidadesEnCarrito(): Int = carrito.sumOf { it.cantidad }

    fun subtotal(): Long = carrito.sumOf { it.subtotal }

    fun descuento(): Long = if (cuponAplicado) subtotal() / 10 else 0L

    fun envio(): Long = if (carrito.isEmpty()) 0L else COSTO_ENVIO

    fun total(): Long = subtotal() - descuento() + envio()

    /** IVA incluido en el total (19 %), informado según el Estatuto del Consumidor. */
    fun ivaIncluido(): Long = total() * 19 / 119

    fun aplicarCupon(codigo: String): Boolean {
        cuponAplicado = codigo.trim().equals(CUPON_VALIDO, ignoreCase = true)
        return cuponAplicado
    }

    // ---------------------------------------------------------------- Pedidos
    /** Convierte el carrito en pedidos (uno por vendedor), descuenta inventario y vacía el carrito. */
    fun confirmarCompra(metodoPago: String): List<Pedido> {
        val comprador = usuarioActual ?: return emptyList()
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CO")).format(Date())
        var envioPendiente = COSTO_ENVIO // el envío se cobra una sola vez por compra
        val creados = carrito.groupBy { it.producto.vendedorId }.map { (vendedorId, items) ->
            val detalle = items.joinToString(", ") { "${it.producto.nombre} × ${it.cantidad}" }
            val bruto = items.sumOf { it.subtotal }
            val total = (if (cuponAplicado) bruto - bruto / 10 else bruto) + envioPendiente
            envioPendiente = 0L
            Pedido("AT-${consecutivoPedido++}", comprador.id, comprador.nombres, vendedorId, detalle,
                total, fecha, metodoPago, items.first().producto.categoria, EstadoPedido.POR_DESPACHAR)
        }
        carrito.forEach { item ->
            item.producto.stock -= item.cantidad
            if (item.producto.stock <= 0) {
                item.producto.stock = 0
                item.producto.estado = EstadoProducto.AGOTADO
            }
        }
        pedidos.addAll(0, creados)
        carrito.clear()
        cuponAplicado = false
        return creados
    }

    fun pedidosDelVendedor(vendedorId: Int): List<Pedido> = pedidos.filter { it.vendedorId == vendedorId }

    fun pedidosDelComprador(compradorId: Int): List<Pedido> = pedidos.filter { it.compradorId == compradorId }

    // ---------------------------------------------------------------- Reportes
    private fun pedidosValidos() = pedidos.filter { it.estado != EstadoPedido.CANCELADO }

    fun ventasTotales(): Long = pedidosValidos().sumOf { it.total }

    fun ventasDelVendedor(vendedorId: Int): Long =
        pedidosValidos().filter { it.vendedorId == vendedorId }.sumOf { it.total }

    fun ventasPorCategoria(): List<Pair<Categoria, Long>> =
        pedidosValidos().groupBy { it.categoria }
            .map { (categoria, lista) -> categoria to lista.sumOf { it.total } }
            .sortedByDescending { it.second }

    fun mejoresVendedores(): List<Triple<String, String, Long>> =
        pedidosValidos().groupBy { it.vendedorId }
            .map { (id, lista) -> Triple(nombreVendedor(id), usuarioPorId(id)?.region.orEmpty(), lista.sumOf { it.total }) }
            .sortedByDescending { it.third }
            .take(3)
}
