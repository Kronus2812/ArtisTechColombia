package com.artistech.colombia.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream

/**
 * Manejo de imágenes: lectura desde cámara/galería, compresión y subida a Firebase Storage.
 *
 * Estrategia de respaldo: si Firebase Storage no está disponible (por ejemplo, un proyecto en el
 * plan gratuito Spark sin bucket), la imagen se reduce y se guarda como texto Base64 dentro del
 * documento de Firestore. Así la funcionalidad de fotos nunca se interrumpe.
 */
object ImagenesFirebase {

    private const val TAG = "ImagenesFirebase"
    private const val LADO_MAXIMO_STORAGE = 1080
    private const val LADO_MAXIMO_RESPALDO = 480
    private const val TIEMPO_MAXIMO_SUBIDA_MS = 20_000L

    /**
     * Sube [foto] a la ruta indicada de Storage. Llama a [alTerminar] con (url, "") si la subida
     * funcionó, o con ("", base64) si fue necesario usar el respaldo en Firestore.
     */
    fun subir(ruta: String, foto: Bitmap, alTerminar: (url: String, base64: String) -> Unit) {
        val bytes = comprimir(escalar(foto, LADO_MAXIMO_STORAGE), 82)
        try {
            val storage = FirebaseStorage.getInstance()
            storage.maxUploadRetryTimeMillis = TIEMPO_MAXIMO_SUBIDA_MS
            val referencia = storage.reference.child(ruta)
            val metadatos = StorageMetadata.Builder().setContentType("image/jpeg").build()
            referencia.putBytes(bytes, metadatos)
                .continueWithTask { tarea ->
                    // Si la subida falló se propaga el error; si no, se pide la URL pública de descarga
                    if (!tarea.isSuccessful) throw tarea.exception ?: IllegalStateException("Subida fallida")
                    referencia.downloadUrl
                }
                .addOnSuccessListener { uri -> alTerminar(uri.toString(), "") }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Storage no disponible, se usa respaldo Base64", error)
                    alTerminar("", aBase64(foto))
                }
        } catch (e: Exception) {
            // Proyecto sin bucket de Storage configurado en google-services.json
            Log.w(TAG, "Storage no configurado, se usa respaldo Base64", e)
            alTerminar("", aBase64(foto))
        }
    }

    /** Versión reducida de la foto codificada en Base64 (≈ 30-60 KB, muy por debajo de 1 MB). */
    private fun aBase64(foto: Bitmap): String =
        Base64.encodeToString(comprimir(escalar(foto, LADO_MAXIMO_RESPALDO), 65), Base64.NO_WRAP)

    /** Comprime un Bitmap como JPEG con la calidad indicada (0-100). */
    private fun comprimir(foto: Bitmap, calidad: Int): ByteArray {
        val salida = ByteArrayOutputStream()
        foto.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        return salida.toByteArray()
    }

    /** Reduce el Bitmap para que su lado más largo no supere [ladoMaximo], conservando la proporción. */
    fun escalar(foto: Bitmap, ladoMaximo: Int): Bitmap {
        val mayor = maxOf(foto.width, foto.height)
        if (mayor <= ladoMaximo) return foto
        val factor = ladoMaximo.toFloat() / mayor
        return Bitmap.createScaledBitmap(foto, (foto.width * factor).toInt(), (foto.height * factor).toInt(), true)
    }

    /**
     * Lee una imagen desde un Uri (foto de la cámara o archivo de la galería) sin cargarla completa
     * en memoria (inSampleSize) y la endereza según su orientación EXIF.
     */
    fun cargarDesdeUri(contexto: Context, uri: Uri, ladoMaximo: Int = 1600): Bitmap? {
        val resolver = contexto.contentResolver
        return try {
            // 1. Solo se leen las dimensiones para calcular el factor de reducción
            val opcionesTamano = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcionesTamano) }
            var muestra = 1
            while (maxOf(opcionesTamano.outWidth, opcionesTamano.outHeight) / muestra > ladoMaximo) muestra *= 2

            // 2. Se decodifica la imagen reducida
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            val imagen = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                ?: return null

            // 3. Se corrige la rotación que algunas cámaras guardan solo en los metadatos EXIF
            val grados = resolver.openInputStream(uri)?.use { flujo ->
                when (ExifInterface(flujo).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            if (grados == 0f) imagen else {
                val matriz = Matrix().apply { postRotate(grados) }
                Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matriz, true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la imagen", e)
            null
        }
    }
}
