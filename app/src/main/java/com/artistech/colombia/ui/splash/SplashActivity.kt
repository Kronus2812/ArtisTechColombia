package com.artistech.colombia.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.data.Repositorio
import com.artistech.colombia.databinding.ActivitySplashBinding
import com.artistech.colombia.ui.auth.LoginActivity
import com.artistech.colombia.util.irAInicioSegunRol

/**
 * Pantalla de bienvenida: anima el logotipo y, mientras tanto, revisa si Firebase conserva una
 * sesión abierta. Si la hay, entra directo al inicio del rol; si no, abre el inicio de sesión.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var b: ActivitySplashBinding
    private val manejador = Handler(Looper.getMainLooper())

    /** Momento en que se mostró el splash, para garantizar una duración mínima de la animación. */
    private var inicio = 0L

    /** Ciclo de vida: infla el layout con ViewBinding y configura los eventos de la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)
        inicio = System.currentTimeMillis()

        // Animación de entrada del logotipo
        b.contenedorLogo.alpha = 0f
        b.contenedorLogo.scaleX = 0.7f
        b.contenedorLogo.scaleY = 0.7f
        b.contenedorLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()
        b.tvLema.alpha = 0f
        b.tvLema.animate().alpha(1f).setStartDelay(400).setDuration(600).start()

        // Restauración de sesión en paralelo con la animación
        Repositorio.restaurarSesion { usuario ->
            val espera = (DURACION_MINIMA_MS - (System.currentTimeMillis() - inicio)).coerceAtLeast(0L)
            manejador.postDelayed({
                if (isFinishing) return@postDelayed
                if (usuario != null) {
                    irAInicioSegunRol(usuario)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }, espera)
        }
    }

    /** Libera los recursos de la pantalla (manejadores pendientes o WebView) al cerrarse. */
    override fun onDestroy() {
        manejador.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val DURACION_MINIMA_MS = 1800L
    }
}
