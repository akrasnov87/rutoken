/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.alkona.rutoken.databinding.ActivityMainBinding
import it.alkona.rutoken.pkcs11.Pkcs11Launcher
import it.alkona.rutoken.ui.installpanel.InstallPanelDialogFragment
import it.alkona.rutoken.ui.installpanel.isRutokenPanelInstalled
import it.alkona.rutoken.ui.window
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.getViewModel


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = getViewModel()
        lifecycle.addObserver(get<Pkcs11Launcher>())

        window("Главный экран")
    }

    override fun onResume() {
        super.onResume()

        if (!isRutokenPanelInstalled(this)) {
            InstallPanelDialogFragment().show(supportFragmentManager, null)
            return
        }
    }
}
