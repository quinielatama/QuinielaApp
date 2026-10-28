package mx.quiniela.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true      // para que la web guarde datos (localStorage)
            allowFileAccess = false
        }
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Puente(), "Puente")

        web.webViewClient = object : WebViewClient() {

            // Links de WhatsApp y otros esquemas se abren fuera de la app
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                val host = url.host ?: ""
                val esMiWeb = host == Uri.parse(BuildConfig.URL_WEB).host
                if (esMiWeb) return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (e: Exception) {
                    false
                }
            }

            // Pantalla simple cuando no hay internet
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadDataWithBaseURL(
                        null,
                        """
                        <html><body style='font-family:sans-serif;text-align:center;padding-top:30vh'>
                        <h2>Sin conexión</h2>
                        <p style='color:#5B6575'>Revisa tu internet y vuelve a intentar.</p>
                        <button onclick="location.href='${BuildConfig.URL_WEB}'"
                          style='margin-top:16px;background:#0A7A3B;color:#fff;border:0;border-radius:14px;padding:14px 28px;font-size:17px'>
                          Reintentar</button>
                        </body></html>
                        """.trimIndent(),
                        "text/html", "utf-8", null
                    )
                }
            }
        }

        // Botón atrás navega dentro de la web
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (savedInstanceState == null) web.loadUrl(BuildConfig.URL_WEB)
    }

    /** Desde la web se llama así: Puente.enviarWhatsApp("523312345678", "Mi quiniela...") */
    inner class Puente {
        @JavascriptInterface
        fun enviarWhatsApp(telefono: String, mensaje: String) {
            val uri = Uri.parse("https://wa.me/$telefono?text=" + Uri.encode(mensaje))
            runOnUiThread {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
