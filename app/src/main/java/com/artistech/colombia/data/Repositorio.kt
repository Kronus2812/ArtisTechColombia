package com.artistech.colombia.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.artistech.colombia.R
import com.artistech.colombia.util.ImagenesFirebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

/**
 * Excepción propia para errores de negocio que la interfaz traduce con un recurso de texto
 * (por ejemplo, "tu cuenta está suspendida").
 */
class ErrorNegocio(val mensajeRes: Int, val detalle: String = "") : Exception(detalle)

/**
 * Repositorio único de datos de la aplicación (patrón Repository + Singleton).
 *
 * Arquitectura:
 * - La fuente de verdad es Firebase: Authentication para las cuentas y Cloud Firestore para
 *   usuarios, productos, carritos y pedidos. Las fotos se suben a Cloud Storage.
 * - Al iniciar sesión se registran "escuchas" en tiempo real (addSnapshotListener). Cada vez que
 *   Firestore cambia, las listas en memoria (usuarios, productos, pedidos, carrito) se actualizan
 *   y se avisa a las pantallas suscritas con [observar]. Así la interfaz lee datos de forma
 *   síncrona y siempre ve la información más reciente, incluso entre dispositivos.
 * - Las operaciones de escritura (CRUD) son asíncronas y reportan su resultado con un callback
 *   que recibe un [Result] de Kotlin (éxito o excepción).
 */
object Repositorio {

    private const val TAG = "Repositorio"

    /** Contraseña sugerida para las cuentas de demostración (botones de acceso rápido del login). */
    const val CLAVE_DEMO = "Artis2026"
    const val CUPON_VALIDO = "ARTESANO10"
    const val COSTO_ENVIO = 14_900L

    // Nombres de las colecciones en Firestore
    const val COL_USUARIOS = "usuarios"
    const val COL_PRODUCTOS = "productos"
    const val COL_PEDIDOS = "pedidos"
    const val SUBCOL_CARRITO = "carrito"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Perfil del usuario autenticado (null si no hay sesión). */
    var usuarioActual: Usuario? = null
        private set

    /** Indica si el cupón ARTESANO10 está aplicado al carrito actual. */
    var cuponAplicado = false
        private set

    /** Indica si la primera carga del catálogo ya llegó desde Firestore. */
    var catalogoCargado = false
        private set

    // ---------------------------------------------------------------- Caché en memoria
    val usuarios = mutableListOf<Usuario>()
    val productos = mutableListOf<Producto>()
    val pedidos = mutableListOf<Pedido>()
    val carrito = mutableListOf<ItemCarrito>()

    /** Cantidades del carrito tal como están en Firestore: productoId -> cantidad. */
    private val cantidadesCarrito = linkedMapOf<String, Int>()

    /** Escuchas de Firestore activas; se cancelan al cerrar sesión. */
    private val escuchas = mutableListOf<ListenerRegistration>()

    /** Pantallas suscritas a cambios de datos. */
    private val observadores = mutableSetOf<() -> Unit>()

