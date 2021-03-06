package com.ridi.inyoung.epub.activity

import android.animation.Animator
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
import com.ridi.inyoung.epub.model.EpubDataSource
import com.ridi.inyoung.epub.model.EpubNavPoint
import com.ridi.inyoung.epub.model.EpubSpine
import com.ridi.inyoung.epub.util.EpubParser
import com.ridi.inyoung.epub.util.PageUtil
import com.ridi.inyoung.epub.view.EpubPager
import com.ridi.inyoung.epub.view.EpubWebView
import com.ridi.inyoung.epub.view.NavPointAdapter

class EpubReaderActivity : Activity(), EpubPager.PagingListener , EpubWebView.ScrollChangeListener,
        EpubWebView.GestureListener{
    companion object {
        val KEY_BOOK_NAME = "book_name"
    }

    private val webView by findLazy<EpubWebView>(R.id.webView)
    private val pagerWebView by findLazy<EpubWebView>(R.id.pagerWebView)
    private val drawerLayout by findLazy<DrawerLayout>(R.id.drawerLayout)
    private val menuList by findLazy<ListView>(R.id.menuList)
    private val loadingLayout by findLazy<RelativeLayout>(R.id.loadingLayout)
    private val loadingText by findLazy<TextView>(R.id.loadingText)
    private val titleText by findLazy<TextView>(R.id.titleText)
    private val authorText by findLazy<TextView>(R.id.authorText)
    private val pageCountText by findLazy<TextView>(R.id.pageCountText)
    private val naviBar by findLazy<RelativeLayout>(R.id.naviBar)

    lateinit var context: EpubParser.Context
    lateinit var epubPager: EpubPager
    lateinit var epubDataSource: EpubDataSource
    private var currentOffset = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_reader)

        val bookName = intent.getStringExtra(KEY_BOOK_NAME)

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 디버그 모드에 한해서 디바이스에도 웹 디버거를 연결할 수 있도록(Android 4.4 이상).
            WebView.setWebContentsDebuggingEnabled(true)
        }

        findViewById<ImageButton>(R.id.closeButton).setOnClickListener({
            finish()
        })

        loadingLayout.setOnTouchListener({ _, _ -> true })

        epubDataSource = EpubDataSource(bookName)
        setContext()
        setMetaData()
        generatePager(context)
    }

    private fun showUI() {
        naviBar.animate().run {
            alpha(1.0f)
            duration = 500
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {
                    naviBar.visibility = VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {}
            })
        }
        pageCountText.visibility = VISIBLE
    }

    private fun hideUI() {
        naviBar.animate().run {
            alpha(0.0f)
            duration = 500
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    naviBar.visibility = GONE
                }
            })
        }
        pageCountText.visibility = GONE
    }

    private fun setContext() {
        context = epubDataSource.parseEpub()
    }

    private fun setEpubWebView(context: EpubParser.Context) {
        webView.context = context
        webView.forPagination = false
        webView.scrollChangeListener = this
        webView.gestureListener = this
    }

    private fun generatePager(context: EpubParser.Context) {
        pagerWebView.context = context
        epubPager = EpubPager(this, pagerWebView)
        epubPager.startPaging()
        loadingLayout.visibility = VISIBLE
    }

    private fun setMetaData() {
        epubDataSource.getMetadata().let {
            titleText.text = it.title ?: "title"
            authorText.text = it.creator ?: "author"
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
            navpoints[position].run {
                webView.loadSpine(spineIndex, anchor)
            }
            drawerLayout.closeDrawers()
        }
    }

    private fun loadBook() {
        webView.loadSpine()
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                spineLoaded(currentOffset)
                Handler().postDelayed ({
                    loadingLayout.visibility = GONE
                    spineLoaded(currentOffset)
                }, 300)
            }
        }
    }

    override fun onScrollChanged(y: Int, currentSpineIndex: Int) {
        epubPager.run {
            val prevPageCount = if (currentSpineIndex != 0)
                pageIndexes[currentSpineIndex - 1]
            else 0

            getCurrentPagePosition(y).let {
                pageCountText.text =  "${(it + prevPageCount)} / $totalPageCount"
            }
        }
        hideUI()
    }

    override fun onSingleTapUp() {
        if (naviBar.visibility == VISIBLE) {
            hideUI()
        } else {
            showUI()
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
            currentAnchor?.let {
                loadAnchor()
            } ?: scrollToPageOffset(offset)
        }
    }
}