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
import com.artistech.colombia.data.Producto
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.data.Rol
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.ui.admin.AdminActivity
import com.artistech.colombia.ui.auth.LoginActivity
import com.artistech.colombia.ui.comprador.CompradorActivity
import com.artistech.colombia.ui.vendedor.VendedorActivity

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
 * Muestra la imagen de un producto: foto real (cámara o galería) si existe;
 * si no, el degradado y el ícono vectorial de su categoría.
 */
fun mostrarImagenProducto(contenedor: View, icono: ImageView, foto: ImageView, producto: Producto) {
    contenedor.setBackgroundResource(producto.categoria.fondo)
    icono.setImageResource(producto.categoria.icono)
    when {
        producto.foto != null -> foto.setImageBitmap(producto.foto)
        producto.fotoUri != null -> foto.setImageURI(producto.fotoUri)
    }
    val tieneFoto = producto.foto != null || producto.fotoUri != null
    foto.isVisible = tieneFoto
    icono.isVisible = !tieneFoto
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

/** Cierra la sesión y vuelve al inicio de sesión limpiando la pila de pantallas. */
fun Activity.cerrarSesionYSalir() {
    Repositorio.cerrarSesion()
    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}
