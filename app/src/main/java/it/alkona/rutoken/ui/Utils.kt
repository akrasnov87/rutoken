package it.alkona.rutoken.ui

import android.content.Context
import android.net.Uri
import android.text.Layout.Alignment.ALIGN_NORMAL
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.AlignmentSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.buildSpannedString
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import it.alkona.rutoken.Constants
import it.alkona.rutoken.R

fun Context.buildWaitingTokenString() = buildSpannedString {
    append(getText(R.string.connect_token), RelativeSizeSpan(1.3f), SPAN_INCLUSIVE_EXCLUSIVE)
    append(getText(R.string.waiting_token), AlignmentSpan.Standard(ALIGN_NORMAL), SPAN_INCLUSIVE_EXCLUSIVE)
}

/**
 * Opens URL in a browser using Custom Tabs API
 */
fun Context.launchCustomTabsUrl(url: Uri) {
    CustomTabsIntent.Builder().build().launchUrl(this, url)
}

fun logger(message: String) {
    Firebase.crashlytics.log(message)
    Log.d(Constants.TAG, message)
}

fun action(message: String) {
    logger("action: $message")
}

fun window(message: String) {
    logger("window: $message")
}

fun warning(message: String) {
    logger("warning: $message")
}

fun error(throwable: Throwable, message: String?) {
    Firebase.crashlytics.recordException(throwable)

    if(message != null) {
        logger(message)
    }
}