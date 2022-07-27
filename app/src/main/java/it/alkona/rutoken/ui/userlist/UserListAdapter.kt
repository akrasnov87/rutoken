/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.userlist

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.UserCardBinding
import it.alkona.rutoken.repository.User

class UserListAdapter(
    private val context: Context,
    private var listeners: UserSelectListeners
) :
    RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {
    private var users: SortedList<User> = SortedList(User::class.java, SortedListCallback(this))

    fun setUsers(userList: List<User>) {
        users.replaceAll(userList)
        notifyDataSetChanged()
    }

    fun getUsers(): ArrayList<User> {
        val array = ArrayList<User>()
        for (i in 0 until users.size()) {
            array.add(users.get(i))
        }
        return array
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val view = holder.view
        val user = getUser(position)
        val binding = UserCardBinding.bind(view)

        binding.userFullName.text = user.fullName
        binding.userPosition.text =
            user.position ?: view.context.getString(R.string.field_not_set)
        binding.userOrganization.text = user.organization ?: view.context.getString(
            R.string.field_not_set
        )
        binding.userCertificateExpires.text = user.certificateExpires
        if(user.userEntity.userDefault) {
            binding.userDefault.text = context.getString(R.string.yes)
        } else {
            binding.userDefault.text = context.getString(R.string.no)
        }

        binding.userCardView.setOnClickListener {
            if(!user.userEntity.userDefault) {
                user.userEntity.userDefault = true

                Toast.makeText(
                    context,
                    context.getString(R.string.user_default_selected),
                    Toast.LENGTH_SHORT
                ).show()
            }

            listeners.onUserSelect(user)
        }
    }

    fun getUser(position: Int): User = users[position]

    override fun getItemCount() = users.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val userView =
            UserCardBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
        return UserViewHolder(userView)
    }

    class UserViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}

private class SortedListCallback(adapter: UserListAdapter) :
    SortedListAdapterCallback<User>(adapter) {
    override fun compare(o1: User, o2: User): Int {
        val fullNameCompare = o1.fullName.compareTo(o2.fullName)

        return if (fullNameCompare != 0)
            fullNameCompare
        else
            o1.userEntity.tokenSerialNumber.compareTo(o2.userEntity.tokenSerialNumber)
    }

    override fun areItemsTheSame(item1: User, item2: User) = item1 === item2

    override fun areContentsTheSame(oldItem: User, newItem: User) = compare(oldItem, newItem) == 0
}