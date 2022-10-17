package it.alkona.rutoken.ui.web

import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import it.alkona.rutoken.R
import it.alkona.rutoken.Constants
import it.alkona.rutoken.ui.logger

/**
 * Собственный объект для работы с интерфейсом Chrome
 */
class CustomWebChromeClient(
    private val context: Context
): WebChromeClient() {
    /**
     * Вывожу alert в интерфейсе Android
     */
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        logger("alert: $message")
        return super.onJsAlert(view, context.getString(R.string.app_name), message, result)
    }

    /**
     * Переопределяю вывод логов из console.log
     */
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.message()?.let {
            logger("console.log: $it")
        }

        return super.onConsoleMessage(consoleMessage)
    }
}