/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.userlist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import it.alkona.rutoken.Constants
import org.koin.androidx.viewmodel.ext.android.viewModel
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.FragmentUserListBinding
import it.alkona.rutoken.repository.User
import it.alkona.rutoken.ui.launchCustomTabsUrl
import it.alkona.rutoken.ui.pin.PinDialogFragment
import it.alkona.rutoken.ui.pin.PinDialogFragment.Companion.DIALOG_RESULT_KEY
import it.alkona.rutoken.ui.pin.PinDialogFragment.Companion.PIN_KEY
import it.alkona.rutoken.ui.userlist.UserListFragmentDirections.toCertificateListFragment


const val PRIVACY_POLICY_URL = "https://www.rutoken.ru/company/policy/demosmena-android.html"

class UserListFragment : UserSelectListeners, Fragment() {
    private lateinit var binding: FragmentUserListBinding
    private val viewModel: UserListViewModel by viewModel()
    private lateinit var userListAdapter: UserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userListAdapter = UserListAdapter(requireContext(), this)
        childFragmentManager.setFragmentResultListener(DIALOG_RESULT_KEY, this) { _, bundle ->
            val pin = bundle.getString(PIN_KEY)
            findNavController().navigate(toCertificateListFragment(pin!!))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(Constants.TAG, "Интерфейс выбора пользователя")
        binding = FragmentUserListBinding.inflate(inflater)
        binding.addUserButton.setOnClickListener {
            PinDialogFragment().show(childFragmentManager, null)
        }

        binding.usersRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = userListAdapter
            ItemTouchHelper(ItemTouchHelperCallback()).attachToRecyclerView(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        viewModel.getUsersAsync().observe(viewLifecycleOwner) {
            userListAdapter.setUsers(it)

            binding.emptyUserListTextView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.usersRecyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupToolbar() {
        binding.toolbarLayout.toolbar.apply {
            inflateMenu(R.menu.menu_main)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.toPrivacyPolicy -> {
                        context.launchCustomTabsUrl(Uri.parse(PRIVACY_POLICY_URL))
                        true
                    }
                    else -> false
                }
            }
        }
    }

    inner class ItemTouchHelperCallback :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val user = userListAdapter.getUser(position)

            viewModel.removeUser(user)

            Snackbar.make(binding.userListLayout, R.string.user_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    viewModel.addUser(user)
                }
                .setBackgroundTint(
                    ContextCompat.getColor(binding.userListLayout.context, R.color.rutokenBlack)
                )
                .setActionTextColor(
                    ContextCompat.getColor(binding.userListLayout.context, R.color.rutokenLightBlue)
                )
                .show()
        }
    }

    override fun onUserSelect(user: User) {
        Log.d(Constants.TAG, "Пользователь ${user.fullName} выбран по умолчанию")
        val users = userListAdapter.getUsers()
        for (u in users) {
            if(u.userEntity.id != user.userEntity.id) {
                u.userEntity.userDefault = false
            }
            viewModel.updateUser(u)
        }
        findNavController().popBackStack(R.id.webFragment, true);
    }
}