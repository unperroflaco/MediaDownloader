package es.upf.mediadownloader

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URI
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var home: LinearLayout
    private lateinit var browser: LinearLayout
    private lateinit var urlInput: EditText
    private lateinit var address: EditText
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var browserStatus: TextView
    private var inBrowser = false
    private var lastUrl = "https://www.google.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildHome()
        buildBrowser()
        showHome()
        if (savedInstanceState != null) lastUrl = savedInstanceState.getString("lastUrl") ?: lastUrl
        requestNotificationPermission()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("lastUrl", if (::webView.isInitialized) webView.url ?: lastUrl else lastUrl)
        super.onSaveInstanceState(outState)
    }

    private fun buildHome() {
        home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(22))
            setBackgroundColor(0xFFF5F7FA.toInt())
        }
        home.addView(TextView(this).apply {
            text = "MediaDownloader"
            textSize = 27f
            setTextColor(0xFF172033.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        home.addView(TextView(this).apply {
            text = "Navega por Internet y descarga vídeos"
            textSize = 14f
            setTextColor(0xFF667085.toInt())
            setPadding(0, dp(6), 0, dp(18))
        })
        home.addView(Button(this).apply {
            text = "ABRIR NAVEGADOR"
            setOnClickListener { showBrowser() }
        }, matchWrap())
        home.addView(Button(this).apply {
            text = "ABRIR FACEBOOK"
            setOnClickListener { openFacebook() }
        }, matchWrap())
        home.addView(TextView(this).apply {
            text = "DESCARGA DIRECTA"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF344054.toInt())
            setPadding(0, dp(24), 0, dp(8))
        })
        home.addView(TextView(this).apply {
            text = "Pega la dirección directa de un archivo de vídeo HTTP o HTTPS."
            textSize = 14f
            setTextColor(0xFF667085.toInt())
        })
        urlInput = EditText(this).apply {
            hint = "https://.../video.mp4"
            textSize = 15f
            minLines = 2
            maxLines = 3
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        home.addView(urlInput, matchWrap())
        home.addView(Button(this).apply {
            text = "PEGAR ENLACE"
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (cb.hasPrimaryClip()) urlInput.setText(cb.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity))
                else showStatus("El portapapeles está vacío.")
            }
        }, matchWrap())
        home.addView(Button(this).apply {
            text = "DESCARGAR"
            setOnClickListener { startDownload(urlInput.text.toString().trim()) }
        }, matchWrap())
        status = TextView(this).apply {
            text = "Preparado."
            textSize = 14f
            setTextColor(0xFF344054.toInt())
            setPadding(0, dp(14), 0, dp(8))
        }
        home.addView(status)
        home.addView(TextView(this).apply {
            text = "Los archivos se guardan en Movies/MediaDownloader. Descarga únicamente contenido propio o para el que tengas autorización."
            textSize = 12f
            setTextColor(0xFF667085.toInt())
            setPadding(0, dp(18), 0, 0)
        })
    }

    private fun buildBrowser() {
        browser = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(4), dp(5), dp(4))
            setBackgroundColor(0xFFF1F3F6.toInt())
        }
        bar.addView(smallButton("‹") { if (webView.canGoBack()) webView.goBack() }, buttonParams(38))
        bar.addView(smallButton("›") { if (webView.canGoForward()) webView.goForward() }, buttonParams(38))
        bar.addView(smallButton("↻") { webView.reload() }, buttonParams(38))
        address = EditText(this).apply {
            hint = "Dirección o búsqueda"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(0xFFFFFFFF.toInt())
            setOnEditorActionListener { _, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    navigate(address.text.toString())
                    true
                } else false
            }
        }
        bar.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        bar.addView(smallButton("Ir") { navigate(address.text.toString()) }, buttonParams(45))
        browser.addView(bar)

        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), 0, dp(4), dp(3))
            setBackgroundColor(0xFFF1F3F6.toInt())
        }
        shortcuts.addView(smallButton("Inicio") { webView.loadUrl("https://www.google.com") }, buttonParams(0, 1f))
        shortcuts.addView(smallButton("Facebook") { openFacebook() }, buttonParams(0, 1f))
        shortcuts.addView(smallButton("Descargas") { showHome() }, buttonParams(0, 1f))
        browser.addView(shortcuts)

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        browser.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))

        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, value: Int) {
                progress.progress = value
                progress.visibility = if (value >= 100) View.GONE else View.VISIBLE
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) browserStatus.text = title
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase(Locale.ROOT)
                if (scheme == "http" || scheme == "https") {
                    if (isFacebook(uri)) {
                        openFacebook(uri)
                        return true
                    }
                    return false
                }
                if (scheme == "mailto" || scheme == "tel") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                }
                return true
            }
            override fun onPageStarted(view: WebView?, url: String?, icon: android.graphics.Bitmap?) {
                if (url != null) {
                    lastUrl = url
                    address.setText(url)
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) {
                    lastUrl = url
                    address.setText(url)
                }
            }
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, disposition, mimeType, _ ->
            if (url.startsWith("https://") || url.startsWith("http://")) {
                downloadFromBrowser(url, userAgent, disposition, mimeType)
            } else {
                Toast.makeText(this, "Este tipo de descarga no está disponible.", Toast.LENGTH_LONG).show()
            }
        })
        browser.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        browserStatus = TextView(this).apply {
            text = "Navegador integrado"
            textSize = 11f
            setTextColor(0xFF667085.toInt())
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        browser.addView(browserStatus)
    }

    private fun showHome() {
        inBrowser = false
        if (::webView.isInitialized && webView.url != null) lastUrl = webView.url!!
        setContentView(home)
    }

    private fun showBrowser() {
        inBrowser = true
        setContentView(browser)
        if (webView.url.isNullOrBlank()) webView.loadUrl(lastUrl)
        address.clearFocus()
    }

    private fun navigate(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        val target = when {
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            value.contains(" ") || !value.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(value)
            else -> "https://" + value
        }
        val uri = Uri.parse(target)
        hideBrowserKeyboard()
        if (isFacebook(uri)) openFacebook(uri) else webView.loadUrl(target)
    }

    private fun openFacebook(uri: Uri = Uri.parse("https://www.facebook.com/")) {
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
            browserStatus.text = "Facebook abierto en una pestaña segura"
        } catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: Exception) { Toast.makeText(this, "No se ha podido abrir Facebook.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun isFacebook(uri: Uri): Boolean {
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        return host == "facebook.com" || host.endsWith(".facebook.com") ||
            host == "fb.watch" || host.endsWith(".fb.watch")
    }

    private fun downloadFromBrowser(url: String, userAgent: String?, disposition: String?, mimeType: String?) {
        try {
            val name = android.webkit.URLUtil.guessFileName(url, disposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setDescription("Descargado desde MediaDownloader")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "MediaDownloader/" + name)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) request.addRequestHeader("Cookie", cookies)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Descarga iniciada: " + name, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo descargar: " + (e.localizedMessage ?: "error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun startDownload(raw: String) {
        if (raw.isBlank()) { showStatus("Introduce primero un enlace."); return }
        val uri = try { URI(raw) } catch (_: Exception) {
            showStatus("El enlace no tiene un formato válido."); return
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            showStatus("Solo se admiten enlaces HTTP o HTTPS."); return
        }
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        if (host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch") {
            showStatus("Abre Facebook desde el botón Facebook. El descargador admite direcciones directas de archivos."); return
        }
        try {
            val name = buildFileName(Uri.parse(raw))
            val request = DownloadManager.Request(Uri.parse(raw))
                .setTitle(name)
                .setDescription("MediaDownloader")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "MediaDownloader/" + name)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            showStatus("Descarga enviada al gestor de Android. Consulta las notificaciones.")
            hideMainKeyboard()
        } catch (e: Exception) {
            showStatus("No se pudo iniciar la descarga: " + (e.localizedMessage ?: "error desconocido"))
        }
    }

    private fun buildFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "video"
        name = name.substringAfterLast('/').substringBefore('?').replace(Regex("[^A-Za-z0-9._-]"), "_")
        if (name.isBlank() || name == "video") name = "video_" + System.currentTimeMillis() + ".mp4"
        if (!name.contains(".")) name += ".mp4"
        return name.take(100)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
    }

    private fun showStatus(message: String) { status.text = message }

    private fun hideMainKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
        urlInput.clearFocus()
    }

    private fun hideBrowserKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(address.windowToken, 0)
        address.clearFocus()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (inBrowser) {
            if (webView.canGoBack()) webView.goBack() else showHome()
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun smallButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        minWidth = 0
        minimumWidth = 0
        setPadding(dp(3), 0, dp(3), 0)
        setOnClickListener { action() }
    }

    private fun buttonParams(width: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(if (weight > 0f) 0 else dp(width), dp(44), weight)

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
