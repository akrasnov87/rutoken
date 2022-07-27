package ru.rutoken.demoshift.ui.userlist

import ru.rutoken.demoshift.repository.User

interface UserSelectListeners {
    fun onUserSelect(user: User)
}