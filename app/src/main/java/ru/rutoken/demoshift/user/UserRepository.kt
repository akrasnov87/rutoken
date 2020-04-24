package ru.rutoken.demoshift.user

import androidx.lifecycle.LiveData

interface UserRepository {
    fun getUser(userId: Int): LiveData<User>
    fun getUsers(): LiveData<List<User>>
    fun addUser(user: User)
    fun removeUser(user: User)
}