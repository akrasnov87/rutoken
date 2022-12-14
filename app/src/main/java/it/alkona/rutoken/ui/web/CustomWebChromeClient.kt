package it.alkona.rutoken.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
import it.alkona.rutoken.R
import it.alkona.rutoken.ui.logger
import it.alkona.rutoken.ui.main.MainActivity


/**
 * Собственный объект для работы с интерфейсом Chrome
 */
class CustomWebChromeClient(
    private val context: Context,
    var activity: MainActivity
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


    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        logger("Выбор файла из локального хранилища устройства")

        activity.filePath = filePathCallback

        val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentIntent.type = "*/*"
        contentIntent.addCategory(Intent.CATEGORY_OPENABLE)
        activity.getFile.launch(contentIntent)
        return true
    }
}