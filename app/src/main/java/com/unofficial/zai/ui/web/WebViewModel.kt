package com.unofficial.zai.ui.web

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.unofficial.zai.BuildConfig

class WebViewModel(app: Application) : AndroidViewModel(app) {

    var webView: WebView? = null
        private set

    var isLoading by mutableStateOf(true)
    var progress by mutableStateOf(0)
    var errorMessage by mutableStateOf<String?>(null)
    var canGoBack by mutableStateOf(false)

    /** Live page title / URL, used for the header subtitle. */
    var pageTitle by mutableStateOf("")
    var currentUrl by mutableStateOf("")

    /** Package name + version of the System WebView in use, e.g. "110.0.5481.154". */
    var webViewVersion by mutableStateOf("")
    var webViewPackageName by mutableStateOf("")
    var webViewMajor by mutableStateOf(0)

    /**
     * True only when an old engine (< Chromium 115) loaded the page but could not
     * paint anything. Modern engines are never accused: they get [blankHint] instead.
     */
    var renderFailed by mutableStateOf(false)

    /** True when a modern engine finished loading yet still painted nothing. */
    var blankHint by mutableStateOf(false)

    /** True once the paint probe has reported (or timed out) for the current page. */
    var paintChecked by mutableStateOf(false)

    /** True once the page proved it has visible content. */
    var pagePainted by mutableStateOf(false)

    /** Last paint probe report, kept for the diagnostics clipboard. */
    var lastProbe by mutableStateOf("")

    /** Desktop-site UA mode, persisted between launches. */
    var desktopMode by mutableStateOf(false)
        private set

    /** Bumped when the renderer dies so Compose builds a fresh WebView. */
    var webViewGeneration by mutableStateOf(0)
        private set

    var dialog by mutableStateOf<JsDialog?>(null)

    var launchFileChooser: ((Intent) -> Unit)? = null
    var launchPermissions: ((Array<String>) -> Unit)? = null

    /** Recent JS console / error messages, newest last. Shown in Settings for troubleshooting. */
    val consoleLines = androidx.compose.runtime.mutableStateListOf<String>()

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var permissionRequest: PermissionRequest? = null

    /** Bumped per navigation so a stale timeout cannot mark the new page as checked. */
    private var pageGen = 0

    init {
        desktopMode = prefs().getBoolean(KEY_DESKTOP, false)
    }

