# ArtisTech Colombia · Tienda virtual Android

Marketplace móvil nativo de **artesanías y productos independientes colombianos**. Este repositorio contiene la **aplicación completa y funcional** (proyecto final, Actividad 6): el frontend de la Actividad 4 conectado a un backend en **Firebase**, con geolocalización, autenticación biométrica y pasarela de pagos **ePayco**, según la planeación de la Actividad 2 para la asignatura *Desarrollo de Aplicaciones Móviles Nativas* (Fundación Universitaria Compensar).

> *"Lo hecho a mano, cerca de ti"*

**Autor:** Tomás Santiago Martínez Rodríguez · **Docente:** Flavio Alexander Navarro Carmona

---

## Tecnologías

| Elemento | Detalle |
|---|---|
| Lenguaje | Kotlin 2.0 |
| Interfaces | **XML** (layouts, drawables, estilos y values) + ViewBinding · Material Components 3 |
| Arquitectura | Capas `ui` → `data` (patrón Repository + Singleton) → Firebase · escuchas en tiempo real (`addSnapshotListener`) y patrón observador hacia las pantallas |
| Autenticación | **Firebase Authentication** (correo/contraseña, recuperación por correo) + **BiometricPrompt** con credenciales cifradas (`EncryptedSharedPreferences`, AES-256) |
| Base de datos | **Cloud Firestore**: colecciones `usuarios`, `usuarios/{uid}/carrito`, `productos`, `pedidos` |
| Archivos | **Cloud Storage for Firebase** (fotos de productos y de perfil) con respaldo Base64 en Firestore |
| Geolocalización | Google Play Services `FusedLocationProviderClient` + `Geocoder` + Google Maps (Intent `geo:`) |
| Pagos | **ePayco** (checkout oficial en `WebView`, modo pruebas) + validación de la transacción por `ref_payco` |
| Imágenes | Coil 2.7 (carga y caché de imágenes desde URL) |
| SDK | minSdk 26 (Android 8.0) · targetSdk / compileSdk 35 |
| Build | Gradle 8.11 (Kotlin DSL) + `libs.versions.toml` · AGP 8.7.3 · plugin `google-services` 4.4.2 · Firebase BoM 33.7.0 |

## Funcionalidades (requisitos de la Actividad 6)

| # | Componente | Implementación |
|---|---|---|
| 1 | Autenticación | Registro, inicio y cierre de sesión y recuperación de contraseña con Firebase Authentication para los tres roles. El rol y el estado se leen de `usuarios/{uid}`; las cuentas suspendidas o eliminadas no pueden ingresar. La sesión se restaura en el splash. |
| 2 | Huella dactilar | `BiometricPrompt` (BIOMETRIC_WEAK). Tras el primer ingreso con contraseña, las credenciales quedan cifradas con el Android Keystore y la huella las libera para iniciar sesión. También confirma pagos. |
| 3 | CRUD de usuarios | El Administrador crea (instancia secundaria de `FirebaseApp` para no cerrar su sesión), lista, busca, edita rol/estado y elimina perfiles en Firestore. |
| 4 | CRUD de productos | Vendedor y Administrador crean, editan y eliminan productos con foto desde **cámara** (`TakePicture` + `FileProvider`) o **galería** (`PickVisualMedia`), subida a Firebase Storage. Moderación: aprobar/rechazar. |
| 5 | Carrito y órdenes | Carrito persistente en `usuarios/{uid}/carrito`. La compra se confirma con una **transacción** de Firestore que valida y descuenta el stock y crea un pedido por vendedor en `pedidos`. Historial para Comprador, Vendedor y Administrador. |
| 6 | Geolocalización | La ubicación del Comprador se convierte en dirección y se guarda en su perfil y en cada pedido (dirección + coordenadas). El Vendedor abre la entrega en Google Maps. |
| 7 | Pasarela de pagos | ePayco (tarjeta, PSE, Nequi, Daviplata) en modo pruebas. La app intercepta la URL de respuesta y **confirma la transacción** en `https://secure.epayco.co/validation/v1/reference/{ref_payco}`. Si se aprueba (o queda pendiente) se registra el pedido con la referencia; si se rechaza, el carrito se conserva. También existe pago contra entrega. |

## Configuración de Firebase (una sola vez)

