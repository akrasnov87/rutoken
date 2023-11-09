/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.installpanel

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.alkona.rutoken.R
import it.alkona.rutoken.ui.action
import it.alkona.rutoken.ui.logger
import it.alkona.rutoken.ui.window

const val PCSC_PACKAGE_NAME = "ru.rutoken"
const val RUSTORE_PACKAGE_NAME = "ru.vk.store"

class InstallPanelDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        window("Предложение установить сервис для Рутокен")

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_no_pcsc)
            .setMessage(R.string.message_no_pcsc)
            .setNeutralButton(R.string.exit) { _, _ -> requireActivity().finish() }
            .setPositiveButton(R.string.install) { _, _ -> installPanel() }
            .create().also { it.setCanceledOnTouchOutside(false) }
    }

    private fun installPanel() = with(requireActivity()) {
        action("Переход на экран для скачивания службы")

        if(isRustoreInstalled(requireActivity())) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://apps.rustore.ru/app/$PCSC_PACKAGE_NAME")
                )
            )
        } else {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PCSC_PACKAGE_NAME"))
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$PCSC_PACKAGE_NAME")
                    )
                )
            }
        }
        finish()
    }
}

fun isRutokenPanelInstalled(activity: FragmentActivity): Boolean {
    val application = activity.packageManager.getInstalledApplications(0).firstOrNull {
        it.packageName == PCSC_PACKAGE_NAME
    }

    logger("Признак установленной службы: ${application != null}")

    return application != null
}

fun isRustoreInstalled(activity: FragmentActivity): Boolean {
    val application = activity.packageManager.getInstalledApplications(0).firstOrNull {
        it.packageName == RUSTORE_PACKAGE_NAME
    }

    logger("Признак установленной службы: ${application != null}")

    return application != null
}