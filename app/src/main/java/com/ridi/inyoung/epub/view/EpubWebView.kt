package com.ridi.inyoung.epub.view

import android.content.Context
import android.os.Build
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import com.ridi.books.helper.Log
import com.ridi.inyoung.epub.util.EpubParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.properties.Delegates

/**
 * Created by inyoung on 2017. 10. 16..
 */

class EpubWebView : WebView {
    interface PageChangeListener {
        fun onPrevSpine(spineIndex: Int)
        fun onNextSpine(spineIndex: Int)
    }
    private val CHROME_51 = 270400000

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var context: EpubParser.Context
    lateinit var pageChangeListener: PageChangeListener

    private var dragging = false
    private var scrolling = false
    private var currentSpineIndex = 0
    private var preScrollPosY = 0
    private var spineLoaded = true
    private var scrollTask: Runnable by Delegates.notNull()

    init {
        setHorizontalScrollBarEnabled(false)
        isVerticalScrollBarEnabled = false
        setVerticalScrollbarOverlay(true)
        overScrollMode = View.OVER_SCROLL_NEVER


        settings.run {
            javaScriptEnabled = true
            pluginState = WebSettings.PluginState.ON
            setSupportZoom(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                allowFileAccessFromFileURLs = true
            }
            setNeedInitialFocus(false)

            // KITKAT 이상에서 접근성->큰 텍스트 선택 시 이 값이 조정되어 크기가 커지는데 pagination 요소에 포함되어 있지 않아 강제한다
            textZoom = 100
        }

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

        scrollTask = Runnable {
            if (scrolling) {
                if (scrollY == preScrollPosY) {
                    if (dragging.not()) {
                        scrolling = false
                    } else {
                        postDelayed(scrollTask, 100)
                    }
                } else {
                    preScrollPosY = scrollY
                    postDelayed(scrollTask, 100)
                }
            }
        }
    }

    fun loadSpine(index: Int = 0) {
        preScrollPosY = 0
        currentSpineIndex = index
        scrolling = false
        dragging = false
        spineLoaded = true

        alpha = 0f
        ViewCompat.animate(this)
                .setDuration(500 * 2)
                .alpha(1f)

        val curSpine = context.spines[index]
        try {
            val html = curSpine.getHtml()
            loadDataWithBaseURL(curSpine.baseUrl, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (spineLoaded) {
            spineLoaded = false
            return
        }

        if (t == 0 && oldt == 0) {
            return
        }
        if (!scrolling) {
            scrolling = true
            preScrollPosY = oldt
            postDelayed(scrollTask, 100)
        }

        if (scrollY <= 0) {
            if (currentSpineIndex > 0) {
                currentSpineIndex--
            }
            pageChangeListener.onPrevSpine(currentSpineIndex)
        }

        if (scrollY + measuredHeight >= contentHeight * scale) {
            if (currentSpineIndex < context.spines.size - 1) {
                currentSpineIndex++
            }
            pageChangeListener.onNextSpine(currentSpineIndex)
        }
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

    fun scrollToPageOffset(pageOffset: Int) {
        val padding = 20f / scale
        val offset = Math.max(
                ((pageOffset - 1) * (height / scale) ) - padding, padding)
        injectJs("scrollAbsY($offset)")
    }

    fun scrollYPosition(y: Int) {
        val padding = 20f
        val positionY = Math.max(y - padding, padding).toInt()
        injectJs("scrollAbsY($positionY)")
    }

    fun computeVerticalScrollHeight(): Int = super.computeVerticalScrollRange()

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