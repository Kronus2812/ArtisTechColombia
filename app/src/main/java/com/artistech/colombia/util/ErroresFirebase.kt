package com.artistech.colombia.util

import android.content.Context
import com.artistech.colombia.R
import com.artistech.colombia.data.ErrorNegocio
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Traduce las excepciones de Firebase a mensajes claros en español para el usuario final.
 * Centralizar esta lógica evita repetir el mismo "when" en cada pantalla.
 */
fun Context.mensajeError(error: Throwable?): String = when (error) {
    is ErrorNegocio -> getString(error.mensajeRes)
    // WeakPassword hereda de InvalidCredentials, por eso se evalúa primero
    is FirebaseAuthWeakPasswordException -> getString(R.string.error_clave_corta)
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_credenciales)
    is FirebaseAuthUserCollisionException -> getString(R.string.error_email_existe)
    is FirebaseNetworkException -> getString(R.string.error_red)
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> getString(R.string.error_permiso)
        FirebaseFirestoreException.Code.UNAVAILABLE -> getString(R.string.error_red)
        FirebaseFirestoreException.Code.ABORTED -> error.message ?: getString(R.string.error_generico)
        else -> error.localizedMessage ?: getString(R.string.error_generico)
    }
    else -> error?.localizedMessage ?: getString(R.string.error_generico)
}
