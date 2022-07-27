/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package it.alkona.rutoken.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import it.alkona.rutoken.R
import it.alkona.rutoken.Constants
import java.io.File
import java.net.URLConnection

fun shareFileAndLogcat(context: Context): Intent {
    val signatureFile = File(context.cacheDir, Constants.LOGCAT)

    val signatureUri =
        FileProvider.getUriForFile(context, "it.alkona.rutoken.fileprovider", signatureFile)

    val intentShareFile = Intent(Intent.ACTION_SEND)

    intentShareFile.type = URLConnection.guessContentTypeFromName(signatureFile.getName())
    intentShareFile.putExtra(
        Intent.EXTRA_STREAM,
        signatureUri
    )

    //if you need
    //intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Sharing File Subject);
    //intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File Description");

    return Intent.createChooser(intentShareFile, context.getString(R.string.share_logcat))
}