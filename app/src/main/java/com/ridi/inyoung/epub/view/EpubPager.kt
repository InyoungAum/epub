package com.ridi.inyoung.epub.view

import android.os.Handler
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ridi.inyoung.epub.model.EpubSpine

class EpubPager constructor(val pagingListener: PagingListener, val epubWebView: EpubWebView) {
    interface PagingListener {
        fun onProgressPaging(spine: EpubSpine)
        fun onCompletePaging()
    }

    val pageIndexes: MutableList<Int> = ArrayList()
    private val navPointIndexes: MutableList<Float> = ArrayList()
    private val jsInterface = DisposableJSInterface()
    private var spines: List<EpubSpine>

    init {
        epubWebView.addJavascriptInterface(jsInterface, "android")
        spines = epubWebView.context.spines
        epubWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                epubWebView.loadJsModule()
                epubWebView.injectJs("setTimeout( function() {" +
                        "android.calcPageCount(calcPageCount());" +
                        "}, 50);")
            }
        }
    }

    fun startPaging() {
        loadSpine()
    }

    private fun loadSpine(index: Int = 0) {
        epubWebView.loadSpine(index)
    }

    private fun pageCalculated(count: Int) {
        val spine = spines[pageIndexes.size]

        var prevCount = 0
        if (spine.index > 0) {
            prevCount = pageIndexes[spine.index - 1]
        }
        pageIndexes.add(prevCount + count)

        pagingListener.onProgressPaging(spine)

        if (pageIndexes.size < spines.size) {
            loadSpine(pageIndexes.size)
        } else {
            jsInterface.dispose()

            epubWebView.removeJavascriptInterface("android")
            epubWebView.stopLoading()
            pagingListener.onCompletePaging()
        }
    }

    inner class DisposableJSInterface {
        val handler = Handler()
        var dispose = false

        fun dispose() {
            dispose = true
        }

        @JavascriptInterface
        fun calcPageCount(count: Int) {
            if (count == -1) {
                handler.postDelayed ({
                    if (dispose) {
                        return@postDelayed
                    }
                    epubWebView.injectJs("android.calcPageCount(calcPageCount())")
                }, 300)
            } else {
                handler.post {
                    pageCalculated(count)
                }
            }
        }
    }
}