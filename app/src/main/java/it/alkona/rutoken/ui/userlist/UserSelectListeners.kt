package it.alkona.rutoken.ui.userlist

import it.alkona.rutoken.repository.User

interface UserSelectListeners {
    fun onUserSelect(user: User)
}