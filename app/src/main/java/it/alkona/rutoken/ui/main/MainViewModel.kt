package it.alkona.rutoken.ui.main

import androidx.lifecycle.ViewModel
import it.alkona.rutoken.pkcs11.Pkcs11Launcher
import it.alkona.rutoken.repository.UserRepository

class MainViewModel(launcher: Pkcs11Launcher
) : ViewModel() {
    init {
        launcher.launchPkcs11()
    }
}
