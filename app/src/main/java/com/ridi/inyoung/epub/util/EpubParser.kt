package com.ridi.inyoung.epub.util

import com.ridi.books.helper.Log
import com.ridi.inyoung.epub.activity.MainActivity
import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.HashMap


object EpubParser {
    private val CONTAINER_PATH = File.separator + "META-INF" + File.separator + "container.xml"

    fun parseContainer(context: Context) : Context{
        val container = File(MainActivity.defaultBookFile, CONTAINER_PATH)
        SimpleXmlParser(container, ContainerHandler(context)).parse()
        return context
    }


    private class ContainerHandler internal constructor(context: Context) : EpubXmlHandler(context) {
        init {
            registerStartCallback("rootfile", "pushOpfPath")
        }

        fun pushOpfPath(element: SimpleXmlParser.Element) {
            if (element.attr("media-type").equals("application/oebps-package+xml")) {
                context.opfPath = element.attr("full-path")
            }
        }
    }

    private abstract class EpubXmlHandler internal constructor(protected val context: Context) : SimpleXmlParser.Handler() {

        protected fun err(msg: String) {
            Log.e(EpubParser::class.java, msg)
        }

        protected fun err(msg: String, e: Throwable) {
            Log.e(EpubParser::class.java, msg, e)
        }
    }

    class Context internal constructor(val baseDir: File) {
        var opfPath: String? = null
        var publisher: String? = null
        var ncxPath: String? = null
        var contentsBaseDir: String? = null
        var contentsBaseURI: URI? = null
        //var spines: MutableList<EpubSpine> = ArrayList<EpubSpine>()
        //var navPoints: MutableList<EpubNavPoint> = ArrayList<EpubNavPoint>()
        //var spineMapForNavPoints: MutableMap<String, EpubSpine> = HashMap<String, EpubSpine>()
        var manifest: MutableMap<String, String> = HashMap()
        var cssPaths: MutableList<String> = ArrayList()
        var hasFontFile: Boolean = false
    }


}