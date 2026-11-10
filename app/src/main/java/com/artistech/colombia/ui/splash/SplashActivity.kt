package com.artistech.colombia.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.artistech.colombia.databinding.ActivitySplashBinding
import com.artistech.colombia.ui.auth.LoginActivity

/** Pantalla de bienvenida: anima el logotipo y abre el inicio de sesión. */
class SplashActivity : AppCompatActivity() {

    private lateinit var b: ActivitySplashBinding
    private val manejador = Handler(Looper.getMainLooper())
    private val abrirLogin = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Animación de entrada del logotipo
        b.contenedorLogo.alpha = 0f
        b.contenedorLogo.scaleX = 0.7f
        b.contenedorLogo.scaleY = 0.7f
        b.contenedorLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()
        b.tvLema.alpha = 0f
        b.tvLema.animate().alpha(1f).setStartDelay(400).setDuration(600).start()

        manejador.postDelayed(abrirLogin, DURACION_MS)
    }

    override fun onDestroy() {
        manejador.removeCallbacks(abrirLogin)
        super.onDestroy()
    }

    companion object {
        private const val DURACION_MS = 2000L
    }
}
