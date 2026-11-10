package com.artistech.colombia.ui.comprador

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.R
import com.artistech.colombia.databinding.ActivityPedidoConfirmadoBinding
import com.artistech.colombia.util.Formato

/** Paso 3 de la pasarela: confirmación del pedido. */
class PedidoConfirmadoActivity : AppCompatActivity() {

    private lateinit var b: ActivityPedidoConfirmadoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPedidoConfirmadoBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvCodigo.text = getString(R.string.confirmado_codigo, intent.getStringExtra(EXTRA_CODIGO).orEmpty())
        b.tvTotal.text = Formato.cop(intent.getLongExtra(EXTRA_TOTAL, 0L))
        b.tvMetodo.text = intent.getStringExtra(EXTRA_METODO).orEmpty()

        b.btnSeguirComprando.setOnClickListener { volverA(R.id.nav_inicio) }
        b.btnVerPedidos.setOnClickListener { volverA(R.id.nav_perfil) }
        onBackPressedDispatcher.addCallback(this) { volverA(R.id.nav_inicio) }
    }

    private fun volverA(tab: Int) {
        startActivity(
            Intent(this, CompradorActivity::class.java)
                .putExtra(CompradorActivity.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_METODO = "metodo"
    }
}
