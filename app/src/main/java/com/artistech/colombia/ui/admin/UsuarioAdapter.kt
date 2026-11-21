package com.artistech.colombia.ui.admin

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.artistech.colombia.data.Usuario
import com.artistech.colombia.databinding.ItemUsuarioBinding
import com.artistech.colombia.util.pintarInsignia

/** Adaptador del listado de usuarios del Administrador. */
class UsuarioAdapter(
    private var usuarios: List<Usuario>,
    private val alEditar: (Usuario) -> Unit,
    private val alEliminar: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    /** Contenedor de las vistas de una fila (patrón ViewHolder con ViewBinding). */
    class UsuarioViewHolder(val b: ItemUsuarioBinding) : RecyclerView.ViewHolder(b.root)

    /** Infla el layout de la fila con ViewBinding. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder =
        UsuarioViewHolder(ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Número de elementos que muestra la lista. */
    override fun getItemCount(): Int = usuarios.size

    /** Llena la fila con iniciales, nombre, correo, rol y estado, y conecta editar/eliminar. */
    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        val b = holder.b
        b.tvIniciales.text = usuario.iniciales
        b.tvNombre.text = usuario.nombres
        b.tvEmail.text = usuario.email
        b.tvRol.text = usuario.rol.etiqueta
        b.tvEstado.pintarInsignia(usuario.estado.etiqueta, usuario.estado.fondo, usuario.estado.texto)
        b.root.setOnClickListener { alEditar(usuario) }
        b.btnEditar.setOnClickListener { alEditar(usuario) }
        b.btnEliminar.setOnClickListener { alEliminar(usuario) }
    }

    /** Reemplaza la lista de usuarios mostrada. */
    @SuppressLint("NotifyDataSetChanged")
    fun actualizar(nuevos: List<Usuario>) {
        usuarios = nuevos
        notifyDataSetChanged()
    }
}
