/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.document

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import org.koin.androidx.viewmodel.ext.android.getViewModel
import it.alkona.rutoken.ui.document.DocumentFragmentDirections.toSignFragment
import it.alkona.rutoken.ui.pin.PinDialogFragment
import it.alkona.rutoken.ui.pin.PinDialogFragment.Companion.DIALOG_RESULT_KEY
import it.alkona.rutoken.ui.pin.PinDialogFragment.Companion.PIN_KEY
import it.alkona.rutoken.databinding.FragmentDocumentBinding

class DocumentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDocumentBinding.inflate(inflater)
        val args: DocumentFragmentArgs by navArgs()

        val viewModel = getViewModel<DocumentViewModel>()

        val getContentLauncher = registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let { viewModel.documentUri.value = it }
        }

        viewModel.documentUri.observe(viewLifecycleOwner) { uri ->
            val isSupported = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(requireContext().contentResolver.getType(uri)) == "pdf"

            if (isSupported)
                binding.documentPdfView.fromUri(uri)
                    .scrollHandle(DefaultScrollHandle(requireContext()))
                    .load()

            binding.documentPdfView.visibility = if (isSupported) VISIBLE else GONE
            binding.unsupportedFileSelected.visibility = if (isSupported) GONE else VISIBLE
        }

        binding.signButton.setOnClickListener {
            PinDialogFragment().show(childFragmentManager, null)
        }

        binding.selectButton.setOnClickListener {
            getContentLauncher.launch("*/*")
        }

        childFragmentManager.setFragmentResultListener(DIALOG_RESULT_KEY, this) { _, bundle ->

            findNavController().navigate(
                toSignFragment(
                    checkNotNull(bundle.getString(PIN_KEY)),
                    args.userId,
                    args.documentUri
                )
            )
        }

        return binding.root
    }
}