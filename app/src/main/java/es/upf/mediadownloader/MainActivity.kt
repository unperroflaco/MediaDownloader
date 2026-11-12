package es.upf.mediadownloader

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.net.URI
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button
    private var pendingUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        requestNotificationPermission()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(22))
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        val title = TextView(this).apply {
            text = "MediaDownloader"
            textSize = 27f
            setTextColor(0xFF172033.toInt())
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Descarga vídeos directamente en tu dispositivo"
            textSize = 14f
            setTextColor(0xFF667085.toInt())
            setPadding(0, dp(6), 0, dp(24))
        }
        root.addView(subtitle)

        val label = TextView(this).apply {
            text = "ENLACE DEL VÍDEO"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF344054.toInt())
        }
        root.addView(label)

        urlInput = EditText(this).apply {
            hint = "Pega aquí una URL de vídeo..."
            textSize = 15f
            singleLine = false
            minLines = 2
            maxLines = 3
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val inputParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        inputParams.topMargin = dp(8)
        root.addView(urlInput, inputParams)

        val pasteButton = Button(this).apply {
            text = "PEGAR ENLACE"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clipboard.hasPrimaryClip()) {
                    urlInput.setText(clipboard.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity))
                } else {
                    showStatus("El portapapeles está vacío.")
                }
            }
        }
        root.addView(pasteButton, matchWrap())

        val qualityLabel = TextView(this).apply {
            text = "CALIDAD"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF344054.toInt())
            setPadding(0, dp(20), 0, dp(6))
        }
        root.addView(qualityLabel)

        val qualitySpinner = Spinner(this)
        val qualityOptions = listOf(
            "Calidad original (sin conversión)",
            "720p (requiere conversión)",
            "480p (requiere conversión)",
            "360p (requiere conversión)"
        )
        qualitySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, qualityOptions
        )
        qualitySpinner.isEnabled = false
        root.addView(qualitySpinner, matchWrap())

        val qualityNote = TextView(this).apply {
            text = "En esta primera versión se conserva la calidad original. La conversión con FFmpeg llegará en una actualización."
            textSize = 12f
            setTextColor(0xFF667085.toInt())
            setPadding(0, dp(6), 0, dp(12))
        }
        root.addView(qualityNote)

        downloadButton = Button(this).apply {
            text = "ANALIZAR Y DESCARGAR"
            setOnClickListener { startDownload() }
        }
        root.addView(downloadButton, matchWrap())

        statusText = TextView(this).apply {
            text = "Preparado. Pega un enlace para comenzar."
            textSize = 14f
            setTextColor(0xFF344054.toInt())
            setPadding(0, dp(18), 0, dp(8))
        }
        root.addView(statusText)

        val info = TextView(this).apply {
            text = "Los vídeos se guardan en Movies/MediaDownloader. Descarga únicamente contenido propio o para el que tengas autorización."
            textSize = 12f
            setTextColor(0xFF667085.toInt())
            setPadding(0, dp(18), 0, 0)
        }
        root.addView(info)

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun startDownload() {
        val raw = urlInput.text.toString().trim()
        if (raw.isEmpty()) {
            showStatus("Introduce primero un enlace.")
            return
        }

        val uri = try {
            URI(raw)
        } catch (_: Exception) {
            showStatus("El enlace no tiene un formato válido.")
            return
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            showStatus("Solo se admiten enlaces HTTP o HTTPS.")
            return
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val isFacebookPage = host == "facebook.com" ||
            host.endsWith(".facebook.com") ||
            host == "fb.watch" ||
            host.endsWith(".fb.watch")

        if (isFacebookPage) {
            showStatus(
                "Has pegado un enlace de Facebook. Esta versión no extrae vídeos de páginas o publicaciones. " +
                "Utiliza una opción de descarga autorizada de Facebook o facilita una URL directa del archivo de vídeo."
            )
            return
        }

        pendingUrl = raw
        enqueueDownload(raw)
    }

    private fun enqueueDownload(raw: String) {
        try {
            val uri = Uri.parse(raw)
            val fileName = buildFileName(uri)
            val request = DownloadManager.Request(uri)
                .setTitle(fileName)
                .setDescription("MediaDownloader")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES, "MediaDownloader/$fileName"
                )

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            showStatus("Descarga enviada al gestor de Android. Consulta las notificaciones para ver el progreso.")
            hideKeyboard()
        } catch (e: Exception) {
            showStatus("No se pudo iniciar la descarga: ${e.localizedMessage ?: "error desconocido"}")
        }
    }

    private fun buildFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "video"
        name = name.substringAfterLast('/').substringBefore('?')
        name = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        if (name.isBlank() || name == "video") {
            name = "video_${System.currentTimeMillis()}.mp4"
        }
        if (!name.contains('.')) name += ".mp4"
        return name.take(100)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun showStatus(message: String) {
        statusText.text = message
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
        urlInput.clearFocus()
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
