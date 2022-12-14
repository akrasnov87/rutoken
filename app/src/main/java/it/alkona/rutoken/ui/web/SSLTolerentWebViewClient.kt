package it.alkona.rutoken.ui.web

import android.net.http.SslError
import android.webkit.*

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import it.alkona.rutoken.ui.logger


class SSLTolerentWebViewClient : WebViewClient() {
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed() // Ignore SSL certificate errors
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        view?.loadUrl("javascript:(function() {" +
                "rutokenEnabled = function() { console.log('Перенаправление на RutokenAndroid.enabled'); return RutokenAndroid.enabled(); }; " +
                "rutokenDocumentSelect = function(docId, url) { console.log('Перенаправление на RutokenAndroid.documentSelect'); RutokenAndroid.documentSelect(docId, url); }; " +
                "rutokenDone = function(docId) { console.log('Перенаправление на RutokenAndroid.done'); RutokenAndroid.done(docId); }; " +
                "rutokenGetCertificate = function() { console.log('Перенаправление на RutokenAndroid.getCertificate'); return RutokenAndroid.getCertificate(); }; " +
                "rutokenSignToken = function(token) { console.log('Перенаправление на RutokenAndroid.signToken'); RutokenAndroid.signToken(token); }; " +
                "})();")

        logger("Внедряем javascript код для перенаправления на объекты window")
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request != null && error != null) {
            logger("${request.url} $error")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)

        if (request != null && errorResponse != null) {
            val inputAsString = errorResponse.data?.bufferedReader().use { it?.readText() ?: "" }
            logger("${request.url} $inputAsString")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request != null) {
            logger("Вывоз ссылки: ${request.url}")
        }
        return super.shouldOverrideUrlLoading(view, request)
    }
}