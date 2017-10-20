package com.ridi.inyoung.epub.model

import com.ridi.inyoung.epub.activity.MainActivity
import com.ridi.inyoung.epub.util.EpubParser
import java.io.*

class EpubDataSource constructor(val bookName: String) {

    fun parseEpub(): EpubParser.Context {
        File(MainActivity.defaultBookFile, bookName).let {
           return EpubParser.parseReaderData(it)
        }
    }

    fun getMetadata(): EpubParser.Metadata {
        File(MainActivity.defaultBookFile, bookName).let { file ->
            EpubParser.parseMetadata(file).let {
                return it
            }
        }
    }
}