    fun obtain(context: Context): WebView {
        webView?.let { return it }

        val view = WebView(context)
        with(view.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            // Popups (OAuth, share targets) are routed back into this same view.
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // Keep the stock WebView UA, byte for byte. Extra markers make some
            // sites treat the app as an unknown client.
            userAgentString = if (desktopMode) DESKTOP_UA else defaultUa(context)
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        detectEngine(context, view)
        log("UA=" + view.settings.userAgentString)

        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                isLoading = true
                errorMessage = null
                renderFailed = false
                blankHint = false
                paintChecked = false
                pagePainted = false
                lastProbe = ""
                pageGen++
                log("PAGE_START $url")
            }

            override fun onPageCommitVisible(v: WebView?, url: String?) {
                isLoading = false
                log("PAGE_COMMIT_VISIBLE $url")
            }

            override fun doUpdateVisitedHistory(v: WebView?, url: String?, isReload: Boolean) {
                canGoBack = v?.canGoBack() == true
                currentUrl = url.orEmpty()
                pageTitle = v?.title.orEmpty()
            }

            override fun onPageFinished(v: WebView?, url: String?) {
                isLoading = false
                canGoBack = v?.canGoBack() == true
                pageTitle = v?.title.orEmpty()
                currentUrl = url.orEmpty()
                CookieManager.getInstance().flush()
                log("PAGE_FINISHED $url title=${v?.title}")
                // Log what actually ended up in the DOM, so a failed hydration is visible.
                v?.evaluateJavascript(
                    """(function(){try{
                        var b=document.body;
                        var h=document.documentElement;
                        return 'DOM htmlLen='+(h?h.outerHTML.length:-1)
                          +' bodyLen='+(b?b.innerHTML.length:-1)
                          +' text="'+((b&&b.innerText)?b.innerText.slice(0,90).replace(/\s+/g,' '):'none')+'"';
                    }catch(e){return 'DOM_ERR '+e.message}})()"""
                ) { result -> log("DOM_RESULT $result") }

                // A single early probe is not evidence: SPAs can spend several seconds
                // hydrating on a slow phone or network. Probe repeatedly; only the
                // final attempt is allowed to declare failure.
                v?.evaluateJavascript(PAINT_PROBE_JS) { }
                val gen = pageGen
                v?.postDelayed({
                    if (gen == pageGen && !paintChecked) {
                        // Never hide the page behind an opaque error screen because of
                        // a slow hydration. Just reveal whatever was rendered.
                        paintChecked = true
                        log("PAINT_TIMEOUT (no probe report)")
                    }
                }, 7000)
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                log(
                    "RENDER_PROCESS_GONE didCrash=${detail?.didCrash()} " +
                        "priority=${detail?.rendererPriorityAtExit()}"
                )
                // The renderer is dead; this WebView instance is unusable. Drop it and
                // let Compose build a new one in a fresh process.
                webView = null
                runCatching { view?.destroy() }
                blankHint = false
                renderFailed = false
                paintChecked = false
                pagePainted = false
                isLoading = true
                webViewGeneration++
                return true
            }

            override fun onReceivedError(
                v: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    isLoading = false
                    errorMessage = "Couldn't load the page (${error?.errorCode}). " +
                        (error?.description?.toString() ?: "")
                    log("MAIN_FRAME_ERROR ${error?.errorCode} ${error?.description} ${request.url}")
                } else {
                    log("SUB_ERROR ${error?.errorCode} ${request?.url}")
                }
            }

