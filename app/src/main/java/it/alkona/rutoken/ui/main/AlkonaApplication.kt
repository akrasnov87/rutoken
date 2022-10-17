/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.main

import android.app.Application
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import it.alkona.rutoken.koin.koinModule
import it.alkona.rutoken.tokenmanager.TokenManager
import it.alkona.rutoken.Constants
import java.io.File


class AlkonaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AlkonaApplication)
            modules(koinModule)
        }
        get<TokenManager>()

        val filePath = File(cacheDir, Constants.LOGCAT)
        Runtime.getRuntime().exec(arrayOf("logcat", "-f", filePath.path, "*:V"))
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        Firebase.crashlytics.setCustomKey("url", Constants.URL)
    }
}