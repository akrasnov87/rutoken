/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package ru.rutoken.demoshift.repository

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import ru.rutoken.demoshift.database.Database
import ru.rutoken.demoshift.database.UserEntity

@AnyThread
class UserRepositoryImpl(database: Database) : UserRepository {
    private val userDao = database.userDao()

    override suspend fun getUser(userId: Int) = makeUser(userDao.getUser(userId))

    override suspend fun getUsers() = userDao.getUsers().map { makeUser(it) }

    @MainThread
    override fun getUsersAsync() = Transformations.map(userDao.getUsersAsync()) { userEntityList ->
        userEntityList.map { makeUser(it) }
    }

    @MainThread
    override fun getUserAsync(userId: Int) = Transformations.map(userDao.getUserAsync(userId)) { user ->
        makeUser(user)
    }

    override suspend fun addUser(user: User) = userDao.addUser(user.userEntity)

    override suspend fun removeUser(user: User) = userDao.deleteUser(user.userEntity)
    override suspend fun updateUser(user: User)  = userDao.updateUser(user.userEntity)
}
