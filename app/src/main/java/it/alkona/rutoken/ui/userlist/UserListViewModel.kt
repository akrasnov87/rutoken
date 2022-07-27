/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.userlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import it.alkona.rutoken.repository.User
import it.alkona.rutoken.repository.UserRepository

class UserListViewModel(private val repository: UserRepository) : ViewModel() {
    fun getUsersAsync(): LiveData<List<User>> = repository.getUsersAsync()

    fun removeUser(user: User) = viewModelScope.launch { repository.removeUser(user) }

    fun addUser(user: User) = viewModelScope.launch { repository.addUser(user) }

    fun updateUser(user: User) = viewModelScope.launch { repository.updateUser(user) }
}