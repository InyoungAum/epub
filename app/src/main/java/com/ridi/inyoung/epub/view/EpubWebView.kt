package com.ridi.inyoung.epub.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.*
import com.ridi.books.helper.Log
import com.ridi.inyoung.epub.model.EpubSpine
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by inyoung on 2017. 10. 16..
 */

class EpubWebView : WebView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var spine: EpubSpine

    init {
        setHorizontalScrollBarEnabled(false);
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
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return true
            }

            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return true
            }

            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String,
                                    result: JsPromptResult): Boolean {
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(EpubWebView::class.java, consoleMessage.message()
                        + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId())
                return true
            }
        }
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

    private fun loadJavascriptModule(name: String) {
        val script = StringBuilder()
        try {
            val inputStream = context.assets.open("javascripts/$name.js")
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