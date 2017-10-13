package com.ridi.inyoung.epub.activity

import android.app.Activity
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import android.webkit.WebView
import com.ridi.books.helper.view.findLazy
import com.ridi.inyoung.epub.EPubApplication
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import com.ridi.inyoung.epub.R
import java.io.FileInputStream
import java.util.zip.ZipInputStream


class MainActivity : Activity() {

    companion object {
        val TAG : String = "MainActivity"
        val defaultBookFile = File(EPubApplication.instance.filesDir, "EPub")
    }

    val webView by findLazy<WebView>(R.id.webView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        unpack()
    }

    private fun unpack() {
        val zipStream = ZipInputStream(resources.openRawResource(R.raw.book1))
        try {
            defaultBookFile.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        while (zipStream.available() == 1) {
            val entry = zipStream.nextEntry as ZipEntry
            Log.d(TAG, "entry: " + entry)
            Log.d(TAG, "isdir: " + entry.isDirectory)

            if (entry.isDirectory) {
                val file = File(defaultBookFile, entry.name)
                file.mkdirs()
                Log.d(TAG, "Create dir " + entry.name)
            } else {
                val f = File(defaultBookFile, entry.name)
                if (f.exists().not()) {
                    f.createNewFile()
                }
                val fis = FileInputStream(f)
                fis.use {
                    f.outputStream().buffered(1024).use { out ->
                        fis.copyTo(out)
                    }
                }
                Log.d(TAG, "file" + entry.getName())
            }
        }
        Log.d(TAG, "done")
    }
}
