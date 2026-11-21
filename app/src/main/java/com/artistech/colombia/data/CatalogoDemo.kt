package com.artistech.colombia.data

/**
 * Catálogo de ejemplo con 12 productos artesanales colombianos (los mismos de la Actividad 4).
 * El Administrador puede cargarlo en Firestore desde el Dashboard para poblar una base de datos
 * nueva; cada producto queda asignado al vendedor que el Administrador elija.
 */
object CatalogoDemo {

    /** Construye la lista de productos con el vendedor indicado. El id vacío lo genera Firestore. */
    fun productos(vendedorId: String, vendedorNombre: String): List<Producto> {
        // Pequeño desfase de tiempo para conservar el orden de la lista al ordenar por fecha
        val base = System.currentTimeMillis()
        /** Atajo para construir cada producto de ejemplo con el vendedor elegido. */
        fun p(
            indice: Int, nombre: String, descripcion: String, precio: Long, stock: Int, categoria: Categoria,
            region: String, tecnica: String, tiempo: String, estado: EstadoProducto, calificacion: Double, resenas: Int
        ) = Producto(
            id = "", nombre = nombre, descripcion = descripcion, precio = precio, stock = stock,
            categoria = categoria, region = region, tecnica = tecnica, tiempoElaboracion = tiempo,
            vendedorId = vendedorId, vendedorNombre = vendedorNombre, estado = estado,
            calificacion = calificacion, resenas = resenas, creadoEn = base - indice * 1000L
        )

        return listOf(
            p(1, "Mochila wayúu tradicional",
                "Tejida en crochet con hilo acrílico por artesanas wayúu. Diseño de kanaas (figuras geométricas) que representan elementos de la cultura de La Guajira. 25 × 22 cm, con gaza ajustable.",
                185_000, 12, Categoria.TEJIDOS, "Uribia, La Guajira", "Crochet en hilo acrílico", "20 días",
                EstadoProducto.PUBLICADO, 4.9, 126),
            p(2, "Chinchorro wayúu",
                "Chinchorro tejido a mano con flecos decorativos. Resistente y cómodo, ideal para terrazas y zonas de descanso.",
                420_000, 3, Categoria.TEJIDOS, "Manaure, La Guajira", "Tejido de malla en telar", "30 días",
                EstadoProducto.PUBLICADO, 4.8, 41),
            p(3, "Sombrero vueltiao 23 vueltas",
                "Sombrero elaborado con fibra de caña flecha trenzada. Símbolo cultural de Colombia, de 23 vueltas y ala ancha.",
                240_000, 7, Categoria.TEJIDOS, "Tuchín, Córdoba", "Trenzado en caña flecha", "15 días",
                EstadoProducto.EN_REVISION, 0.0, 0),
            p(4, "Olla de barro La Chamba",
                "Olla de cerámica negra bruñida a mano, apta para cocinar y servir. Producto con denominación de origen.",
                95_000, 9, Categoria.CERAMICA, "El Guamo, Tolima", "Cerámica negra bruñida", "8 días",
                EstadoProducto.PUBLICADO, 4.7, 58),
            p(5, "Juego de platos en cerámica negra",
                "Set de 4 platos de cerámica negra con acabado brillante, elaborados en torno y quemados a leña.",
                160_000, 5, Categoria.CERAMICA, "El Guamo, Tolima", "Torno y quema a leña", "10 días",
                EstadoProducto.PUBLICADO, 4.6, 22),
            p(6, "Canasto en palma de iraca",
                "Canasto tejido en palma de iraca con tapa. Perfecto para organizar y decorar espacios.",
                78_000, 15, Categoria.CESTERIA, "Sandoná, Nariño", "Tejido en palma de iraca", "6 días",
                EstadoProducto.PUBLICADO, 4.5, 37),
            p(7, "Aretes en filigrana momposina",
                "Aretes de plata 950 elaborados con la técnica de filigrana, tradición orfebre de Mompox.",
                130_000, 0, Categoria.JOYERIA, "Mompox, Bolívar", "Filigrana en plata", "10 días",
                EstadoProducto.AGOTADO, 5.0, 19),
            p(8, "Mola guna tradicional",
                "Mola elaborada con la técnica de capas superpuestas de tela cosidas a mano, inspirada en la fauna del Darién.",
                150_000, 4, Categoria.TEJIDOS, "Necoclí, Antioquia", "Capas superpuestas cosidas a mano", "12 días",
                EstadoProducto.PUBLICADO, 4.8, 33),
            p(9, "Tabla de cocina en guayacán",
                "Tabla de cortar en madera de guayacán con acabado en aceite mineral grado alimenticio.",
                68_000, 20, Categoria.MADERA, "Pereira, Risaralda", "Tallado y lijado a mano", "4 días",
                EstadoProducto.PUBLICADO, 4.4, 15),
            p(10, "Hamaca San Jacinto",
                "Hamaca de algodón tejida en telar vertical con los colores tradicionales de los Montes de María.",
                320_000, 6, Categoria.TEJIDOS, "San Jacinto, Bolívar", "Telar vertical en algodón", "18 días",
                EstadoProducto.PUBLICADO, 4.9, 64),
            p(11, "Lámina ilustrada Fauna de Colombia",
                "Ilustración original impresa en papel de algodón de 300 g. Edición limitada y firmada.",
                55_000, 25, Categoria.DISENO, "Bogotá D.C.", "Ilustración digital e impresión giclée", "3 días",
                EstadoProducto.EN_REVISION, 0.0, 0),
            p(12, "Collar en tagua",
                "Collar de semillas de tagua talladas y teñidas con pigmentos naturales del Pacífico colombiano.",
                89_000, 10, Categoria.JOYERIA, "Quibdó, Chocó", "Tallado en tagua", "5 días",
                EstadoProducto.PUBLICADO, 4.6, 12)
        )
    }
}
