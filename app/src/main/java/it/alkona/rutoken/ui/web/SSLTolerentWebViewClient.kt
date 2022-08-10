package it.alkona.rutoken.ui.web

import android.net.http.SslError

import android.webkit.SslErrorHandler

import android.webkit.WebView

import android.webkit.WebViewClient


class SSLTolerentWebViewClient : WebViewClient() {
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed() // Ignore SSL certificate errors
    }
}