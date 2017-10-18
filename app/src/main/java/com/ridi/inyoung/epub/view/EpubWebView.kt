package com.ridi.inyoung.epub.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import com.ridi.books.helper.Log
import com.ridi.inyoung.epub.EPubApplication
import com.ridi.inyoung.epub.util.EpubParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by inyoung on 2017. 10. 16..
 */

class EpubWebView : WebView {
    interface PageChangeListener {
        fun onPrevPage(spineIndex: Int)
        fun onNextPage(spineIndex: Int)
    }
    private val CHROME_51 = 270400000

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var context: EpubParser.Context
    lateinit var pageChangeListener: PageChangeListener
    private var dragging = false
    private var currentSpineIndex = 0

    init {
        setHorizontalScrollBarEnabled(false)
        isVerticalScrollBarEnabled = false
        setVerticalScrollbarOverlay(true)
        overScrollMode = View.OVER_SCROLL_NEVER

        val settings = settings
        settings.javaScriptEnabled = true
        settings.pluginState = WebSettings.PluginState.ON
        settings.setSupportZoom(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.allowFileAccessFromFileURLs = true
        }
        settings.setNeedInitialFocus(false)

        // KITKAT 이상에서 접근성->큰 텍스트 선택 시 이 값이 조정되어 크기가 커지는데 pagination 요소에 포함되어 있지 않아 강제한다
        settings.textZoom = 100

        webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView, url: String, message: String,
                                   result: JsResult): Boolean = true

            override fun onJsConfirm(view: WebView, url: String, message: String,
                                     result: JsResult): Boolean = true


            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String,
                                    result: JsPromptResult): Boolean = true


            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(EpubWebView::class.java, consoleMessage.message()
                        + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId())
                return true
            }
        }
    }

    fun loadSpine(index: Int = 0) {
        currentSpineIndex = index

        val curSpine = context.spines[index]
        try {
            val html = curSpine.getHtml()
            loadDataWithBaseURL(curSpine.baseUrl, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        if (scrollY <= 0) {
            if (currentSpineIndex > 0) {
                currentSpineIndex --
            }
            pageChangeListener.onPrevPage(currentSpineIndex)
        }

        if (scrollY + measuredHeight >= contentHeight * scale) {
            if (currentSpineIndex < context.spines.size - 1) {
                currentSpineIndex++
            }
            pageChangeListener.onNextPage(currentSpineIndex)
        }

        super.onScrollChanged(l, t, oldl, oldt)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.action
        when (action) {
            MotionEvent.ACTION_MOVE ->
                if (dragging.not()) {
                    dragging = true
                }
            MotionEvent.ACTION_UP ->
                dragging = false
        }
        return super.onTouchEvent(event)
    }

    fun loadJsModule() {
        loadJavascriptModule("epub")
    }

    fun injectJs(script: String) {
        val src = ("try { " +
                script + " } catch (e) { console.error('catch exception : ' + e); console.error(e.stack); }")

        src.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                evaluateJavascript(it, null)
            } else {
                loadUrl("javascript: " + it)
            }
        }
    }

    fun scrollToTop() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            scrollTo(0, 1)
        } else if (EPubApplication.systemWebViewVersionCode >= CHROME_51) {
            scrollToPageOffset(1)
        }
    }

    fun scrollToPageOffset(pageOffset: Int) {
        val padding = 20f
        val offset = Math.max(
                ((pageOffset - 1) * (height / scale) ) - padding, padding)
        injectJs("scrollAbsY($offset)")
    }

    private fun loadJavascriptModule(name: String) {
        val script = StringBuilder()
        try {
            val inputStream = getContext().assets.open("javascripts/$name.js")
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach {
                    script.append(it)
                    script.append('\n')
                }
            }
        } catch (e: IOException) {
            Log.e(EpubWebView::class.java, e)
        }

        injectJs(script.toString())
    }
}