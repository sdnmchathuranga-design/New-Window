package com.pdfforge.android

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
