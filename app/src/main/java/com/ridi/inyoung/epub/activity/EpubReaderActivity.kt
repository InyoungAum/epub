package com.ridi.inyoung.epub.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.widget.DrawerLayout
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.ridi.books.helper.view.findLazy
import com.ridi.inyoung.epub.BuildConfig
import com.ridi.inyoung.epub.R
import com.ridi.inyoung.epub.model.EpubNavPoint
import com.ridi.inyoung.epub.model.EpubSpine
import com.ridi.inyoung.epub.util.EpubParser
import com.ridi.inyoung.epub.util.PageUtil
import com.ridi.inyoung.epub.view.EpubPager
import com.ridi.inyoung.epub.view.EpubWebView
import com.ridi.inyoung.epub.view.NavPointAdapter
import java.io.File

class EpubReaderActivity : Activity(), EpubPager.PagingListener , EpubWebView.PageChangeListener{

    private val webView by findLazy<EpubWebView>(R.id.webView)
    private val pagerWebView by findLazy<EpubWebView>(R.id.pagerWebView)
    private val drawerLayout by findLazy<DrawerLayout>(R.id.drawerLayout)
    private val menuList by findLazy<ListView>(R.id.menuList)
    private val loadingLayout by findLazy<RelativeLayout>(R.id.loadingLayout)
    private val loadingText by findLazy<TextView>(R.id.loadingText)
    private val titleText by findLazy<TextView>(R.id.titleText)
    private val authorText by findLazy<TextView>(R.id.authorText)

    lateinit var context: EpubParser.Context
    lateinit var epubPager: EpubPager
    lateinit var bookName: String
    var currentOffset = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        bookName = intent.getStringExtra("book_name")

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 디버그 모드에 한해서 디바이스에도 웹 디버거를 연결할 수 있도록(Android 4.4 이상).
            WebView.setWebContentsDebuggingEnabled(true)
        }

        findViewById<ImageButton>(R.id.closeButton).setOnClickListener({
            finish()
        })

        parseEpub()
        getMetadata()
        generatePager(context)
    }

    private fun parseEpub() {
        File(MainActivity.defaultBookFile, bookName).let {
            context = EpubParser.parseReaderData(it)
        }
    }

    private fun setEpubWebView(context: EpubParser.Context) {
        webView.context = context
        webView.pageChangeListener = this
    }

    private fun generatePager(context: EpubParser.Context) {
        pagerWebView.context = context
        epubPager = EpubPager(this, pagerWebView)
        epubPager.startPaging()
        loadingLayout.visibility = VISIBLE
    }

    private fun getMetadata() {
        File(MainActivity.defaultBookFile, bookName).let { file ->
            EpubParser.parseMetadata(file).let {
                titleText.text = it.title ?: "title"
                authorText.text = it.creator ?: "author"
            }
        }
    }

    override fun onProgressPaging(spine: EpubSpine) {
        runOnUiThread {
            val progress = (spine.index + 1).times(100).div(context.spines.size)
            loadingText.text = "로딩중입니다..$progress%"
        }
    }

    override fun onCompletePaging() {
        loadingLayout.visibility = GONE
        setEpubWebView(context)
        setDrawerMenu(context.navPoints)
        loadBook()
    }

    private fun setDrawerMenu(navpoints: MutableList<EpubNavPoint>) {
        val navPointAdapter = NavPointAdapter(this, R.layout.navpoint_drawer_menu, navpoints)
        menuList.adapter = navPointAdapter
        menuList.setOnItemClickListener { _, _, position, _ ->
            webView.loadSpine(navpoints[position].spineIndex)
            drawerLayout.closeDrawers()
        }
    }

    private fun loadBook() {
        webView.loadSpine(1)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                spineLoaded(currentOffset)
                Handler().postDelayed ({
                    loadingLayout.visibility = GONE
                }, 300)
            }
        }
    }

    override fun onPrevSpine(spineIndex: Int) {
        currentOffset = PageUtil.currentPageCount(epubPager.pageIndexes, spineIndex)
        loadSpine(spineIndex)
    }

    override fun onNextSpine(spineIndex: Int) {
        currentOffset = 0
        loadSpine(spineIndex)
    }

    private fun loadSpine(index: Int) {
        loadingLayout.visibility = VISIBLE
        webView.loadSpine(index)
    }

    private fun spineLoaded(offset: Int) {
        webView.run {
            loadJsModule()
            if (offset != 0) {
                scrollYPosition(
                        computeVerticalScrollHeight() - height
                )
            } else {
                webView.scrollToPageOffset(offset)
            }
        }
    }

}