# ArtisTech Colombia · Tienda virtual Android

Marketplace móvil nativo de **artesanías y productos independientes colombianos**. Este repositorio contiene el **frontend completo** de la aplicación (Actividad 4), construido a partir de la planeación de la Actividad 2 para la asignatura *Desarrollo de Aplicaciones Móviles Nativas* (Fundación Universitaria Compensar).

> *"Lo hecho a mano, cerca de ti"*

**Autor:** Tomás Santiago Martínez Rodríguez · **Docente:** Flavio Alexander Navarro Carmona

---

## Tecnologías

| Elemento | Detalle |
|---|---|
| Lenguaje | Kotlin 2.0 |
| Interfaces | **XML** (layouts, drawables, estilos y values) + ViewBinding |
| Componentes | Material Components 3 (1.12), ConstraintLayout, RecyclerView, Fragments |
| Hardware | BiometricPrompt (huella), cámara (`TakePicturePreview`), permiso de ubicación |
| SDK | minSdk 26 (Android 8.0) · targetSdk / compileSdk 35 |
| Build | Gradle 8.11 (Kotlin DSL) + catálogo de versiones `libs.versions.toml` · AGP 8.7.3 |

## Cuentas de demostración

En el inicio de sesión hay chips de **acceso rápido** que llenan estos datos automáticamente.

| Rol | Correo | Contraseña |
|---|---|---|
| Comprador | `comprador@artistech.co` | `Artis2026` |
| Vendedor | `vendedor@artistech.co` | `Artis2026` |
| Administrador | `admin@artistech.co` | `Artis2026` |

- **Recuperar contraseña:** el código de demostración es `123456`.
- **Cupón de descuento:** `ARTESANO10` (10 %).
- **Huella:** inicia sesión una vez con contraseña. Después, el botón *Ingresar con huella* usa `BiometricPrompt`. En el emulador, registra una huella en *Ajustes → Seguridad* y simula el toque desde *Extended controls → Fingerprint*.

## Pantallas por rol

| Rol | Pantallas | Navegación |
|---|---|---|
| **Transversal** | Splash · Inicio de sesión con huella · Registro · Recuperación de contraseña (2 pasos) | — |
| **Comprador** | Inicio (búsqueda, GPS, banner, categorías, destacados) · Catálogo (búsqueda y filtros) · Detalle de producto · Carrito (CRUD + cupón) · Pasarela de pagos (tarjeta, PSE, billetera, efectivo, pago con huella) · Confirmación · Perfil (CRUD, foto con cámara, mis pedidos) | Bottom Navigation |
| **Vendedor** | Panel de productos (CRUD completo con indicadores, búsqueda y filtros) · Formulario de producto (foto con cámara o galería) · Historial de pedidos recibidos (cambio de estado) · Perfil de vendedor (datos del taller) | Bottom Navigation |
| **Administrador** | Dashboard (indicadores, alertas, accesos rápidos) · Gestión de usuarios (listado, creación, edición, eliminación) · Gestión de productos (moderación: aprobar/rechazar, editar, eliminar) · Reporte de ventas (gráfico de barras, ventas por categoría, mejores vendedores, exportar CSV) · Perfil | Navigation Drawer |

## Estructura del proyecto

```
app/src/main/
├── java/com/artistech/colombia/
│   ├── data/            Modelos (enums de estado con colores) y Repositorio en memoria (CRUD)
│   ├── util/            Formato COP, extensiones, biometría y preferencias
│   └── ui/
│       ├── splash/      SplashActivity
│       ├── auth/        Login, Registro, RecuperarContrasena
│       ├── comprador/   CompradorActivity + Inicio, Catálogo, Carrito, Detalle, Pasarela, Confirmación
│       ├── vendedor/    VendedorActivity + Productos, Pedidos, ProductoForm
│       ├── admin/       AdminActivity + Dashboard, Usuarios, Productos, Reporte, UsuarioForm
│       └── comun/       PerfilFragment y adaptadores compartidos
└── res/
    ├── layout/          32 layouts (activities, fragments, ítems de lista, encabezado del drawer)
    ├── drawable/        77 recursos: íconos vectoriales, degradados, selectores, ripple, layer-list
    ├── values/          colors, dimens, strings (≈340 textos), styles, themes
    ├── values-v35/      ajuste de tema para Android 15
    ├── color/           selectores de color por estado (checked / focused)
    ├── font/            Poppins (títulos) e Inter (cuerpo), licencia SIL OFL
    ├── menu/            Bottom Navigation (Comprador, Vendedor) y Drawer (Administrador)
    └── mipmap-anydpi-v26/  ícono adaptativo
```

## Sistema de diseño (recursos centralizados)

- **`colors.xml`:** paleta propia inspirada en materiales artesanales: terracota `#B4472A` (primario), verde selva `#1F5F4A` (confianza y GPS), mostaza `#E0A526` (acento), crema `#FBF6EE` (fondo) y café `#2B2118` (texto). No se usa ningún color por defecto de la plantilla.
- **`themes.xml`:** hereda de `Theme.Material3.Light.NoActionBar` y reasigna todos los roles de color, la tipografía y el estilo por defecto de cada componente (`materialButtonStyle`, `textInputStyle`, `chipStyle`, `bottomNavigationStyle`…).
- **`styles.xml`:** apariencias de texto (`TextAppearance.ArtisTech.*`), variantes de botón (primario, verde, contorno, texto, peligro, toggle), campos de texto, tarjetas, chips, FAB, switch, insignias y filas de resumen.
- **`dimens.xml`:** espaciados, radios, grosores, tamaños de íconos y contenedores, y tamaños de texto en `sp`. Ningún layout ni estilo usa valores de `dp` o `sp` escritos directamente.
- **`strings.xml`:** todos los textos, incluidos plurales (`plurals`), listas (`string-array`) y descripciones de accesibilidad.
- **Drawables:** `shape` con degradados (fondos por categoría), `selector` (métodos de pago), `ripple` (filas de lista), `layer-list` (banner promocional), bordes punteados (zona de cámara) y 46 íconos `vector`.

## Cómo ejecutar

1. Clonar: `git clone https://github.com/Kronus2812/ArtisTechColombia.git`
2. Abrir la carpeta en **Android Studio** y esperar la sincronización de Gradle.
3. Ejecutar ▶ en un emulador o dispositivo con Android 8.0 o superior.
4. Generar el APK: **Build → Generate App Bundles or APKs → Generate APKs**. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Alcance de esta entrega

Esta entrega corresponde al **frontend**. Los datos viven en memoria (`data/Repositorio.kt`) para que todas las pantallas sean navegables y funcionales sin backend. La integración con Firebase / API REST, la pasarela de pagos real y la geolocalización con coordenadas reales están planeadas para la siguiente fase, según la Actividad 2.

## Normatividad considerada en la interfaz

- Ley 1581 de 2012 (Habeas Data): autorización no premarcada en el registro, eliminación de cuenta y huella procesada solo en el dispositivo.
- Ley 1480 de 2011: precio total con IVA, resumen previo al pago y aviso del derecho de retracto.
- Ley 2300 de 2023: las notificaciones promocionales son opcionales.
