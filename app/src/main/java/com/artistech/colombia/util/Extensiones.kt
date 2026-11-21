package com.artistech.colombia.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.load
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.ui.admin.AdminActivity
import com.artistech.colombia.ui.auth.LoginActivity
import com.artistech.colombia.ui.comprador.CompradorActivity
import com.artistech.colombia.ui.vendedor.VendedorActivity

/** Muestra un mensaje corto en pantalla (Toast). */
fun Context.mostrarToast(mensaje: String) {
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
}

/** Pinta una insignia (TextView con bg_insignia) con los colores de un estado. */
fun TextView.pintarInsignia(texto: String, @ColorRes fondo: Int, @ColorRes colorTexto: Int) {
    text = texto
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, fondo))
    setTextColor(ContextCompat.getColor(context, colorTexto))
}

/**
 * Muestra la imagen de un producto:
 * 1. Foto en Firebase Storage: se descarga y se guarda en caché con la librería Coil.
 * 2. Foto de respaldo en Base64 (cuando Storage no está disponible): se decodifica localmente.
 * 3. Sin foto: degradado e ícono vectorial de su categoría.
 */
fun mostrarImagenProducto(contenedor: View, icono: ImageView, foto: ImageView, producto: Producto) {
    contenedor.setBackgroundResource(producto.categoria.fondo)
    icono.setImageResource(producto.categoria.icono)
    when {
        producto.fotoUrl.isNotBlank() -> foto.load(producto.fotoUrl) { crossfade(true) }
        producto.fotoRespaldo != null -> foto.setImageBitmap(producto.fotoRespaldo)
        else -> foto.setImageDrawable(null)
    }
    foto.isVisible = producto.tieneFoto
    icono.isVisible = !producto.tieneFoto
}

/** Muestra la foto de perfil de un usuario (Storage o respaldo) y devuelve si tiene foto. */
fun mostrarFotoUsuario(foto: ImageView, usuario: Usuario): Boolean {
    when {
        usuario.fotoUrl.isNotBlank() -> foto.load(usuario.fotoUrl) { crossfade(true) }
        usuario.fotoRespaldo != null -> foto.setImageBitmap(usuario.fotoRespaldo)
        else -> foto.setImageDrawable(null)
    }
    val tiene = usuario.fotoUrl.isNotBlank() || usuario.fotoRespaldo != null
    foto.isVisible = tiene
    return tiene
}

/** Navegación por rol después de autenticarse (lógica de navegación de la Actividad 2). */
fun Activity.irAInicioSegunRol(usuario: Usuario) {
    val destino: Class<out Activity> = when (usuario.rol) {
        Rol.COMPRADOR -> CompradorActivity::class.java
        Rol.VENDEDOR -> VendedorActivity::class.java
        Rol.ADMIN -> AdminActivity::class.java
    }
    startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}

/** Cierra la sesión de Firebase y vuelve al inicio de sesión limpiando la pila de pantallas. */
fun Activity.cerrarSesionYSalir() {
    Repositorio.cerrarSesion()
    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}
