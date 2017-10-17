package com.ridi.inyoung.epub.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
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
    private val CHROME_51 = 270400000

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var context: EpubParser.Context
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

        /*if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            // - 젤리빈(4.1.x) 이하에서 '페이지 넘김 <-> 스크롤 보기' 전환 시 이전 페이지의 scroll을 가지고 있어 형광펜이 틀어지고 엉뚱한 페이지로 이동하는 문제가 있다
            //   (페이지 넘김 상태인데 pageOffset 크기의 y값이 있고 스크롤 보기인데 x값이 있음)
            scrollTo(0, 0)
        } else if (EPubApplication.systemWebViewVersionCode >= CHROME_51) {
            // - Chrome 51에서 스파인 이동 시 이전 scroll 값이 유지되고 있어 형광펜이 틀어지고 엉뚱한 페이지로 이동하는 문제가 있다
            //   (scrollTo로 호출해도 되긴 하는데 간헐적으로 window.pageOffset이 갱신되기 전에 loadData가 호출되어 원복되는 문제가 있어 injectJS로 이동시킨다
            //    그럼 젤리빈도 injectJS로 이동하면 되지 않나 싶지만 안 된다...)
            scrollToPageOffset(0)
        }*/

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
            loadSpine(currentSpineIndex)
        }

        if (scrollY + measuredHeight >= contentHeight * scale) {
            if (currentSpineIndex < context.spines.size - 1) {
                currentSpineIndex++
            }
            loadSpine(currentSpineIndex)
        }

        super.onScrollChanged(l, t, oldl, oldt)
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

    private fun scrollToPageOffset(pageOffset: Int) {
        injectJs("scrollAbsY($pageOffset)")
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