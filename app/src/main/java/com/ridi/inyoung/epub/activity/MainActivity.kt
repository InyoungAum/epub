package com.ridi.inyoung.epub.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.ridi.books.helper.Log
import com.ridi.books.helper.view.findLazy
import com.ridi.inyoung.epub.EPubApplication
import com.ridi.inyoung.epub.R
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

class MainActivity: Activity() {
    companion object {
        val TAG = "EpubReaderActivity"
        val defaultBookFile = File(EPubApplication.instance.filesDir, "EPub")
    }

    private val book1 by findLazy<Button>(R.id.book1)
    private val book2 by findLazy<Button>(R.id.book2)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findRawList()

        book1.setOnClickListener({
            val intent = Intent(this, EpubReaderActivity::class.java)
            intent.putExtra(EpubReaderActivity.KEY_BOOK_NAME, "book1")
            startActivity(intent)
        })

        book2.setOnClickListener({
            val intent = Intent(this, EpubReaderActivity::class.java)
            intent.putExtra(EpubReaderActivity.KEY_BOOK_NAME, "pr")
            startActivity(intent)
        })
    }

    private fun findRawList() {
        val fields = R.raw::class.java.fields

        fields.filter { it.type.name.equals("int") }.forEach { field ->
            copyFile(resources.openRawResource(
                    resources.getIdentifier(field.name, "raw", packageName)),
                    field.name)?.let {
                unzip(it, defaultBookFile.absolutePath + "/${field.name}")
            }
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

    private fun copyFile(inputStream: InputStream, name: String): File? {
        val file = File(EPubApplication.instance.filesDir, "$name.epub")
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