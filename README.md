# MediaDownloader Android

Aplicación Android nativa en Kotlin.

## Versión 0.2.0

- Navegador integrado basado en Android WebView.
- Barra de direcciones y búsqueda, atrás, adelante, recargar e inicio.
- JavaScript y almacenamiento web habilitados para compatibilidad con páginas modernas.
- Detección de enlaces de Facebook: se abren en Chrome Custom Tabs (o en el navegador predeterminado si Custom Tabs no está disponible), evitando el bloqueo habitual del inicio de sesión en WebView.
- Descarga de enlaces directos desde el navegador integrado mediante Android DownloadManager.
- Las descargas se guardan en `Movies/MediaDownloader`.
- Se conserva la pantalla de descarga directa de versiones anteriores.

## Facebook: qué hace y qué no hace

Facebook se abre en una pestaña segura del navegador del dispositivo. Esto permite iniciar sesión y navegar con la sesión que gestione el navegador. Al cerrar la pestaña se vuelve a MediaDownloader.

Por las restricciones de inicio de sesión de Meta, Facebook no se fuerza dentro del WebView. Los vídeos reproducidos dentro de la pestaña segura de Facebook no se interceptan ni se extraen automáticamente desde MediaDownloader. Si Facebook ofrece una descarga para un contenido propio, utiliza esa opción. El navegador integrado sí puede descargar enlaces directos que una página entregue al WebView.

No se implementan bypass de DRM, acceso a contenido privado ni evasión de restricciones de plataformas.

## Limitaciones conocidas

- Las descargas desde el WebView dependen de que la página exponga una URL descargable normal. Los vídeos servidos mediante segmentos, blobs, DRM o reproductores protegidos no se convierten automáticamente en archivos.
- La selección de calidad y la conversión FFmpeg siguen pendientes.
- La autenticación y las cookies de Facebook se gestionan en la pestaña segura del navegador, no se copian ni se extraen hacia el WebView.

## Compilar

Requiere JDK 17 y Gradle 8.9 o compatible.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

También se puede ejecutar el workflow `Build MediaDownloader APK` desde GitHub Actions.
