package com.artistech.colombia.ui.vendedor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.artistech.colombia.R
import com.artistech.colombia.data.EstadoPedido
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivityVendedorBinding
import com.artistech.colombia.ui.comun.PerfilFragment
import com.artistech.colombia.util.cerrarSesionYSalir

/** Contenedor del rol Vendedor con Bottom Navigation (Productos, Pedidos, Perfil). */
class VendedorActivity : AppCompatActivity() {

    private lateinit var b: ActivityVendedorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repositorio.usuarioActual == null) {
            cerrarSesionYSalir()
            finish()
            return
        }
        b = ActivityVendedorBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.bottomNav.setOnItemSelectedListener { item ->
            mostrar(item.itemId)
            true
        }
        if (savedInstanceState == null) {
            b.bottomNav.menu.findItem(R.id.nav_productos_vendedor)?.isChecked = true
            mostrar(R.id.nav_productos_vendedor)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarInsigniaPedidos()
    }

    private fun mostrar(idMenu: Int) {
        val fragmento: Fragment = when (idMenu) {
            R.id.nav_pedidos_vendedor -> PedidosVendedorFragment()
            R.id.nav_perfil_vendedor -> PerfilFragment()
            else -> ProductosVendedorFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.contenedorFragmentos, fragmento)
            .commit()
    }

    /** Insignia con el número de pedidos por despachar. */
    fun actualizarInsigniaPedidos() {
        val idVendedor = Repositorio.usuarioActual?.id ?: return
        val pendientes = Repositorio.pedidosDelVendedor(idVendedor).count { it.estado == EstadoPedido.POR_DESPACHAR }
        val insignia = b.bottomNav.getOrCreateBadge(R.id.nav_pedidos_vendedor)
        insignia.backgroundColor = ContextCompat.getColor(this, R.color.mostaza)
        insignia.badgeTextColor = ContextCompat.getColor(this, R.color.cafe_texto)
        insignia.number = pendientes
        insignia.isVisible = pendientes > 0
    }
}
