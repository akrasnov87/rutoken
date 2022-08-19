package it.alkona.rutoken.ui.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.FragmentWebBinding
import it.alkona.rutoken.Constants
import it.alkona.rutoken.utils.asReadableText
import it.alkona.rutoken.utils.shareFileAndLogcat
import java.net.URL
import java.util.*

/**
 * Фрагмент для вывода WebView
 */
class WebFragment : Fragment() {
    lateinit var binding: FragmentWebBinding
    lateinit var viewModel: WebViewModel

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWebBinding.inflate(layoutInflater)
        viewModel = getViewModel()

        viewModel.result.observe(viewLifecycleOwner) { result ->
            Log.d(Constants.TAG, "Результат подписания документа ${viewModel.docId}: ${result.isSuccess}")

            if (result.isSuccess) {
                webClientDocumentSignature(viewModel.docId, result.getOrThrow())
            } else {
                val exceptionMessage = (result.exceptionOrNull() as Exception).message.orEmpty()
                Log.d(Constants.TAG, exceptionMessage)

                webClientDocumentStatus(viewModel.docId,
                    result.exceptionOrNull()?.asReadableText(requireContext())
                        ?: ("${getString(R.string.error_text)}\n\n" + exceptionMessage), true)
            }
        }

        viewModel.viewModelScope.launch {
            viewModel.initCurrentUser()
            if(viewModel.user != null) {
                Log.d(
                    Constants.TAG,
                    "Текущий пользователь инициализирован: ${viewModel.user!!.fullName}"
                )
            } else {
                Log.d(
                    Constants.TAG,
                    "Текущий пользователь не найден"
                )
            }
        }

        webViewInit(binding.webView)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        viewModel.viewModelScope.launch {
            if (viewModel.isUserNotLoad()) {
                findNavController().navigate(
                    WebFragmentDirections.toUserListFragment()
                )
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbarLayout.toolbar.apply {
            inflateMenu(R.menu.menu_web)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.refreshWebView -> {
                        Log.d(Constants.TAG, "Выбор пункта меню: Обновление страницы")

                        //documentSelect("1234", "https://delo.cap.ru/files/documents/2022/7/28/782b6d26-4b04-4d2c-9868-6f0eef0732d0")
                        binding.webView.reload()
                        true
                    }
                    R.id.selectUser -> {
                        Log.d(Constants.TAG, "Выбор пункта меню: Выбор пользователя")

                        findNavController().navigate(R.id.userListFragment)
                        true
                    }
                    R.id.logcat -> {
                        requireActivity().startActivity(shareFileAndLogcat(requireContext()))
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun webViewInit(webView: WebView) {
        webView.webChromeClient = CustomWebChromeClient(requireContext())
        webView.webViewClient = SSLTolerentWebViewClient()
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(this, Constants.JS_OBJECT)
        webView.apply {
            settings.javaScriptEnabled = true
        }
        Log.d(Constants.TAG, "Страница для загрузки ${Constants.URL}")
        webView.loadUrl(Constants.URL)
    }

    private fun webClientDocumentStatus(docId: String, message: String, isError: Boolean) {
        Log.d(Constants.TAG, "Вызов клиентской функции: RutokenWeb.documentStatus")

        requireActivity().runOnUiThread {
            binding.webView.evaluateJavascript(
                "RutokenWeb.documentStatus('${docId}', '${message}', ${isError})",
                null
            )
        }
    }

    private fun webClientDocumentSignature(docId: String, signature: String) {
        Log.d(Constants.TAG, "Вызов клиентской функции: RutokenWeb.documentSignature")

        requireActivity().runOnUiThread {
            val byte = signature.toByteArray(charset("UTF-8"))
            val base64 = Base64.getEncoder().encodeToString(byte)
            binding.webView.evaluateJavascript(
                "RutokenWeb.documentSignature('${docId}', '${base64}')",
                null
            )
        }
    }

    // JS function

    @JavascriptInterface
    fun documentSelect(docId: String, url: String) {
        Log.d(Constants.TAG, "Вызов android функции: RutokenAndroid.documentSelect")

        webClientDocumentStatus(docId, getString(R.string.download), false)

        Thread {
            viewModel.downloadFile(requireActivity(), URL(url))

            requireActivity().runOnUiThread {
                webClientDocumentStatus(docId, getString(R.string.signature), false)

                viewModel.sign(docId)
            }
        }.start()
    }

    /**
     * Получение информации по пользователю rutoken
     */
    @JavascriptInterface
    fun getCurrentUser(): String {
        Log.d(Constants.TAG, "Вызов android функции: RutokenAndroid.getCurrentUser")

        val user = viewModel.user
        return if(user != null) {
            val json: Gson =
                GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().create()
            json.toJson(user)
        } else {
            "{}"
        }
    }

    /**
     * Указание информации, что подпись по документу получена и кэш можно удалять
     */
    @JavascriptInterface
    fun documentDone(docId: String) {
        Log.d(Constants.TAG, "Вызов android функции: RutokenAndroid.documentDone")

        val file = viewModel.getDocumentFile(requireActivity())
        if (file.exists()) {
            if(file.delete()) {
                Log.d(Constants.TAG, "Документ ${docId} удалён.")
            }
        }
    }

    /**
     * Проверка доступности мобильного интерфейса
     */
    @JavascriptInterface
    fun enabled(): Boolean {
        Log.d(Constants.TAG, "Вызов android функции: RutokenAndroid.enabled")

        return true
    }
}