package com.artistech.colombia.ui.admin

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.artistech.colombia.R
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityAdminBinding
import com.artistech.colombia.databinding.NavHeaderAdminBinding
import com.artistech.colombia.ui.comun.PerfilFragment
import com.artistech.colombia.util.cerrarSesionYSalir
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Contenedor del Administrador con menú lateral (Navigation Drawer). */
class AdminActivity : AppCompatActivity() {

    private lateinit var b: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val admin = Repositorio.usuarioActual
        if (admin == null) {
            cerrarSesionYSalir()
            finish()
            return
        }
        b = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        // Botón de hamburguesa sincronizado con el Drawer
        val toggle = ActionBarDrawerToggle(this, b.drawerLayout, b.toolbar, R.string.drawer_abrir, R.string.drawer_cerrar)
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.cafe_texto)
        b.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Encabezado del Drawer con los datos del administrador
        val encabezado = NavHeaderAdminBinding.bind(b.navView.getHeaderView(0))
        encabezado.tvIniciales.text = admin.iniciales
        encabezado.tvNombre.text = admin.nombres
        encabezado.tvEmail.text = admin.email

        b.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_salir) confirmarSalida() else navegarA(item.itemId)
            b.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        onBackPressedDispatcher.addCallback(this) {
            if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                b.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        if (savedInstanceState == null) navegarA(R.id.nav_dashboard)
    }

    /** Cambia el fragmento visible y el título de la barra. */
    fun navegarA(idMenu: Int) {
        val (fragmento: Fragment, titulo: Int) = when (idMenu) {
            R.id.nav_usuarios -> UsuariosFragment() to R.string.admin_usuarios
            R.id.nav_productos_admin -> ProductosAdminFragment() to R.string.admin_productos
            R.id.nav_reporte -> ReporteVentasFragment() to R.string.admin_reporte
            R.id.nav_perfil_admin -> PerfilFragment() to R.string.admin_perfil
            else -> DashboardAdminFragment() to R.string.admin_dashboard
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.contenedorAdmin, fragmento)
            .commit()
        supportActionBar?.setTitle(titulo)
        b.navView.setCheckedItem(idMenu)
    }

    private fun confirmarSalida() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cerrar_sesion)
            .setMessage(R.string.cerrar_sesion_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salir) { _, _ -> cerrarSesionYSalir() }
            .show()
    }
}