    // ================================================================ Observadores
    /**
     * Suscribe una acción que se ejecuta cada vez que cambian los datos.
     * La suscripción se elimina sola cuando el ciclo de vida del [dueno] se destruye,
     * evitando fugas de memoria.
     */
    fun observar(dueno: LifecycleOwner, accion: () -> Unit) {
        observadores.add(accion)
        dueno.lifecycle.addObserver(LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_DESTROY) observadores.remove(accion)
        })
    }

    /** Avisa a todas las pantallas suscritas que hay datos nuevos. */
    private fun notificar() {
        observadores.toList().forEach { it() }
    }

    // ================================================================ Autenticación
    /**
     * Inicia sesión con correo y contraseña en Firebase Authentication y luego carga el perfil
     * desde Firestore. Rechaza la sesión si el perfil fue eliminado o la cuenta está suspendida.
     */
    fun iniciarSesion(email: String, clave: String, alTerminar: (Result<Usuario>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), clave)
            .addOnSuccessListener { cargarPerfilYAbrirSesion(alTerminar) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /**
     * Restaura la sesión guardada por Firebase (la app recuerda al usuario entre aperturas).
     * Devuelve null si no hay sesión o si el perfil ya no es válido.
     */
    fun restaurarSesion(alTerminar: (Usuario?) -> Unit) {
        if (auth.currentUser == null) {
            alTerminar(null)
            return
        }
        cargarPerfilYAbrirSesion { resultado -> alTerminar(resultado.getOrNull()) }
    }

    /** Lee usuarios/{uid}, valida el estado de la cuenta y activa las escuchas en tiempo real. */
    private fun cargarPerfilYAbrirSesion(alTerminar: (Result<Usuario>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            alTerminar(Result.failure(ErrorNegocio(R.string.error_credenciales)))
            return
        }
        db.collection(COL_USUARIOS).document(uid).get()
            .addOnSuccessListener { doc ->
                when {
                    // El administrador eliminó el perfil: la cuenta ya no puede ingresar
                    !doc.exists() -> {
                        auth.signOut()
                        alTerminar(Result.failure(ErrorNegocio(R.string.error_perfil_inexistente)))
                    }
                    EstadoUsuario.desde(doc.getString("estado")) == EstadoUsuario.SUSPENDIDO -> {
                        auth.signOut()
                        alTerminar(Result.failure(ErrorNegocio(R.string.error_suspendido)))
                    }
                    else -> {
                        val usuario = Usuario.desdeDocumento(doc)
                        usuarioActual = usuario
                        iniciarEscuchas(usuario)
                        alTerminar(Result.success(usuario))
                    }
                }
            }
            .addOnFailureListener {
                auth.signOut()
                alTerminar(Result.failure(it))
            }
    }

    /**
     * Registra una cuenta nueva (Comprador o Vendedor): crea el usuario en Firebase Authentication
     * y guarda su perfil en Firestore. Al final cierra la sesión para que la persona ingrese
     * manualmente, como en el flujo diseñado en la Actividad 2.
     */
    fun registrar(datos: Usuario, clave: String, alTerminar: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(datos.email.trim(), clave)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid ?: return@addOnSuccessListener
                val perfil = datos.copy(id = uid)
                db.collection(COL_USUARIOS).document(uid).set(perfil.aMapa())
                    .addOnSuccessListener {
                        auth.signOut()
                        alTerminar(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        // Si no se pudo guardar el perfil, se borra la cuenta para no dejarla huérfana
                        resultado.user?.delete()
                        alTerminar(Result.failure(error))
                    }
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Envía el correo oficial de Firebase con el enlace para restablecer la contraseña (RF-04). */
    fun recuperarContrasena(email: String, alTerminar: (Result<Unit>) -> Unit) {
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Confirma la contraseña del usuario actual (se usa antes de activar el ingreso con huella). */
    fun verificarClave(clave: String, alTerminar: (Result<Unit>) -> Unit) {
        val usuario = auth.currentUser
        val email = usuario?.email
        if (usuario == null || email == null) {
            alTerminar(Result.failure(ErrorNegocio(R.string.error_credenciales)))
            return
        }
        usuario.reauthenticate(EmailAuthProvider.getCredential(email, clave))
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Cierra la sesión en Firebase, cancela las escuchas y limpia toda la caché en memoria. */
    fun cerrarSesion() {
        escuchas.forEach { it.remove() }
        escuchas.clear()
        auth.signOut()
        usuarioActual = null
        cuponAplicado = false
        catalogoCargado = false
        usuarios.clear()
        productos.clear()
        pedidos.clear()
        carrito.clear()
        cantidadesCarrito.clear()
    }

    // ================================================================ Escuchas en tiempo real
    /**
     * Registra las escuchas de Firestore según el rol. Las consultas coinciden con las reglas
     * de seguridad (firestore.rules): cada rol solo pide los documentos que tiene permiso de leer.
     */
    private fun iniciarEscuchas(usuario: Usuario) {
        escuchas.forEach { it.remove() }
        escuchas.clear()

        // 1. Catálogo de productos (lo leen todos los roles autenticados)
        escuchas += db.collection(COL_PRODUCTOS).addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "Escucha de productos", error); return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            productos.clear()
            productos.addAll(snap.documents.map { Producto.desdeDocumento(it) }.sortedByDescending { it.creadoEn })
            catalogoCargado = true
            reconstruirCarrito()
            notificar()
        }

        // 2. Pedidos: el Administrador ve todos; el Vendedor los recibidos; el Comprador los suyos
        val consultaPedidos = when (usuario.rol) {
            Rol.ADMIN -> db.collection(COL_PEDIDOS)
            Rol.VENDEDOR -> db.collection(COL_PEDIDOS).whereEqualTo("vendedorId", usuario.id)
            Rol.COMPRADOR -> db.collection(COL_PEDIDOS).whereEqualTo("compradorId", usuario.id)
        }
        escuchas += consultaPedidos.addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "Escucha de pedidos", error); return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            pedidos.clear()
            // Se ordena en memoria para no requerir índices compuestos en Firestore
            pedidos.addAll(snap.documents.map { Pedido.desdeDocumento(it) }.sortedByDescending { it.fechaMillis })
            notificar()
        }

        // 3. Usuarios: el Administrador escucha toda la colección; los demás solo su propio perfil
        if (usuario.rol == Rol.ADMIN) {
            escuchas += db.collection(COL_USUARIOS).addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "Escucha de usuarios", error); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                usuarios.clear()
                usuarios.addAll(snap.documents.map { Usuario.desdeDocumento(it) }.sortedByDescending { it.creadoEn })
                usuarios.find { it.id == usuario.id }?.let { usuarioActual = it }
                notificar()
            }
        } else {
            escuchas += db.collection(COL_USUARIOS).document(usuario.id).addSnapshotListener { doc, error ->
                if (error != null) { Log.w(TAG, "Escucha de perfil", error); return@addSnapshotListener }
                if (doc == null || !doc.exists()) return@addSnapshotListener
                val perfil = Usuario.desdeDocumento(doc)
                usuarioActual = perfil
                usuarios.clear()
                usuarios.add(perfil)
                notificar()
            }
        }

        // 4. Carrito persistente del Comprador: usuarios/{uid}/carrito
        if (usuario.rol == Rol.COMPRADOR) {
            escuchas += refCarrito(usuario.id).addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "Escucha de carrito", error); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                cantidadesCarrito.clear()
                snap.documents.forEach { doc ->
                    cantidadesCarrito[doc.id] = (doc.getLong("cantidad") ?: 0L).toInt()
                }
                reconstruirCarrito()
                notificar()
            }
        }
    }

    /** Une las cantidades guardadas en Firestore con los productos del catálogo en memoria. */
    private fun reconstruirCarrito() {
        carrito.clear()
        cantidadesCarrito.forEach { (productoId, cantidad) ->
            val producto = productos.find { it.id == productoId }
            if (producto != null && cantidad > 0) carrito.add(ItemCarrito(producto, cantidad))
        }
    }

    /** Referencia a la subcolección usuarios/{uid}/carrito. */
    private fun refCarrito(uid: String) = db.collection(COL_USUARIOS).document(uid).collection(SUBCOL_CARRITO)

    // ================================================================ CRUD Usuarios
    fun usuarioPorId(id: String): Usuario? = usuarios.find { it.id == id }

    /**
     * El Administrador crea una cuenta nueva (C). Se usa una segunda instancia de FirebaseApp
     * para que crear la cuenta no reemplace la sesión del Administrador; después se guarda el
     * perfil en Firestore con la sesión del Administrador.
     */
    fun crearUsuarioComoAdmin(contexto: Context, datos: Usuario, clave: String, alTerminar: (Result<Unit>) -> Unit) {
        val appSecundaria = FirebaseApp.getApps(contexto).firstOrNull { it.name == APP_SECUNDARIA }
            ?: FirebaseApp.initializeApp(contexto, FirebaseApp.getInstance().options, APP_SECUNDARIA)
        val authSecundaria = FirebaseAuth.getInstance(appSecundaria)
        authSecundaria.createUserWithEmailAndPassword(datos.email.trim(), clave)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid ?: return@addOnSuccessListener
                authSecundaria.signOut()
                db.collection(COL_USUARIOS).document(uid).set(datos.copy(id = uid).aMapa())
                    .addOnSuccessListener { alTerminar(Result.success(Unit)) }
                    .addOnFailureListener { alTerminar(Result.failure(it)) }
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Actualiza (U) el perfil de un usuario. Lo usa el Administrador y cada usuario en su Perfil. */
    fun actualizarUsuario(usuario: Usuario, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COL_USUARIOS).document(usuario.id).set(usuario.aMapa(), SetOptions.merge())
            .addOnSuccessListener {
                if (usuario.id == usuarioActual?.id) usuarioActual = usuario
                alTerminar(Result.success(Unit))
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /**
     * Elimina (D) el perfil de un usuario en Firestore. Sin perfil, la cuenta queda bloqueada:
     * el inicio de sesión la rechaza (ver [cargarPerfilYAbrirSesion]). Borrar la credencial de
     * Authentication de otra persona solo es posible desde un servidor con el Admin SDK.
     */
    fun eliminarUsuario(id: String, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COL_USUARIOS).document(id).delete()
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** El usuario elimina su propia cuenta: borra su perfil, su credencial y cierra la sesión. */
    fun eliminarCuentaPropia(alTerminar: (Result<Unit>) -> Unit) {
        val usuario = auth.currentUser ?: return alTerminar(Result.failure(ErrorNegocio(R.string.error_credenciales)))
        db.collection(COL_USUARIOS).document(usuario.uid).delete()
            .addOnSuccessListener {
                // delete() puede pedir un inicio de sesión reciente; el perfil ya quedó eliminado
                usuario.delete().addOnCompleteListener {
                    cerrarSesion()
                    alTerminar(Result.success(Unit))
                }
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Sube la foto de perfil (Storage o respaldo Base64) y la guarda en el documento del usuario. */
    fun actualizarFotoPerfil(foto: Bitmap, alTerminar: (Result<Unit>) -> Unit) {
        val usuario = usuarioActual ?: return
        ImagenesFirebase.subir("usuarios/${usuario.id}/perfil.jpg", foto) { url, base64 ->
            val actualizado = usuario.copy(fotoUrl = url, fotoBase64 = base64)
            actualizarUsuario(actualizado, alTerminar)
        }
    }

    /** Guarda en el perfil la dirección y coordenadas obtenidas por GPS (geolocalización). */
    fun guardarUbicacion(direccion: String, latitud: Double, longitud: Double) {
        val usuario = usuarioActual ?: return
        usuario.direccion = direccion
        usuario.latitud = latitud
        usuario.longitud = longitud
        db.collection(COL_USUARIOS).document(usuario.id)
            .update(mapOf("direccion" to direccion, "latitud" to latitud, "longitud" to longitud))
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar la ubicación", it) }
    }

    // ================================================================ CRUD Productos
    fun productoPorId(id: String): Producto? = productos.find { it.id == id }

    /** Productos visibles para el Comprador (estado Publicado). */
    fun productosPublicados(): List<Producto> = productos.filter { it.estado == EstadoProducto.PUBLICADO }

    /** Los cuatro productos publicados con mejor calificación. */
    fun destacados(): List<Producto> = productosPublicados().sortedByDescending { it.calificacion }.take(4)

    /** Búsqueda por nombre, región o técnica, con filtro opcional de categoría (RF-08 y RF-09). */
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

    /** Productos que pertenecen a un vendedor (su panel). */
    fun productosDelVendedor(vendedorId: String): List<Producto> = productos.filter { it.vendedorId == vendedorId }

    /**
     * Crea (C) o actualiza (U) un producto. Si llega una foto nueva, primero se sube a
     * Firebase Storage y luego se guarda el documento con la URL de descarga.
     */
    fun guardarProducto(producto: Producto, fotoNueva: Bitmap?, alTerminar: (Result<Producto>) -> Unit) {
        val coleccion = db.collection(COL_PRODUCTOS)
        // Un id vacío significa "producto nuevo": Firestore genera el identificador
        val referencia = if (producto.id.isEmpty()) coleccion.document() else coleccion.document(producto.id)
        val base = producto.copy(id = referencia.id)

        val guardarDocumento: (Producto) -> Unit = { final ->
            referencia.set(final.aMapa())
                .addOnSuccessListener { alTerminar(Result.success(final)) }
                .addOnFailureListener { alTerminar(Result.failure(it)) }
        }
        if (fotoNueva == null) {
            guardarDocumento(base)
        } else {
            ImagenesFirebase.subir("productos/${referencia.id}.jpg", fotoNueva) { url, base64 ->
                guardarDocumento(base.copy(fotoUrl = url, fotoBase64 = base64))
            }
        }
    }

    /** Elimina (D) un producto y su foto en Storage (si existe). */
    fun eliminarProducto(producto: Producto, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COL_PRODUCTOS).document(producto.id).delete()
            .addOnSuccessListener {
                if (producto.fotoUrl.isNotBlank()) {
                    try {
                        FirebaseStorage.getInstance().getReferenceFromUrl(producto.fotoUrl).delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo borrar la foto del producto", e)
                    }
                }
                alTerminar(Result.success(Unit))
            }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Cambia el estado de moderación de un producto (aprobar o rechazar, RF-23). */
    fun cambiarEstadoProducto(producto: Producto, estado: EstadoProducto, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COL_PRODUCTOS).document(producto.id).update("estado", estado.name)
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Nombre comercial del vendedor: primero el perfil (si se conoce) y si no el dato del producto. */
    fun nombreVendedor(vendedorId: String): String {
        usuarioPorId(vendedorId)?.let { return it.nombreTaller.ifBlank { it.nombres } }
        return productos.firstOrNull { it.vendedorId == vendedorId }?.vendedorNombre?.ifBlank { null }
            ?: "Taller artesanal"
    }

    /**
     * Carga en Firestore el catálogo de ejemplo (12 productos artesanales) asignado al vendedor
     * elegido por el Administrador. Sirve para poblar la base de datos en una instalación nueva.
     */
    fun cargarCatalogoDemo(vendedor: Usuario, alTerminar: (Result<Int>) -> Unit) {
        val lote = db.batch()
        val nombreTaller = vendedor.nombreTaller.ifBlank { vendedor.nombres }
        val ejemplos = CatalogoDemo.productos(vendedor.id, nombreTaller)
        ejemplos.forEach { p -> lote.set(db.collection(COL_PRODUCTOS).document(), p.aMapa()) }
        lote.commit()
            .addOnSuccessListener { alTerminar(Result.success(ejemplos.size)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    // ================================================================ CRUD Carrito
    /** Agrega (C) unidades de un producto al carrito persistente, sin superar el stock. */
    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val actual = cantidadesCarrito[producto.id] ?: 0
        escribirCantidad(producto.id, (actual + cantidad).coerceAtMost(producto.stock))
    }

    /** Suma o resta unidades (U). Devuelve false si se alcanzó el límite de stock. */
    fun cambiarCantidad(item: ItemCarrito, delta: Int): Boolean {
        val nueva = item.cantidad + delta
        if (nueva > item.producto.stock) return false
        escribirCantidad(item.producto.id, nueva)
        return true
    }

    /** Quita (D) un producto del carrito. */
    fun quitarDelCarrito(item: ItemCarrito) {
        escribirCantidad(item.producto.id, 0)
    }

    /**
     * Escribe la cantidad en usuarios/{uid}/carrito/{productoId}. La caché local se actualiza
     * de inmediato (respuesta instantánea en la interfaz) y Firestore sincroniza en segundo plano.
     */
    private fun escribirCantidad(productoId: String, cantidad: Int) {
        val uid = usuarioActual?.id ?: return
        val documento = refCarrito(uid).document(productoId)
        if (cantidad <= 0) {
            cantidadesCarrito.remove(productoId)
            documento.delete()
        } else {
            cantidadesCarrito[productoId] = cantidad
            documento.set(mapOf("productoId" to productoId, "cantidad" to cantidad, "actualizadoEn" to System.currentTimeMillis()))
        }
        reconstruirCarrito()
        notificar()
    }

    /** Vacía el carrito en Firestore con una escritura por lotes. */
    private fun vaciarCarrito() {
        val uid = usuarioActual?.id ?: return
        val lote = db.batch()
        cantidadesCarrito.keys.forEach { lote.delete(refCarrito(uid).document(it)) }
        lote.commit()
        cantidadesCarrito.clear()
        carrito.clear()
        cuponAplicado = false
        notificar()
    }

    /** Total de unidades en el carrito (insignia de la barra inferior). */
    fun unidadesEnCarrito(): Int = carrito.sumOf { it.cantidad }

    /** Suma de precio × cantidad de todos los ítems. */
    fun subtotal(): Long = carrito.sumOf { it.subtotal }

    /** 10 % del subtotal si el cupón está aplicado. */
    fun descuento(): Long = if (cuponAplicado) subtotal() / 10 else 0L

    /** Costo de envío fijo por compra (0 si el carrito está vacío). */
    fun envio(): Long = if (carrito.isEmpty()) 0L else COSTO_ENVIO

    /** Total a pagar: subtotal − descuento + envío. */
    fun total(): Long = subtotal() - descuento() + envio()

    /** IVA incluido en el total (19 %), informado según el Estatuto del Consumidor. */
    fun ivaIncluido(): Long = total() * 19 / 119

    /** Valida el cupón y lo deja aplicado si es correcto. */
    fun aplicarCupon(codigo: String): Boolean {
        cuponAplicado = codigo.trim().equals(CUPON_VALIDO, ignoreCase = true)
        return cuponAplicado
    }

    // ================================================================ Pedidos (órdenes)
    /**
     * Convierte el carrito en pedidos (uno por vendedor) dentro de una TRANSACCIÓN de Firestore:
     * 1) lee el stock actual de cada producto, 2) verifica que alcance, 3) descuenta el inventario
     * y 4) crea los documentos de pedido. Si algo falla, no se aplica ningún cambio.
     */
    fun confirmarCompra(pago: DatosPago, alTerminar: (Result<List<Pedido>>) -> Unit) {
        val comprador = usuarioActual ?: return alTerminar(Result.failure(ErrorNegocio(R.string.error_credenciales)))
        val items = carrito.toList()
        if (items.isEmpty()) return alTerminar(Result.failure(ErrorNegocio(R.string.carrito_vacio_titulo)))
        val conCupon = cuponAplicado
        val ahora = System.currentTimeMillis()

        db.runTransaction { tx ->
            // Lecturas: en una transacción todas las lecturas van antes de las escrituras
            val referencias = items.map { db.collection(COL_PRODUCTOS).document(it.producto.id) }
            val stocks = referencias.map { (tx.get(it).getLong("stock") ?: 0L).toInt() }

            items.forEachIndexed { i, item ->
                if (stocks[i] < item.cantidad) {
                    throw FirebaseFirestoreException(
                        "Sin unidades suficientes de ${item.producto.nombre}",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
            }
            // Escrituras: descuento de inventario
            items.forEachIndexed { i, item ->
                val restante = stocks[i] - item.cantidad
                val cambios = mutableMapOf<String, Any>("stock" to restante)
                if (restante == 0) cambios["estado"] = EstadoProducto.AGOTADO.name
                tx.update(referencias[i], cambios)
            }
            // Un pedido por vendedor; el envío se cobra una sola vez por compra
            var envioPendiente = COSTO_ENVIO
            val creados = items.groupBy { it.producto.vendedorId }.entries.mapIndexed { indice, (vendedorId, lineas) ->
                val bruto = lineas.sumOf { it.subtotal }
                val total = (if (conCupon) bruto - bruto / 10 else bruto) + envioPendiente
                envioPendiente = 0L
                val referencia = db.collection(COL_PEDIDOS).document()
                val pedido = Pedido(
                    id = referencia.id,
                    codigo = "AT-${(ahora / 1000) % 1_000_000}${indice + 1}",
                    compradorId = comprador.id,
                    compradorNombre = comprador.nombres,
                    vendedorId = vendedorId,
                    vendedorNombre = lineas.first().producto.vendedorNombre,
                    items = lineas.map { LineaPedido(it.producto.id, it.producto.nombre, it.cantidad, it.producto.precio) },
                    total = total,
                    fechaMillis = ahora,
                    metodoPago = pago.metodo,
                    categoria = lineas.first().producto.categoria,
                    estado = EstadoPedido.POR_DESPACHAR,
                    estadoPago = pago.estado,
                    referenciaPago = pago.referencia,
                    direccionEntrega = pago.direccion,
                    latitud = pago.latitud,
                    longitud = pago.longitud
                )
                tx.set(referencia, pedido.aMapa())
                pedido
            }
            creados
        }.addOnSuccessListener { creados ->
            vaciarCarrito()
            alTerminar(Result.success(creados))
        }.addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** El Vendedor o el Administrador actualiza el estado logístico de un pedido. */
    fun actualizarEstadoPedido(pedido: Pedido, estado: EstadoPedido, alTerminar: (Result<Unit>) -> Unit) {
        db.collection(COL_PEDIDOS).document(pedido.id).update("estado", estado.name)
            .addOnSuccessListener { alTerminar(Result.success(Unit)) }
            .addOnFailureListener { alTerminar(Result.failure(it)) }
    }

    /** Pedidos recibidos por un vendedor. */
    fun pedidosDelVendedor(vendedorId: String): List<Pedido> = pedidos.filter { it.vendedorId == vendedorId }

    /** Historial de compras de un comprador. */
    fun pedidosDelComprador(compradorId: String): List<Pedido> = pedidos.filter { it.compradorId == compradorId }

    // ================================================================ Reportes
    /** Pedidos que cuentan como venta (se excluyen los cancelados). */
    fun pedidosValidos(): List<Pedido> = pedidos.filter { it.estado != EstadoPedido.CANCELADO }

    /** Suma de todos los pedidos válidos (indicador del Dashboard). */
    fun ventasTotales(): Long = pedidosValidos().sumOf { it.total }

    /** Ventas de un vendedor (indicador de su panel). */
    fun ventasDelVendedor(vendedorId: String): Long =
        pedidosValidos().filter { it.vendedorId == vendedorId }.sumOf { it.total }

    /** Ventas agrupadas por categoría, de mayor a menor (reporte). */
    fun ventasPorCategoria(): List<Pair<Categoria, Long>> =
        pedidosValidos().groupBy { it.categoria }
            .map { (categoria, lista) -> categoria to lista.sumOf { it.total } }
            .sortedByDescending { it.second }

    /** Los tres vendedores con más ventas: (nombre, región, total). */
    fun mejoresVendedores(): List<Triple<String, String, Long>> =
        pedidosValidos().groupBy { it.vendedorId }
            .map { (id, lista) ->
                Triple(
                    usuarioPorId(id)?.let { it.nombreTaller.ifBlank { it.nombres } } ?: lista.first().vendedorNombre,
                    usuarioPorId(id)?.region.orEmpty(),
                    lista.sumOf { it.total }
                )
            }
            .sortedByDescending { it.third }
            .take(3)

    private const val APP_SECUNDARIA = "artistech_admin"
}

/**
 * Datos del pago que se guardan con el pedido: método, estado devuelto por la pasarela,
 * referencia de la transacción y la dirección/coordenadas de entrega capturadas por GPS.
 */
data class DatosPago(
    val metodo: String,
    val estado: String,
    val referencia: String,
    val direccion: String,
    val latitud: Double?,
    val longitud: Double?
)
