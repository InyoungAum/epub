package com.ridi.inyoung.epub.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import com.ridi.books.helper.Log
import com.ridi.books.helper.view.findLazy
import com.ridi.inyoung.epub.BuildConfig
import com.ridi.inyoung.epub.EPubApplication
import com.ridi.inyoung.epub.R
import com.ridi.inyoung.epub.model.EpubNavPoint
import com.ridi.inyoung.epub.model.EpubSpine
import com.ridi.inyoung.epub.util.EpubParser
import com.ridi.inyoung.epub.view.EpubPager
import com.ridi.inyoung.epub.view.EpubWebView
import com.ridi.inyoung.epub.view.NavPointAdapter
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

class MainActivity: Activity(), EpubPager.PagingListener , EpubWebView.PageChangeListener{
    companion object {
        val TAG = "MainActivity"
        val defaultBookFile = File(EPubApplication.instance.filesDir, "EPub")
    }

    private val webView by findLazy<EpubWebView>(R.id.webView)
    private val pagerWebView by findLazy<EpubWebView>(R.id.pagerWebView)
    private val drawerLayout by findLazy<DrawerLayout>(R.id.drawerLayout)
    private val leftDrawer by findLazy<ListView>(R.id.leftDrawer)
    private val loadingLayout by findLazy<RelativeLayout>(R.id.loadingLayout)
    private val loadingText by findLazy<TextView>(R.id.loadingText)

    lateinit var context: EpubParser.Context
    lateinit var epubPager: EpubPager
    var currentOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 디버그 모드에 한해서 디바이스에도 웹 디버거를 연결할 수 있도록(Android 4.4 이상).
            WebView.setWebContentsDebuggingEnabled(true)
        }

        copyFile(resources.openRawResource(R.raw.book1))?.let {
            unzip(it, defaultBookFile.absolutePath)
        }
        parseEpub()
        generatePager(context)

    }

    private fun parseEpub() {
        context = EpubParser.parseReaderData(defaultBookFile)
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

    override fun onProgressPaging(spine: EpubSpine) {
        runOnUiThread {
            val progress = (spine.index + 1).times(100).div(context.spines.size)
            loadingText.text = "로딩중입니다..$progress%"
        }
    }

    override fun onCompletePaging() {
        loadingLayout.visibility = GONE
        epubPager.epubWebView.visibility = GONE
        setEpubWebView(context)
        setLeftDrawer(context.navPoints)
        loadBook()
    }

    private fun setLeftDrawer(navpoints: MutableList<EpubNavPoint>) {
        val navPointAdapter = NavPointAdapter(this, R.layout.navpoint_drawer_menu, navpoints)
        leftDrawer.adapter = navPointAdapter
        leftDrawer.setOnItemClickListener { _, _, position, _ ->
            webView.loadSpine(navpoints[position].spineIndex)
            drawerLayout.closeDrawers()
        }
    }

    private fun loadBook() {
        webView.loadSpine(1)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadJsModule()
                webView.scrollToPageOffset(currentOffset)
            }
        }
    }

    override fun onPrevPage(spineIndex: Int) {
        Log.d("index", "${epubPager.pageIndexes[spineIndex]}  ${epubPager.pageIndexes[spineIndex - 1]}")
        currentOffset = epubPager.pageIndexes[spineIndex] - epubPager.pageIndexes[spineIndex - 1]
        webView.loadSpine(spineIndex)
    }

    override fun onNextPage(spineIndex: Int) {
        currentOffset = 0
        webView.loadSpine(spineIndex)
    }

    private fun unzip(zipFile: File, targetPath: String) {
        val zip = ZipFile(zipFile, "euc-kr")
        val entries = zip.entries
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val destFilePath = File(targetPath, entry.name)
            destFilePath.parentFile.mkdirs()
            if (entry.isDirectory) {
                continue
            }

            val bufferedInputStream = BufferedInputStream(zip.getInputStream(entry))
            bufferedInputStream.use {
                destFilePath.outputStream().buffered(1024).use { out ->
                    bufferedInputStream.copyTo(out)
                }
            }
        }
        Log.d(TAG, "unzip done")
    }

    private fun copyFile(inputStream: InputStream): File? {
        val file = File(EPubApplication.instance.filesDir, "book1.epub")
        if (file.exists()) {
            return null
        }

        inputStream.use {
            file.outputStream().buffered(8192).use { out ->
                inputStream.copyTo(out)
            }
        }
        inputStream.close()
        return file
    }
}