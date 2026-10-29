package br.com.fiap.letssign

import android.app.Application
import br.com.fiap.letssign.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class SeloApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}
