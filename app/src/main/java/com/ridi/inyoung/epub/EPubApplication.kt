package com.ridi.inyoung.epub

import android.app.Application
import java.io.File

/**
 * Created by inyoung on 2017. 10. 12..
 */

class EPubApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this
    }

    companion object {
        @JvmStatic
        lateinit var instance: EPubApplication
            private set

        @JvmStatic
        val internalStorageRoot: File by lazy { instance.filesDir }
    }
}