package com.ridi.inyoung.epub.model

import java.io.Serializable

data class EpubNavPoint constructor(val label: String = "", val spineIndex: Int = 0,
                                    val anchor: String? = null, val index: Int = -1,
                                    val level: Int = 0): Serializable {
    private val serialVersionUID = -9204256863979522890L
}
