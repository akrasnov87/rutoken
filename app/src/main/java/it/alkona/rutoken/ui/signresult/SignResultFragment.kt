/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.signresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.FragmentSignResultBinding
import it.alkona.rutoken.ui.signresult.SignResultFragmentDirections.toUserListFragment
import it.alkona.rutoken.utils.shareFileAndSignature

class SignResultFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignResultBinding.inflate(inflater)
        val args: SignResultFragmentArgs by navArgs()

        binding.shareButton.setOnClickListener {
            findNavController().popBackStack(R.id.webFragment, false)
            startActivity(shareFileAndSignature(args.documentUri, args.signature, requireContext()))
        }

        binding.backToUserListButton.setOnClickListener {
            findNavController().navigate(toUserListFragment())
        }

        return binding.root
    }
}