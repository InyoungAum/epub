package com.ridi.inyoung.epub.activity

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import com.ridi.books.helper.Log
import com.ridi.books.helper.view.findLazy
import com.ridi.inyoung.epub.EPubApplication
import com.ridi.inyoung.epub.R
import com.ridi.inyoung.epub.util.EpubParser
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.*

class MainActivity: Activity() {
    companion object {
        val TAG = "MainActivity"
        val defaultBookFile = File(EPubApplication.instance.filesDir, "EPub")
    }

    val webView by findLazy<WebView>(R.id.webView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        copyFile(resources.openRawResource(R.raw.book1))?.let {
            unzip(it, defaultBookFile.absolutePath)
        }

        doParseContainer()
    }

    private fun doParseContainer() {
        val context = EpubParser.parseSpine(defaultBookFile)
        val curSpine = context.spines[10]

        try {
            val html = curSpine.getHtml()
            webView.loadDataWithBaseURL(curSpine.baseUrl, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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