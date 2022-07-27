/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package it.alkona.rutoken.koin

import android.net.Uri
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import it.alkona.rutoken.database.Database
import it.alkona.rutoken.database.MIGRATION_1_2
import it.alkona.rutoken.pkcs11.Pkcs11Launcher
import it.alkona.rutoken.pkcs11.RtPkcs11Module
import it.alkona.rutoken.tokenmanager.TokenManager
import it.alkona.rutoken.ui.certificatelist.CertificateListViewModel
import it.alkona.rutoken.repository.UserRepository
import it.alkona.rutoken.repository.UserRepositoryImpl
import it.alkona.rutoken.ui.main.MainViewModel
import it.alkona.rutoken.ui.userlist.UserListViewModel
import it.alkona.rutoken.ui.web.WebViewModel
import ru.rutoken.pkcs11wrapper.main.Pkcs11Module

val koinModule = module {
    single { RtPkcs11Module() } bind Pkcs11Module::class
    single { Pkcs11Launcher(get()) }
    single { TokenManager().also { get<Pkcs11Launcher>().addListener(it) } }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single {
        Room.databaseBuilder(androidContext(), Database::class.java, "alkona_database")
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    viewModel { (tokenPin: String) ->
        CertificateListViewModel(androidContext(), get(), get(), tokenPin)
    }
    viewModel { UserListViewModel(get()) }
    viewModel { WebViewModel(androidContext(), get(), get()) }
    viewModel { MainViewModel(get()) }
}