/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package ru.rutoken.demoshift.ui.main

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.rutoken.demoshift.koin.koinModule
import ru.rutoken.demoshift.tokenmanager.TokenManager
import ru.rutoken.demoshift.Constants
import java.io.File


class DemoshiftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DemoshiftApplication)
            modules(koinModule)
        }
        get<TokenManager>()

        val filePath = File(cacheDir, Constants.LOGCAT)
        Runtime.getRuntime().exec(arrayOf("logcat", "-f", filePath.path, "*:E", "${Constants.TAG}:V", "*:S"))
    }
}