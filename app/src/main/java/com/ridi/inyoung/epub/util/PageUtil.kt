package com.ridi.inyoung.epub.util

class PageUtil {
    companion object {
        fun currentPageCount(pageIndexes: MutableList<Int>, index: Int): Int {
            return if (index == 0) {
                pageIndexes[index]
            } else {
                pageIndexes[index] - pageIndexes[index - 1]
            }
        }
    }
}