package com.carrot233.travelmemory.data.place

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.carrot233.travelmemory.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference

sealed interface PlaceSearchOutcome {
    data object Unavailable : PlaceSearchOutcome
    data class Hits(val places: List<PlaceSuggestion>) : PlaceSearchOutcome
    data object Empty : PlaceSearchOutcome
    data class Failed(val message: String) : PlaceSearchOutcome
}

object AmapPlaceSearch {
    private const val unavailableMessage =
        "地点搜索不可用：构建时未配置 AMAP_KEY 或 AMAP_SECURITY_CODE。仍可手动填写经纬度。"

    private val gate = Mutex()
    private var webView: WebView? = null
    private var host: WeakReference<Activity>? = null
    private var ready = CompletableDeferred<Unit>()
    private var pending: CompletableDeferred<PlaceSearchOutcome>? = null

    fun isConfigured(): Boolean =
        BuildConfig.AMAP_KEY.isNotBlank() && BuildConfig.AMAP_SECURITY_CODE.isNotBlank()

    fun unavailableMessage(): String = unavailableMessage

    suspend fun search(context: Context, keyword: String): PlaceSearchOutcome {
        if (!isConfigured()) return PlaceSearchOutcome.Unavailable
        val query = keyword.trim()
        if (query.isEmpty()) return PlaceSearchOutcome.Empty
        return gate.withLock {
            withContext(Dispatchers.Main) {
                try {
                    val view = ensureWebView(context)
                    withTimeout(20_000) { ready.await() }
                    val result = CompletableDeferred<PlaceSearchOutcome>()
                    pending = result
                    view.evaluateJavascript("startSearch(${jsStringLiteral(query)})", null)
                    withTimeout(12_000) { result.await() }
                } catch (error: TimeoutCancellationException) {
                    PlaceSearchOutcome.Failed("搜索失败，请检查网络后重试")
                } catch (error: Exception) {
                    PlaceSearchOutcome.Failed("搜索失败，请重试")
                } finally {
                    pending = null
                }
            }
        }
    }

    fun close() {
        val view = webView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
        webView = null
        host = null
        ready = CompletableDeferred()
        pending?.complete(PlaceSearchOutcome.Failed("搜索失败，请重试"))
        pending = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView(context: Context): WebView {
        val activity = findActivity(context)
        val existing = webView
        if (existing != null && (activity == null || host?.get() === activity)) return existing
        existing?.let { previous ->
            (previous.parent as? ViewGroup)?.removeView(previous)
            previous.destroy()
            ready = CompletableDeferred()
        }
        val created = WebView(activity ?: context.applicationContext)
        created.settings.javaScriptEnabled = true
        created.settings.domStorageEnabled = true
        created.addJavascriptInterface(Bridge(), "AndroidBridge")
        created.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame) {
                    fail("搜索失败，请检查网络后重试")
                }
            }
        }
        if (activity != null) {
            (activity.window.decorView as ViewGroup).addView(
                created,
                FrameLayout.LayoutParams(1, 1),
            )
            created.alpha = 0f
        }
        val html = context.assets.open("amap_search.html").bufferedReader().use { it.readText() }
            .replace("{{AMAP_KEY}}", BuildConfig.AMAP_KEY)
            .replace("{{AMAP_SECURITY_CODE}}", BuildConfig.AMAP_SECURITY_CODE)
        created.loadDataWithBaseURL(
            "https://webapi.amap.com/",
            html,
            "text/html",
            "UTF-8",
            null,
        )
        webView = created
        host = activity?.let { WeakReference(it) }
        return created
    }

    private fun fail(message: String) {
        pending?.complete(PlaceSearchOutcome.Failed(message))
        if (!ready.isCompleted) ready.complete(Unit)
    }

    private class Bridge {
        @JavascriptInterface
        fun onReady() {
            if (!ready.isCompleted) ready.complete(Unit)
        }

        @JavascriptInterface
        fun onResults(json: String) {
            val places = parsePlaceSuggestions(json)
            pending?.complete(
                if (places.isEmpty()) PlaceSearchOutcome.Empty else PlaceSearchOutcome.Hits(places),
            )
        }

        @JavascriptInterface
        fun onFailure(message: String) {
            pending?.complete(PlaceSearchOutcome.Failed(publicFailure(message)))
        }
    }

    private fun publicFailure(message: String): String {
        if (Regex("[A-Za-z0-9]{8,}").containsMatchIn(message)) return "搜索失败，请重试"
        return message.take(40).ifBlank { "搜索失败，请重试" }
    }

    private fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
