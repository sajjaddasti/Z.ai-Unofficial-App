package com.unofficial.zai.ui.web

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
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
    var webViewMajor by mutableStateOf(0)

    /** Set when the page loaded but could not paint anything (old WebView engines). */
    var renderFailed by mutableStateOf(false)

    /** True once the paint probe has reported (or timed out) for the current page. */
    var paintChecked by mutableStateOf(false)

    /** Bumped per navigation so a stale timeout cannot mark the new page as checked. */
    private var pageGen = 0

    var launchFileChooser: ((Intent) -> Unit)? = null
    var launchPermissions: ((Array<String>) -> Unit)? = null

    /** Recent JS console / error messages, newest last. Shown in Settings for troubleshooting. */
    val consoleLines = androidx.compose.runtime.mutableStateListOf<String>()

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var permissionRequest: PermissionRequest? = null

    fun obtain(context: Context): WebView {
        webView?.let { return it }

        val view = WebView(context)
        with(view.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // Keep the real WebView UA (honest UA avoids sites serving a broken page)
            // and only append an app marker.
            userAgentString = WebSettings.getDefaultUserAgent(context) +
                " ZaiUnofficial/${BuildConfig.VERSION_NAME}"
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        log("UA=" + view.settings.userAgentString)
        runCatching {
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)?.let {
                webViewVersion = it.versionName ?: ""
                webViewMajor = webViewVersion.substringBefore('.').toIntOrNull() ?: 0
                log("WebView pkg=${it.packageName} v=${it.versionName}")
            }
        }


        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                isLoading = true
                errorMessage = null
                renderFailed = false
                paintChecked = false
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
                // Report what actually ended up in the DOM, so a failed hydration is visible.
                v?.evaluateJavascript(
                    """(function(){try{
                        var b=document.body;
                        var h=document.documentElement;
                        return 'DOM htmlLen='+(h?h.outerHTML.length:-1)
                          +' bodyLen='+(b?b.innerHTML.length:-1)
                          +' text="'+((b&&b.innerText)?b.innerText.slice(0,90).replace(/\s+/g,' '):'none')+'"';
                    }catch(e){return 'DOM_ERR '+e.message}})()"""
                ) { result -> log("DOM_RESULT $result") }
                // Detect a page that technically loaded but painted nothing (happens on old
                // System WebView builds). Reported back over the console channel. A second
                // probe runs only when the first one looks blank, so slow hydration is not
                // mistaken for a dead engine.
                v?.evaluateJavascript(
                    """(function(){
                        function probe(tag){
                            try{
                                function hits(x,y){
                                    try{ return document.elementsFromPoint(x,y).length; }catch(e){ return -1; }
                                }
                                var w=innerWidth, h=innerHeight;
                                var c=hits(w/2,h/2), a=hits(w*0.25,h*0.6), b=hits(w*0.75,h*0.4);
                                var painted=0;
                                try{
                                    var r=document.createRange();
                                    r.selectNodeContents(document.body||document.documentElement);
                                    var rects=r.getClientRects();
                                    for(var i=0;i<rects.length;i++){
                                        if(rects[i].width>6&&rects[i].height>6) painted++;
                                    }
                                }catch(e){}
                                var failed = ((c<=1&&a<=1&&b<=1) || painted===0);
                                console.log('PAINTCHECK'+tag+' c='+c+' a='+a+' b='+b+' painted='+painted+' failed='+(failed?1:0));
                                return failed;
                            }catch(e){
                                console.log('PAINTCHECK'+tag+' err='+e.message);
                                return false;
                            }
                        }
                        setTimeout(function(){
                            if (probe('')) {
                                setTimeout(function(){ probe('2'); }, 1500);
                            }
                        }, 900);
                    })()"""
                ) { }
                val gen = pageGen
                v?.postDelayed({
                    if (gen == pageGen && !paintChecked) {
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
                val text = "JS ${message?.messageLevel()}: ${message?.message()} " +
                    "(${message?.sourceId()}:${message?.lineNumber()})"
                log(text)
                val raw = message?.message().orEmpty()
                if (raw.startsWith("PAINTCHECK")) {
                    if (raw.contains("err=")) {
                        // Probe failed for an unrelated reason: never block the UI on it.
                        renderFailed = false
                        paintChecked = true
                        return true
                    }
                    val secondPass = raw.startsWith("PAINTCHECK2")
                    val failed = Regex("failed=(\\d)").find(raw)?.groupValues
                        ?.getOrNull(1) == "1"
                    if (failed && !secondPass) {
                        // Give hydration another moment before blaming the engine.
                        log("RENDER_RETRY $raw")
                    } else if (failed) {
                        renderFailed = true
                        paintChecked = true
                        log("RENDER_FAILED $raw")
                    } else {
                        renderFailed = false
                        paintChecked = true
                    }
                }
                return true
            }

            override fun onProgressChanged(v: WebView?, newProgress: Int) {
                progress = newProgress
                isLoading = newProgress < 100
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

    fun deliverFileSelection(uris: Array<Uri>?) {        fileCallback?.onReceiveValue(uris)
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
        webView?.destroy()
        webView = null
    }

    private fun log(line: String) {
        android.util.Log.i("ZaiWeb", line)
        if (consoleLines.size >= 80) consoleLines.removeAt(0)
        consoleLines.add(line)
    }

    private fun openExternal(context: Context, uri: Uri) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
