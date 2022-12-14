/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.certificatelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.FragmentCertificateListBinding
import it.alkona.rutoken.ui.action
import it.alkona.rutoken.ui.certificatelist.CertificateListFragmentDirections.toUserListFragment
import it.alkona.rutoken.ui.logger
import it.alkona.rutoken.ui.warning
import it.alkona.rutoken.ui.window
import it.alkona.rutoken.ui.workprogress.WorkProgressView.Status
import it.alkona.rutoken.utils.asReadableText
import it.alkona.rutoken.utils.showError

class CertificateListFragment : Fragment() {
    private lateinit var binding: FragmentCertificateListBinding
    private lateinit var viewModel: CertificateListViewModel

    private val certificatesListAdapter = CertificateListAdapter(object : CertificateCardListener {
        override fun onClick(certificate: Certificate) {
            certificate.userEntity.pin = viewModel.getTokenPin()
            viewModel.addUser(certificate)
            action("Сертификат добавлен")
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCertificateListBinding.inflate(inflater)
        binding.certificatesRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = certificatesListAdapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        window("Выбор сертификата")

        val args: CertificateListFragmentArgs by navArgs()
        viewModel = getViewModel(parameters = { parametersOf(args.pin) })

        viewModel.status.observe(viewLifecycleOwner) {
            binding.workProgress.setStatus(it)
        }

        viewModel.pkcs11Result.observe(viewLifecycleOwner) { result ->
            binding.certificatesView.visibility = if (result.isSuccess) VISIBLE else GONE
            binding.workProgress.visibility = if (result.isSuccess) GONE else VISIBLE

            if (result.isSuccess) {
                certificatesListAdapter.certificates = result.getOrThrow()
                logger("Прочитано из Рутокен ${certificatesListAdapter.certificates.size} сертификатов")
            }
            else {
                val message = result.getFailureMessage()
                warning("Ошибка чтения списка сертификатов из Рутокен: $message")
                binding.workProgress.setStatus(Status(message))
            }
        }

        viewModel.addUserResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(context, getString(R.string.add_user_ok), Toast.LENGTH_SHORT).show()

                findNavController().navigate(toUserListFragment())
            } else {
                val message = result.getFailureMessage()
                warning("Ошибка добавления пользователя из Рутокен: $message")
                showError(binding.certificatesRecyclerView, message)
            }
        }
    }

    private fun <T> Result<T>.getFailureMessage() =
        exceptionOrNull()?.asReadableText(requireContext())
            ?: "${getString(R.string.error_text)}\n\n" +
            (exceptionOrNull() as Exception).message.orEmpty()
}