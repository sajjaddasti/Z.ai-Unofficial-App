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

    var launchFileChooser: ((Intent) -> Unit)? = null
    var launchPermissions: ((Array<String>) -> Unit)? = null

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

        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                isLoading = true
                errorMessage = null
            }

            override fun onPageCommitVisible(v: WebView?, url: String?) {
                isLoading = false
            }

            override fun onPageFinished(v: WebView?, url: String?) {
                isLoading = false
                canGoBack = v?.canGoBack() == true
                CookieManager.getInstance().flush()
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
        webView?.destroy()
        webView = null
    }

    private fun openExternal(context: Context, uri: Uri) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