            override fun onReceivedHttpError(
                v: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    isLoading = false
                    errorMessage = "Server returned HTTP ${errorResponse?.statusCode}"
                }
            }

            override fun shouldOverrideUrlLoading(
                v: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                return when (uri.scheme) {
                    "http", "https" -> false // keep inside the WebView
                    else -> {
                        openExternal(context, uri)
                        true
                    }
                }
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                val raw = message?.message().orEmpty()
                if (raw.startsWith("PAINTCHECK")) {
                    handleProbe(raw)
                    return true
                }
                val text = "JS ${message?.messageLevel()}: $raw " +
                    "(${message?.sourceId()}:${message?.lineNumber()})"
                log(text)
                return true
            }

            override fun onProgressChanged(v: WebView?, newProgress: Int) {
                progress = newProgress
                isLoading = newProgress < 100
            }

            override fun onCreateWindow(
                v: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // window.open() / target=_blank: load the destination in the main view
                // instead of silently dropping it (this is how some OAuth pages work).
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val main = webView ?: return false
                val opener = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            popup: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            main.loadUrl(url)
                            return true
                        }
                    }
                }
                transport.webView = opener
                resultMsg.sendToTarget()
                return true
            }

            override fun onJsAlert(
                v: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                if (result == null) return false
                dialog = JsDialog(JsDialog.Kind.Alert, message.orEmpty(), "", result)
                return true
            }

            override fun onJsConfirm(
                v: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                if (result == null) return false
                dialog = JsDialog(JsDialog.Kind.Confirm, message.orEmpty(), "", result)
                return true
            }

            override fun onJsPrompt(
                v: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                if (result == null) return false
                dialog = JsDialog(
                    JsDialog.Kind.Prompt,
                    message.orEmpty(),
                    defaultValue.orEmpty(),
                    result
                )
                return true
            }

            override fun onShowFileChooser(
                v: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                if (callback == null) return false
                val launcher = launchFileChooser ?: return false
                fileCallback = callback
                val intent = try {
                    params?.createIntent()
                } catch (e: Exception) {
                    null
                } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                return try {
                    launcher(intent)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                permissionRequest = request
                val wanted = request.resources.filter {
                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                        it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                }
                if (wanted.isEmpty()) {
                    request.deny()
                    return
                }
                val perms = wanted.map {
                    if (it == PermissionRequest.RESOURCE_AUDIO_CAPTURE) android.Manifest.permission.RECORD_AUDIO
                    else android.Manifest.permission.CAMERA
                }.toTypedArray()
                launchPermissions?.invoke(perms) ?: request.deny()
            }
        }

        view.setDownloadListener { url, userAgent, _, mimeType, _ ->
            runCatching {
                val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    CookieManager.getInstance().getCookie(url)?.let {
                        addRequestHeader("Cookie", it)
                    }
                    setNotificationVisibility(
                        android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    setDestinationInExternalFilesDir(
                        context,
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                        Uri.parse(url).lastPathSegment ?: "download"
                    )
                }
                (context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager)
                    .enqueue(request)
                android.widget.Toast.makeText(context, "Download started", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        webView = view
        return view
    }

    /** Resets per-page verdicts so a manual retry shows the loader again. */
    fun resetPageState() {
        renderFailed = false
        blankHint = false
        paintChecked = false
        pagePainted = false
        lastProbe = ""
        errorMessage = null
        isLoading = true
    }

    fun setDesktopMode(context: Context, enabled: Boolean) {
        if (desktopMode == enabled) return
        desktopMode = enabled
        prefs().edit().putBoolean(KEY_DESKTOP, enabled).apply()
        val view = webView ?: return
        view.settings.userAgentString = if (enabled) DESKTOP_UA else defaultUa(context)
        log("UA_MODE desktop=$enabled UA=${view.settings.userAgentString}")
        resetPageState()
        view.reload()
    }

    fun diagnostics(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("z.ai unofficial ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine(
            "Device: ${Build.MANUFACTURER} ${Build.MODEL} / Android " +
                "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
        sb.appendLine("WebView package: ${webViewPackageName.ifBlank { "unknown" }}")
        sb.appendLine(
            "WebView version: ${webViewVersion.ifBlank { "unknown" }} " +
                "(major $webViewMajor)"
        )
        val ua = runCatching { webView?.settings?.userAgentString }
            .getOrNull() ?: runCatching { WebSettings.getDefaultUserAgent(context) }
            .getOrDefault("unknown")
        sb.appendLine("UA: $ua")
        sb.appendLine("Desktop mode: $desktopMode")
        sb.appendLine("Painted: $pagePainted / checked: $paintChecked")
        sb.appendLine("Probe: ${lastProbe.ifBlank { "none" }}")
        sb.appendLine("Page: ${currentUrl.ifBlank { "none" }}")
        sb.appendLine("--- console ---")
        consoleLines.takeLast(40).forEach { sb.appendLine(it) }
        return sb.toString()
    }

    fun deliverFileSelection(uris: Array<Uri>?) {
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    fun cancelFileSelection() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
    }

    fun onPermissionResult(granted: Boolean) {
        val request = permissionRequest
        permissionRequest = null
        if (request == null) return
        if (granted) request.grant(request.resources) else request.deny()
    }

    override fun onCleared() {
        super.onCleared()
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        dialog?.resolve(false)
        dialog = null
        webView?.destroy()
        webView = null
    }

    /* -------------------------------------------------------------------------------------- */

    private fun handleProbe(raw: String) {
        lastProbe = raw
        if (raw.contains("err=")) {
            // The probe itself failed; never turn that into a verdict.
            paintChecked = true
            return
        }
        val attempt = Regex("a=(\\d+)").find(raw)?.groupValues?.getOrNull(1)
            ?.toIntOrNull() ?: 0
        val ok = Regex("ok=(\\d)").find(raw)?.groupValues?.getOrNull(1) == "1"
        if (ok) {
            pagePainted = true
            paintChecked = true
            renderFailed = false
            blankHint = false
            return
        }
        if (attempt < PROBE_ATTEMPTS) return
        paintChecked = true
        if (webViewMajor in 1..OLD_ENGINE_MAX) {
            renderFailed = true
        } else {
            // No version (unknown provider) or a current engine: offer help, do not
            // accuse the device and do not cover the page.
            blankHint = true
        }
        log("RENDER_FAILED engine=$webViewVersion $raw")
    }

    private fun detectEngine(context: Context, view: WebView) {
        val pkg = runCatching {
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)
        }.getOrNull()
        webViewPackageName = pkg?.packageName.orEmpty()
        var version = pkg?.versionName.orEmpty()
        if (version.isBlank()) {
            version = Regex("Chrome/([\\w.]+)")
                .find(view.settings.userAgentString)
                ?.groupValues?.getOrNull(1)
                .orEmpty()
        }
        webViewVersion = version
        webViewMajor = version.substringBefore('.').toIntOrNull() ?: 0
        log("WebView pkg=${webViewPackageName.ifBlank { "?" }} v=${version.ifBlank { "?" }}")
    }

    private fun defaultUa(context: Context): String =
        runCatching { WebSettings.getDefaultUserAgent(context) }
            .getOrDefault(
                "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"
            )

    private fun prefs() = getApplication<Application>()
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun log(line: String) {
        android.util.Log.i("ZaiWeb", line)
        if (consoleLines.size >= 120) consoleLines.removeAt(0)
        consoleLines.add(line)
    }

    private fun openExternal(context: Context, uri: Uri) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    class JsDialog(
        val kind: Kind,
        val message: String,
        val defaultValue: String,
        private val result: JsResult
    ) {
        enum class Kind { Alert, Confirm, Prompt }

        fun resolve(accept: Boolean, text: String = "") {
            when (kind) {
                Kind.Alert -> result.confirm()
                Kind.Confirm -> if (accept) result.confirm() else result.cancel()
                Kind.Prompt -> {
                    val prompt = result as? JsPromptResult
                    if (prompt == null) {
                        if (accept) result.confirm() else result.cancel()
                    } else if (accept) {
                        prompt.confirm(text)
                    } else {
                        prompt.cancel()
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_FILE = "zai_prefs"
        private const val KEY_DESKTOP = "desktop_ua"
        private const val PROBE_ATTEMPTS = 6

        /** Chromium versions at or below this cannot lay out the z.ai stylesheet. */
        const val OLD_ENGINE_MAX = 114

        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val PAINT_PROBE_JS = """
            (function(){
              var attempt = 0;
              var delays = [1100, 1400, 1800, 2200, 2600, 3000];
              function hits(x,y){
                try{ return document.elementsFromPoint(x,y).length; }catch(e){ return -1; }
              }
              function paintedRects(){
                try{
                  var r = document.createRange();
                  r.selectNodeContents(document.body || document.documentElement);
                  var rects = r.getClientRects(), n = 0;
                  for (var i=0;i<rects.length;i++){
                    if (rects[i].width > 6 && rects[i].height > 6) n++;
                  }
                  return n;
                }catch(e){ return -1; }
              }
              function tick(){
                attempt++;
                var w = innerWidth || 1, h = innerHeight || 1;
                var c = hits(w/2,h/2), a = hits(w*0.25,h*0.6), b = hits(w*0.75,h*0.4);
                var painted = paintedRects();
                var len = 0;
                try { len = ((document.body && document.body.innerHTML) || '').length; }catch(e){}
                var failed = ((c <= 1 && a <= 1 && b <= 1) || painted === 0);
                console.log('PAINTCHECK a=' + attempt + ' ok=' + (failed ? 0 : 1) +
                  ' c=' + c + ',' + a + ',' + b + ' rects=' + painted + ' len=' + len);
                if (failed && attempt < $PROBE_ATTEMPTS) {
                  setTimeout(tick, delays[attempt] || 3000);
                }
              }
              setTimeout(tick, 900);
            })()
        """.trimIndent()
    }
}
