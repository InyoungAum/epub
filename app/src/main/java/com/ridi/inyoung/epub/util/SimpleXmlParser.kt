package com.ridi.inyoung.epub.util

import android.util.Log
import android.util.Xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.HashMap

class SimpleXmlParser internal constructor(private val xmlFile: File?, private val handler: Handler?) {

    init {
        if (xmlFile == null) {
            throw IllegalArgumentException("XML file is null.")
        }
        if (handler == null) {
            throw IllegalArgumentException("Tag handler is null.")
        }
    }

    @Throws(Exception::class)
    internal fun parse() {
        if (handler == null) {
            return
        }

        val fis: FileInputStream
        try {
            fis = FileInputStream(xmlFile)
        } catch (e: FileNotFoundException) {
            throw Exception(e)
        }

        try {
            val xpp = newPullParser(fis)
            parse(xpp)
        } catch (e: XmlPullParserException) {
            throw Exception(e)
        } catch (e: IOException) {
            throw Exception(e)
        } finally {
            try {
                fis.close()
            } catch (e: IOException) {
                val msg = "Failed to close input file: " + xmlFile?.path
                Log.w(javaClass.name, msg)
            }

        }
    }

    @Throws(XmlPullParserException::class)
    private fun newPullParser(fis: FileInputStream): XmlPullParser {
        Xml.newPullParser().let{
            it.setInput(fis, null)
            return it
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parse(xpp: XmlPullParser) {
        val element = Element(xpp)
        var event = xpp.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            handleEvent(element, event)
            event = xpp.next()
        }
    }

    private fun handleEvent(element: Element, event: Int) {
        val callback = getHandlerCallback(element, event)
        if (callback != null) {
            try {
                val method = handler!!.javaClass.getDeclaredMethod(callback, Element::class.java)
                method.invoke(handler, element)
            } catch (e: NoSuchMethodException) {
                val msg = "Cannot find callback: " + callback
                Log.e(javaClass.name, msg)
            } catch (e: InvocationTargetException) {
                val msg = "Cannot invoke callback: " + callback
                Log.e(javaClass.name, msg)
            } catch (e: IllegalAccessException) {
                val msg = "Cannot invoke callback: " + callback
                Log.e(javaClass.name, msg)
            }

        }
    }

    private fun getHandlerCallback(element: Element, event: Int): String? {
        var callback: String? = null
        element.name()?.let {
            if (event == XmlPullParser.START_TAG) {
                callback = handler!!.getStartCallback(it)
            } else if (event == XmlPullParser.END_TAG) {
                callback = handler!!.getEndCallback(it)
            }
        }
        return callback
    }

    internal abstract class Handler {
        private val startCallbacks = HashMap<String, String>()
        private val endCallbacks = HashMap<String, String>()

        fun getStartCallback(elementName: String): String? = startCallbacks[elementName]
        fun getEndCallback(elementName: String): String? = endCallbacks[elementName]
        /*
         * <b>callback</b>은 <b>elementName</b>을 가진 start tag가 나타났을 때 호출됩니다. <br/>
         * <b>callback</b>은 이 class를 상속 받은 클래스에서 구현되어야 하며, {@link java.lang.reflect}를 통해 호출되므로,
         * ProGuard 적용 시 method 이름이 바뀌지 않도록 예외 설정을 해야 합니다. <br/>
         * 하나의 <b>elementName</b>에는 하나의 <b>callback</b> 만을 등록할 수 있습니다.
         */
        fun registerStartCallback(elementName: String, callback: String) {
            startCallbacks.put(elementName, callback)
        }

        /*
         * End tag가 나타났을 때 호출된다는 점 외에는 {@link #registerStartCallback}과 동일합니다.
         */
        fun registerEndCallback(elementName: String, callback: String) {
            endCallbacks.put(elementName, callback)
        }
    }

    internal class Element(private val xpp: XmlPullParser) {

        fun name(): String? = xpp.name

        fun attr(key: String): String = xpp.getAttributeValue(null, key)

        fun text(): String {
            var text = ""
            try {
                text = xpp.nextText()
                /*
                 * For API level lower than 14, advance to END_TAG manually if it is not done.
                 * @see {@link XmlPullParser#nextText()}
                 */
                if (xpp.eventType != XmlPullParser.END_TAG) {
                    xpp.next()
                }
            } catch (e: XmlPullParserException) {
                Log.e(javaClass.name, "Failed to read text from element: " + name())
            } catch (e: IOException) {
                Log.e(javaClass.name, "Failed to read text from element: " + name())
            }

            return text
        }

        fun depth(): Int = xpp.depth
    }

    internal class Exception(xmlFile: File, e: Throwable) : java.lang.Exception("Failed to parse xml: " + xmlFile.path, e)
}