1. Crear un proyecto en <https://console.firebase.google.com> y registrar una app Android con el paquete `com.artistech.colombia`.
2. Descargar `google-services.json` y copiarlo en la carpeta `app/`.
3. **Authentication** → Método de acceso → habilitar *Correo electrónico/contraseña*.
4. **Firestore Database** → crear la base de datos y publicar las reglas de [`firebase/firestore.rules`](firebase/firestore.rules).
5. **Storage** (requiere plan Blaze) → crear el bucket y publicar [`firebase/storage.rules`](firebase/storage.rules). Si no se habilita, la app guarda las fotos comprimidas en Firestore automáticamente.
6. Crear el primer administrador: registrarse desde la app y, en Firestore, cambiar el campo `rol` de su documento en `usuarios` a `ADMIN`.
7. (Opcional) Desde el Dashboard del Administrador, botón **Cargar catálogo de ejemplo en Firestore**.

La llave pública de ePayco se puede reemplazar sin tocar el código agregando `EPAYCO_PUBLIC_KEY=tu_llave` en `gradle.properties`.

## Cuentas de demostración

En el inicio de sesión hay chips de **acceso rápido** que llenan estos datos (las cuentas se crean una vez en Firebase con esta contraseña).

| Rol | Correo | Contraseña |
|---|---|---|
| Comprador | `comprador@artistech.co` | `Artis2026` |
| Vendedor | `vendedor@artistech.co` | `Artis2026` |
| Administrador | `admin@artistech.co` | `Artis2026` |

- **Cupón de descuento:** `ARTESANO10` (10 %).
- **Huella:** inicia sesión una vez con contraseña; después usa *Ingresar con huella* (dispositivo físico con huella registrada).

## Modelo de datos en Firestore

```
usuarios/{uid}            nombres, email, telefono, tipoDocumento, numeroDocumento, rol, estado,
                          nombreTaller, rut, region, oficio, direccion, latitud, longitud, fotoUrl, creadoEn
  └─ carrito/{productoId} productoId, cantidad, actualizadoEn
productos/{id}            nombre, descripcion, precio, stock, categoria, region, tecnica, tiempoElaboracion,
                          vendedorId, vendedorNombre, estado, calificacion, resenas, fotoUrl, creadoEn
pedidos/{id}              codigo, compradorId, compradorNombre, vendedorId, vendedorNombre, items[],
                          total, fechaMillis, metodoPago, estadoPago, referenciaPago,
                          direccionEntrega, latitud, longitud, categoria, estado
```

## Estructura del proyecto

```
app/src/main/
├── java/com/artistech/colombia/
│   ├── data/            Modelos (mapeo a Firestore), Repositorio (Firebase + caché en tiempo real), CatalogoDemo
│   ├── util/            Biometría y credenciales cifradas, imágenes (Storage), Ubicacion (GPS),
│   │                    PagoEpayco, errores de Firebase, formato COP y extensiones
│   └── ui/
│       ├── splash/      SplashActivity (restaura la sesión)
│       ├── auth/        Login (contraseña y huella), Registro, RecuperarContrasena
│       ├── comprador/   Inicio (GPS), Catálogo, Detalle, Carrito, Pasarela, EpaycoActivity, Confirmación
│       ├── vendedor/    Productos, Pedidos recibidos, ProductoForm (cámara/galería)
│       ├── admin/       Dashboard, Usuarios, UsuarioForm, Productos (moderación), Reporte de ventas
│       └── comun/       PerfilFragment y adaptadores compartidos
└── res/                 layouts XML, drawables, values (colors, dimens, strings, styles, themes), xml/file_paths
firebase/                firestore.rules y storage.rules
```

## Cómo ejecutar

1. Clonar: `git clone https://github.com/Kronus2812/ArtisTechColombia.git` (o descomprimir el .zip de la entrega).
2. Verificar que exista `app/google-services.json`.
3. Abrir la carpeta en **Android Studio** y esperar la sincronización de Gradle.
4. Ejecutar ▶ en un dispositivo físico con Android 8.0 o superior (la huella y el GPS requieren hardware real).
5. Generar el APK: **Build → Generate App Bundles or APKs → Generate APKs** (`app/build/outputs/apk/debug/app-debug.apk`).

## Normatividad considerada

- Ley 1581 de 2012 (Habeas Data): autorización no premarcada en el registro, eliminación de cuenta, reglas de seguridad por rol y huella procesada solo en el dispositivo.
- Ley 1480 de 2011: precio total con IVA, resumen previo al pago y confirmación con referencia de la transacción.
- PCI DSS: los datos de tarjeta se digitan únicamente en el checkout de ePayco; la app nunca los recibe ni los guarda.
- Ley 2300 de 2023: las notificaciones promocionales son opcionales.
