package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.artistech.colombia.R
import com.artistech.colombia.data.Categoria
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityCompradorBinding
import com.artistech.colombia.ui.comun.PerfilFragment
import com.artistech.colombia.util.cerrarSesionYSalir

/** Contenedor del rol Comprador con Bottom Navigation (Inicio, Catálogo, Carrito, Perfil). */
class CompradorActivity : AppCompatActivity() {

    private lateinit var b: ActivityCompradorBinding

    /** Filtro que el Inicio le pasa al Catálogo al tocar una categoría o la búsqueda. */
    var categoriaPendiente: Categoria? = null
    var enfocarBusqueda = false

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repositorio.usuarioActual == null) {
            cerrarSesionYSalir()
            finish()
            return
        }
        b = ActivityCompradorBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.bottomNav.setOnItemSelectedListener { item ->
            mostrar(item.itemId)
            true
        }
        if (savedInstanceState == null) {
            irATab(intent.getIntExtra(EXTRA_TAB, R.id.nav_inicio))
        }
        // La insignia del carrito se actualiza cuando Firestore notifica cambios
        Repositorio.observar(this) { actualizarInsigniaCarrito() }
    }

    /** Permite que otras pantallas (detalle, confirmación) abran una pestaña concreta. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        if (tab != 0) irATab(tab)
    }

    /** Al volver a la pantalla se recalculan los datos visibles. */
    override fun onResume() {
        super.onResume()
        actualizarInsigniaCarrito()
    }

    /** Marca la pestaña en la barra inferior y muestra su fragmento. */
    fun irATab(idMenu: Int) {
        b.bottomNav.menu.findItem(idMenu)?.isChecked = true
        mostrar(idMenu)
    }

    /** Abre el catálogo con un filtro de categoría opcional y, si se pide, con el teclado listo. */
    fun abrirCatalogo(categoria: Categoria?, enfocar: Boolean = false) {
        categoriaPendiente = categoria
        enfocarBusqueda = enfocar
        irATab(R.id.nav_catalogo)
    }

    /** Reemplaza el fragmento visible según la opción del menú inferior. */
    private fun mostrar(idMenu: Int) {
        val fragmento: Fragment = when (idMenu) {
            R.id.nav_catalogo -> CatalogoFragment()
            R.id.nav_carrito -> CarritoFragment()
            R.id.nav_perfil -> PerfilFragment()
            else -> InicioFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.contenedorFragmentos, fragmento)
            .commit()
    }

    /** Insignia numérica (BadgeDrawable) sobre el ícono del carrito. */
    fun actualizarInsigniaCarrito() {
        val unidades = Repositorio.unidadesEnCarrito()
        val insignia = b.bottomNav.getOrCreateBadge(R.id.nav_carrito)
        insignia.backgroundColor = ContextCompat.getColor(this, R.color.mostaza)
        insignia.badgeTextColor = ContextCompat.getColor(this, R.color.cafe_texto)
        insignia.number = unidades
        insignia.isVisible = unidades > 0
    }

    companion object {
        const val EXTRA_TAB = "tab_inicial"
    }
}
