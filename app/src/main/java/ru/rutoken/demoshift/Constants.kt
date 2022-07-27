package ru.rutoken.demoshift

class Constants {
    companion object {
        @Deprecated("После рефакторинга удалить")
        const val LINK_PARAM = "link"
        const val TAG = "ALKONA"
        // JS объект которой будет создан в webview
        const val JS_OBJECT = "RutokenAndroid"
        // адрес для вывода в webview
        const val URL = "http://cic.it-serv.ru/dl.html"

        const val LOGCAT = "logcat.txt"
    }
}