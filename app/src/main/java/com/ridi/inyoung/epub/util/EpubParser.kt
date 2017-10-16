package com.ridi.inyoung.epub.util

import android.net.Uri
import com.ridi.books.helper.Log
import com.ridi.inyoung.epub.model.EpubNavPoint
import com.ridi.inyoung.epub.model.EpubSpine
import java.io.File
import java.net.URI
import java.util.*


object EpubParser {
    private val CONTAINER_PATH = File.separator + "META-INF" + File.separator + "container.xml"

    @Throws(Exception::class)
    private fun parseBaseData(baseDir: File): Context {
        try {
            EpubParser.Context(baseDir).let {
                parseContainer(it)
                parseManifest(it)
                parsePublisher(it)
                return it
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse EPUB directory: " + baseDir, e)
        }
    }

    @Throws(Exception::class)
    fun parseSpine(baseDir: File): Context {
        parseBaseData(baseDir).let {
            try {
                parseSpine(it)
                return it
            } catch (e: Exception) {
                throw Exception("Failed to parse EPUB directory: " + baseDir, e)
            }
        }
    }

    @Throws(Exception::class)
    fun parseReaderData(baseDir: File): Context {
        parseSpine(baseDir).let {
            try {
                parseNcx(it)
                return it
            } catch (e: Exception) {
                throw Exception("Failed to parse EPUB directory: " + baseDir, e)
            }
        }
    }

    @Throws(Exception::class)
    fun parseMetadata(baseDir: File): Metadata {
        parseBaseData(baseDir).let {
            try {
                return parseMetadata(it)
            } catch (e: Exception) {
                throw Exception("Failed to parse EPUB directory: " + baseDir, e)
            }
        }
    }

    @Throws(SimpleXmlParser.Exception::class)
    private fun parseMetadata(context: Context): Metadata {
        File(context.baseDir, context.opfPath).let { opf ->
            Metadata().let {
                parseXml(opf, MetadataHandler(context, it))
                return it
            }
        }
    }

    @Throws(SimpleXmlParser.Exception::class)
    private fun parseContainer(context: Context) {
        File(context.baseDir, CONTAINER_PATH).let { container ->
            parseXml(container, ContainerHandler(context))
            File(context.baseDir, context.opfPath).let { opf ->
                context.contentsBaseDir = opf.parent
                context.contentsBaseURI = File(context.contentsBaseDir).toURI()
            }
        }
    }

    @Throws(SimpleXmlParser.Exception::class)
    private fun parsePublisher(context: Context) {
        val opf = File(context.baseDir, context.opfPath)
        parseXml(opf, PublisherHandler(context))
    }



    @Throws(SimpleXmlParser.Exception::class)
    private fun parseManifest(context: Context) {
        File(context.baseDir, context.opfPath).let {
            parseXml(it, ManifestHandler(context))
        }
    }

    @Throws(SimpleXmlParser.Exception::class)
    private fun parseSpine(context: Context) {
        File(context.baseDir, context.opfPath).let {
            parseXml(it, SpineHandler(context))
        }
    }

    @Throws(SimpleXmlParser.Exception::class)
    private fun parseNcx(context: Context) {
        File(context.contentsBaseDir, context.ncxPath).let {
            parseXml(it, NcxHandler(context))
        }
    }

    private fun parseXml(file: File, handler: SimpleXmlParser.Handler) {
        SimpleXmlParser(file, handler).parse()
    }

    private class ContainerHandler internal constructor(context: Context): EpubXmlHandler(context) {
        init {
            registerStartCallback("rootfile", "pushOpfPath")
        }

        fun pushOpfPath(element: SimpleXmlParser.Element) {
            if (element.attr("media-type").equals("application/oebps-package+xml")) {
                context.opfPath = element.attr("full-path")
            }
        }
    }

    private class ManifestHandler internal constructor(context: Context): EpubXmlHandler(context) {
        init {
            registerStartCallback("item", "pushManifestItem")
        }

        fun pushManifestItem(element: SimpleXmlParser.Element) {
            val id = element.attr("id")
            val href = element.attr("href")
            context.manifest.put(id, href)
            if (element.attr("media-type").equals("text/css")) {
                context.cssPaths.add(href)
            } else if (href.toLowerCase(Locale.US).endsWith(".ttf").or(
                    href.toLowerCase(Locale.US).endsWith(".otf"))) {
                context.hasFontFile = true
            }
        }
    }

    private class PublisherHandler internal constructor(context: Context) : EpubXmlHandler(context) {
        init {
            registerStartCallback("publisher", "pushPublisher")
        }

        fun pushPublisher(element: SimpleXmlParser.Element) {
            context.publisher = element.text()
        }
    }

    private class MetadataHandler internal constructor(context: Context, val metadata: Metadata): EpubXmlHandler(context) {
        init {
            registerStartCallback("title", "pushTitle")
            registerStartCallback("creator", "pushCreator")
            registerStartCallback("meta", "pushCover")
        }

        fun pushTitle(element: SimpleXmlParser.Element) {
            metadata.title = element.text()
        }

        fun pushCreator(element: SimpleXmlParser.Element) {
            if (element.attr("role").equals("aut")) {
                metadata.creator = element.text()
            }
        }

        fun pushCover(element: SimpleXmlParser.Element) {
            if (element.attr("name").equals("cover")) {
                element.attr("content").let { content ->
                    context.manifest[content]?.let {
                        metadata.coverFile = File(context.baseDir, it)
                    }
                }
            }
        }
    }

    private class SpineHandler internal constructor(context: Context) : EpubXmlHandler(context) {
        private var spineIndex = 0

        init {
            registerStartCallback("spine", "pushTocPath")
            registerStartCallback("itemref", "pushSpineItem")
        }

        fun pushTocPath(element: SimpleXmlParser.Element) {
            element.attr("toc").let {
                context.ncxPath = context.manifest[it]
            }
        }

        fun pushSpineItem(element: SimpleXmlParser.Element) {
            val idref = element.attr("idref")
            var href: String? = context.manifest[idref]
            if (href != null) {
                href = Uri.decode(href)
                val content = File(context.contentsBaseDir, href!!)
                val baseUrl = content.parentFile.toURI().toString()
                EpubSpine(idref, context.contentsBaseDir, href, baseUrl, spineIndex).let { spine ->
                    context.spines.add(spine)

                    context.contentsBaseURI!!.relativize(content.toURI()).path.let { relativizedPath ->
                        context.spineMapForNavPoints.put(relativizedPath , spine)
                        spineIndex++
                    }
                }

            } else {
                err("Failed to refer to manifest item for id: " + idref)
            }
        }
    }

    private class NcxHandler internal constructor(context: Context) : EpubXmlHandler(context) {
        private var index: Int = 0
        private var baseLevel: Int = 0
        private var level: Int = 0
        private var labelText: String? = null
        private var contentSrc: String? = null

        init {
            registerStartCallback("navMap", "pushBaseLevel")
            registerStartCallback("navPoint", "pushNavPointIfNeededAndLevel")
            registerStartCallback("text", "pushLabelText")
            registerStartCallback("content", "pushContentSrc")
            registerEndCallback("navPoint", "pushNavPointIfNeeded")
        }

        fun pushBaseLevel(element: SimpleXmlParser.Element) {
            baseLevel = element.depth()
        }

        fun pushNavPointIfNeededAndLevel(element: SimpleXmlParser.Element) {
            pushNavPointIfNeeded()
            level = element.depth()
        }

        fun pushLabelText(element: SimpleXmlParser.Element) {
            labelText = element.text()
        }

        fun pushContentSrc(element: SimpleXmlParser.Element) {
            contentSrc = Uri.decode(element.attr("src"))
        }

        fun pushNavPointIfNeeded() {
            if (hasNavPointToPush().not()) {
                return
            }

            val anchor: String?
            val anchorIndex = contentSrc!!.indexOf('#')
            if (anchorIndex > 0) {
                anchor = contentSrc!!.substring(anchorIndex + 1)
                contentSrc = contentSrc!!.substring(0, anchorIndex)
            } else {
                anchor = null
            }

            val contentURI = File(context.contentsBaseDir, contentSrc!!).toURI()
            val relativizedPath = context.contentsBaseURI?.relativize(contentURI)?.path
            val spine = context.spineMapForNavPoints[relativizedPath]

            if (spine != null) {
                val spineIndex = spine.index
                level = level - baseLevel - 1
                val navPoint = EpubNavPoint(labelText ?: "", spineIndex, anchor, index, level)
                context.navPoints.add(navPoint)
                spine.addNavPoint(navPoint)
                index++
            } else {
                err("Spine for nav point not found: " + contentSrc!!)
            }
            clearNavPointToPush()
        }

        private fun hasNavPointToPush(): Boolean = level > 0


        private fun clearNavPointToPush() {
            level = 0
        }
    }

    private abstract class EpubXmlHandler internal constructor(protected val context: Context): SimpleXmlParser.Handler() {

        protected fun err(msg: String) {
            Log.e(EpubParser::class.java, msg)
        }

        protected fun err(msg: String, e: Throwable) {
            Log.e(EpubParser::class.java, msg, e)
        }
    }

    data class Context internal constructor(val baseDir: File) {
        var opfPath: String? = null
        var publisher: String? = null
        var ncxPath: String? = null
        var contentsBaseDir: String? = null
        var contentsBaseURI: URI? = null
        var spines: MutableList<EpubSpine> = ArrayList()
        var navPoints: MutableList<EpubNavPoint> = ArrayList()
        var spineMapForNavPoints: MutableMap<String, EpubSpine> = HashMap()
        var manifest: MutableMap<String, String> = HashMap()
        var cssPaths: MutableList<String> = ArrayList()
        var hasFontFile: Boolean = false
    }

    class Metadata {
        lateinit var title: String
        lateinit var creator: String
        lateinit var coverFile: File
    }
}