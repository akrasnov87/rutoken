/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package it.alkona.rutoken.repository

import androidx.lifecycle.LiveData
import it.alkona.rutoken.database.UserEntity

interface UserRepository {
    suspend fun getUser(userId: Int): User
    suspend fun getUsers(): List<User>

    fun getUsersAsync(): LiveData<List<User>>
    fun getUserAsync(userId: Int): LiveData<User>
    suspend fun addUser(user: User)
    suspend fun removeUser(user: User)
    suspend fun updateUser(user: User)
}
