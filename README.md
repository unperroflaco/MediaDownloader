# MediaDownloader Android

Primera base del proyecto Android, sin servidor externo.

## Funcionalidad de esta versión

- Interfaz Android nativa en Kotlin.
- Pegado de enlaces desde el portapapeles.
- Descarga de URL directas HTTP/HTTPS mediante Android DownloadManager.
- Guardado en `Movies/MediaDownloader`.
- Reconocimiento de enlaces de Facebook y aviso de que esta versión no extrae publicaciones ni páginas.
- Workflow de GitHub Actions para generar el APK de depuración.

## Limitaciones conocidas

- Esta versión inicial descarga URL directas; no extrae vídeos desde páginas de Facebook.
- No implementa bypass de DRM, autenticación, contenido privado ni restricciones de plataformas.
- La selección de 720p/480p/360p aparece deshabilitada: la descarga directa conserva la calidad del archivo servido.
- FFmpeg no está integrado todavía. Se incorporará después de elegir y verificar una distribución Android mantenida, compatible con la licencia y ABI del proyecto.
- El gestor de descargas de Android requiere que el servidor de origen permita la descarga y entregue un archivo accesible sin sesión privada.

## Compilar

Requiere JDK 17 y Gradle 8.9 o compatible.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

También se puede ejecutar el workflow `Build MediaDownloader APK` desde GitHub Actions.
