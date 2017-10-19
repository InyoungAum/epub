package com.ridi.inyoung.epub.util

class PageUtil {
    companion object {
        fun currentPageCount(pageIndexes: MutableList<Int>, index: Int): Int
                = pageIndexes[index] - pageIndexes[index - 1]
    }
}