package com.ridi.inyoung.epub.view

import android.content.Context
import android.os.Build
import android.support.annotation.Dimension
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import com.ridi.books.helper.Log
import com.ridi.books.helper.annotation.Dp
import com.ridi.inyoung.epub.util.EpubParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by inyoung on 2017. 10. 16..
 */

class EpubWebView : WebView {
    interface ScrollChangeListener {
        fun onPrevSpine(spineIndex: Int)
        fun onNextSpine(spineIndex: Int)
        fun onScrollChanged(y: Int, currentSpineIndex: Int)
    }


    private val CHROME_51 = 270400000

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var context: EpubParser.Context
    lateinit var scrollChangeListener: ScrollChangeListener
    lateinit var renderingContext: RenderingContext

    var forPagination = false
    var currentAnchor: String? = null
     private set

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

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        renderingContext = RenderingContext(getContext(), w, h, forPagination)

    }

    fun loadSpine(index: Int = 0, anchor: String? = null) {
        preScrollPosY = 0
        currentSpineIndex = index
        currentAnchor = anchor
        scrolling = false
        dragging = false
        spineLoaded = true

        alpha = 0f
        ViewCompat.animate(this)
                .setDuration(500 * 2)
                .alpha(1f)

        val curSpine = context.spines[index]
        try {
            val html = normalizeHtml(curSpine.getHtml()!!)
            loadDataWithBaseURL(curSpine.baseUrl, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAnchor() {
        currentAnchor?.let {
            injectJs("scrollToAnchor('$it')")
            currentAnchor = null
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
                scrollChangeListener.onPrevSpine(currentSpineIndex)
            }
        }

        if (scrollY + measuredHeight >= contentHeight * scale) {
            if (currentSpineIndex < context.spines.size - 1) {
                currentSpineIndex++
                scrollChangeListener.onNextSpine(currentSpineIndex)
            }
        }

        scrollChangeListener.onScrollChanged(t + 1, currentSpineIndex)
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
        injectJs("scrollByOffset($pageOffset)")
    }

    fun scrollYPosition(y: Int) {
        val padding = getBodyPaddingTop(renderingContext)
        val positionY = Math.max(y - padding, padding)
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

    private fun getNormalizedStyle(): String {
        val context = RenderingContext(getContext(), width, height, forPagination)

        // html style
        val htmlStyle = String.format(Locale.US,
                "padding: 0 !important; margin: 0 !important;" + " width: %dpx !important;",
                context.columnWidth)

        // body style
        var bodyStyle = String.format(Locale.US,
                "height: auto !important; padding: %dpx 0 %dpx 0 !important;" + " margin: 0 0 %dpx 0 !important; background-color: %s; color: %s;",
                getBodyPaddingTop(context), context.bodyPaddingBottom,
                context.bodyMarginBottom, context.bgColor, context.fgColor)
        val minHeight = getBodyPaddingTop(context) + context.height + context.bodyPaddingBottom
        bodyStyle += String.format(Locale.US, " min-height: %dpx !important;", minHeight)


        val globalStyle = StringBuilder()
        globalStyle.append(" word-break: break-word; -webkit-tap-highlight-color: transparent;")

        // android only
        val replacement = StringBuilder()
        replacement
                .append(" * { ").append(globalStyle).append(" }")
                .append(" html { ").append(htmlStyle).append(" }")
                .append(" body { ").append(bodyStyle).append(" }")
                .append(" p { font-size: 1em; text-align: justify; }")
                .append(" img, video, svg { max-width: 100%; max-height: 95%; margin: 0 auto; padding: 0; }")
                .append(" pre { white-space: pre-wrap; }")
                .append(" a:-webkit-any-link { text-decoration: none; }")
                .append(" { line-height: initial !important; }")
                .append(" aside { display: none; }")

        return replacement.toString()
    }

    private fun normalizeHtml(html: String): String {
        var html = html
        val replacement = "<meta name='format-detection' content='telephone=no' />" +
                "<meta name='format-detection' content='address=no' />" +
                "<style>" +
                getNormalizedStyle() +
                "</style></head>"
        html = html.replace("(?i)</head>".toRegex(), replacement)

        // video 태그 일단 미지원
        html = html.replace("(?i)<video".toRegex(), "<video-not-supported")

        return html
    }

    private fun getBodyPaddingTop(context : RenderingContext): Int
            = if (currentSpineIndex == 0) context.bodyPaddingTopWhenFirstSpine else context.bodyPaddingTop


    class RenderingContext constructor(context: Context, canvasWidth: Int, canvasHeight: Int, forPagination: Boolean){
        val columnWidth: Int
        val height: Int
        val columnGap: Int
        val bodyMarginBottom: Int

        val bodyPaddingTop: Int
        val bodyPaddingTopWhenFirstSpine: Int
        val bodyPaddingBottom: Int

        val bgColor: String
        val fgColor: String
        init {
            columnWidth = Math.round(pixelToDip(context, canvasWidth.toFloat()))
            height = Math.round(pixelToDip(context, canvasHeight.toFloat()))
            columnGap = if (forPagination) 0 else 10
            bodyMarginBottom = 0
            bodyPaddingTop = if (forPagination) 0 else (height * (0.5f / 2)).toInt()
            bodyPaddingTopWhenFirstSpine = if (forPagination) 0 else 30
            bodyPaddingBottom = if (forPagination) 0 else (height * 0.5f).toInt()
            bgColor = "#ffffff"
            fgColor = "black"
        }
        @Dp
        private fun pixelToDip(context: Context, @Dimension pixel: Float): Float =
                pixel / context.resources.displayMetrics.density

    }
}