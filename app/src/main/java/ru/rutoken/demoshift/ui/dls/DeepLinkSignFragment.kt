package ru.rutoken.demoshift.ui.dls

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.koin.androidx.viewmodel.ext.android.getViewModel
import ru.rutoken.demoshift.Constants
import ru.rutoken.demoshift.R
import ru.rutoken.demoshift.databinding.FragmentDeepLinkSignBinding
import ru.rutoken.demoshift.ui.pin.PinDialogFragment
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DeepLinkSignFragment : Fragment() {
    lateinit var binding: FragmentDeepLinkSignBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeepLinkSignBinding.inflate(layoutInflater)
        val args: DeepLinkSignFragmentArgs by navArgs()
        val viewModel = getViewModel<DeepLinkSignViewModel>()

        Log.d(TAG, "Вызывается адрес: " + args.documentUri)

        viewModel.getUsers().observe(this) { users ->

            if(users.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.rutoken_user_not_found), Toast.LENGTH_LONG).show()
                return@observe
            }

            if(viewModel.getDefaultUserId() == null) {
                Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_LONG).show()
            } else {
                var valid = false
                for (user in users) {
                    if(user.userEntity.id == viewModel.getDefaultUserId()) {
                        valid = true
                        break
                    }
                }

                if(!valid) {
                    Toast.makeText(requireContext(), getString(R.string.user_not_found_rutoken), Toast.LENGTH_LONG).show()
                    return@observe
                }
                val uri = Uri.parse(args.documentUri)

                uri?.getQueryParameter(Constants.LINK_PARAM)?.let {
                    val url = URL(it)
                    setStatusMessage(getString(R.string.download_file))

                    Thread {
                        downloadFile(url, File(requireActivity().cacheDir, "document.pdf"))
                        requireActivity().runOnUiThread {
                            setStatusMessage(getString(R.string.download_finish))

                            PinDialogFragment().show(childFragmentManager, null)
                        }
                    }.start()
                } ?: run {
                    setStatusMessage(getString(R.string.url_param_not_found).replace("{0}", Constants.LINK_PARAM))
                }
            }
        }

        childFragmentManager.setFragmentResultListener(PinDialogFragment.DIALOG_RESULT_KEY, this) { _, bundle ->
            setStatusMessage(getString(R.string.sign_document))
            if(viewModel.getDefaultUserId() == null) {
                setStatusMessage(getString(R.string.default_user_not_found))
                return@setFragmentResultListener
            }

            Log.d(TAG, viewModel.documentUri.value!!.toString())

            findNavController().navigate(
                DeepLinkSignFragmentDirections.toSignDefaultUserFragment(
                    checkNotNull(bundle.getString(PinDialogFragment.PIN_KEY)),
                    viewModel.getDefaultUserId()!!,
                    viewModel.documentUri.value!!
                )
            )
        }

        return binding.root
    }

    // Privates

    /**
     * Загрузка файла в локальный каталог /cache
     * @param url адрес
     * @param outputFile место, куда сохраняется файла
     */
    private fun downloadFile(url: URL, outputFile: File) {
        url.openStream().use { inp ->
            BufferedInputStream(inp).use { bis ->
                FileOutputStream(outputFile).use { fos ->
                    val data = ByteArray(1024)
                    var count: Int
                    while (bis.read(data, 0, 1024).also { count = it } != -1) {
                        fos.write(data, 0, count)
                    }
                }
            }
        }
    }

    /**
     * установка статуса обработки
     * @param message текст сообщения
     */
    private fun setStatusMessage(message: String? = null) {
        message?.let {
            binding.dlsLabel.text = message
            binding.dlsProgressBar.visibility = View.VISIBLE
        } ?: run {
            binding.dlsLabel.text = ""
            binding.dlsProgressBar.visibility = View.GONE
        }
    }

    companion object {
        const val TAG: String = "DEEP_LINK"

        fun getInstance(url: String): Intent {
            return Intent(
                "android.intent.action.MAIN",
                Uri.parse("dls://alkona?${Constants.LINK_PARAM}=${url}")
            )
        }
    }
}