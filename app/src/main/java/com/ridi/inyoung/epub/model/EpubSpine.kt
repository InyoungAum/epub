package com.ridi.inyoung.epub.model

import java.io.*
import java.lang.ref.SoftReference

data class EpubSpine constructor(val id: String? = "" , val baseDir: String? = "",
                                 val contentsSrc: String? = "", val baseUrl: String? = "",
                                 val index: Int = -1,
                                 val navPoints: MutableList<EpubNavPoint> = ArrayList()): Serializable {

    @Transient private var htmlRef: SoftReference<String>? = null

    fun getHtml(): String? {
        if (htmlRef?.get() == null) {
            File(baseDir, contentsSrc).let {
                val html = readFile(it)
                //TODO: EpubContentModifier
                htmlRef = SoftReference(html)
            }
        }
        return htmlRef?.get()
    }

    fun addNavPoint(navPoint: EpubNavPoint) = navPoints.add(navPoint)

    fun getNavPointCount(): Int = navPoints.size

    fun getNavPoint(index: Int): EpubNavPoint = navPoints[index]

    private fun readFile(contentsFile: File): String {
        val lineSeparator = System.getProperty("line.separator")
        val sb = StringBuilder()
        BufferedReader(FileReader(contentsFile)).use {
            sb.append(it.readLine())
            sb.append(lineSeparator)
        }
        return sb.toString()
    }

}