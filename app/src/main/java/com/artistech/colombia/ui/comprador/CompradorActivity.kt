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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        if (tab != 0) irATab(tab)
    }

    override fun onResume() {
        super.onResume()
        actualizarInsigniaCarrito()
    }

    fun irATab(idMenu: Int) {
        b.bottomNav.menu.findItem(idMenu)?.isChecked = true
        mostrar(idMenu)
    }

    fun abrirCatalogo(categoria: Categoria?, enfocar: Boolean = false) {
        categoriaPendiente = categoria
        enfocarBusqueda = enfocar
        irATab(R.id.nav_catalogo)
    }

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
