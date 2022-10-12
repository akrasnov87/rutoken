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
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import it.alkona.rutoken.R
import it.alkona.rutoken.databinding.FragmentWebBinding
import it.alkona.rutoken.Constants
import it.alkona.rutoken.utils.asReadableText
import it.alkona.rutoken.utils.shareFileAndLogcat
import org.bouncycastle.cert.X509CertificateHolder
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.text.DateFormat
import java.util.*

/**
 * Фрагмент для вывода WebView
 */
class WebFragment : Fragment() {
    lateinit var binding: FragmentWebBinding
    lateinit var viewModel: WebViewModel
    var rutokenWait: RutokenWaitFragment? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWebBinding.inflate(layoutInflater)
        viewModel = getViewModel()

        // асинхронное получение результат попфтки подписания
        viewModel.result.observe(viewLifecycleOwner) { result ->
            Log.d(Constants.TAG, "Результат подписания документа ${viewModel.docId}: ${result.isSuccess}")

            if (result.isSuccess) {
                if (viewModel.docId == "unknown") {
                    webClientTokenSignature(result.getOrThrow())
                } else {
                    webClientDocumentSignature(viewModel.docId, result.getOrThrow())
                }
            } else {
                hideWaitFragment()
                Snackbar.make(binding.root, R.string.connection_lost, Snackbar.LENGTH_SHORT).show()

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

    private fun showWaitFragment() {
        rutokenWait = RutokenWaitFragment()
        rutokenWait!!.show(requireActivity().supportFragmentManager, "wait")
        Log.d(Constants.TAG, "Выводим окно с ожиданием")
    }

    private fun hideWaitFragment() {
        if (rutokenWait != null) {
            Log.d(Constants.TAG, "Убираем окно ожидания автоматически")
            rutokenWait!!.dismiss()
            rutokenWait = null
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
        Log.d(Constants.TAG, "Вызов клиентской функции: rutokenStatus")

        requireActivity().runOnUiThread {
            binding.webView.evaluateJavascript(
                "rutokenStatus('${docId}', '${message}', ${isError})",
                null
            )
        }
    }

    private fun webClientDocumentSignature(docId: String, signature: String) {
        val file = File(requireActivity().cacheDir, "document.cert")
        file.writeText(clearSignature(signature))

        Log.d(Constants.TAG, "Вызов клиентской функции: rutokenSignatureResult")

        requireActivity().runOnUiThread {
            //val byte = clearSignature(signature).toByteArray(charset("UTF-8"))
            val base64 = clearSignature(signature) //Base64.getEncoder().encodeToString(byte)
            binding.webView.evaluateJavascript(
                "rutokenSignatureResult('${docId}', '${base64}')",
                null
            )
        }
    }

    private fun webClientTokenSignature(signature: String) {
        Log.d(Constants.TAG, "Вызов клиентской функции: rutokenTokenSignature")

        requireActivity().runOnUiThread {
            //val byte = clearSignature(signature).toByteArray(charset("UTF-8"))
            val base64 = clearSignature(signature) //Base64.getEncoder().encodeToString(byte)
            binding.webView.evaluateJavascript(
                "rutokenTokenSignature('${base64}')",
                null
            )
        }
    }

    private fun clearSignature(signature: String): String {
        val lines = signature.split("\n")
        var str = ""
        for (s in lines) {
            if(s.startsWith("-----")) {
                continue
            }

            if(s == "") {
                continue
            }

            str += s
        }
        return str
    }

    // JS function

    @JavascriptInterface
    fun documentSelect(docId: String, url: String) {
        requireActivity().runOnUiThread {
            showWaitFragment()
        }
        Log.d(Constants.TAG, "Вызов android функции: rutokenDocumentSelect(${docId}, ${url})")

        webClientDocumentStatus(docId, getString(R.string.download), false)

        Thread {
            try {
                viewModel.downloadFile(requireActivity(), URL(Constants.URL + url))
                //viewModel.downloadFile(requireActivity(), URL("https://delo.cap.ru/files/documents/2022/9/21/eeb3513a-a6e9-495b-8cc6-f0cb036121fd"))

                requireActivity().runOnUiThread {
                    webClientDocumentStatus(docId, getString(R.string.signature), false)

                    viewModel.sign(docId, isAttached = false)
                }
            } catch (e: MalformedURLException) {
                Log.d(Constants.TAG, "Ошибка вызов android функции: rutokenDocumentSelect(${docId}, ${url})")
            }
        }.start()
    }

    /**
     * Получение информации о сертификате
     */
    @JavascriptInterface
    fun getCertificate(): String {
        Log.d(Constants.TAG, "Вызов android функции: rutokenGetCertificate")

        val user = viewModel.user

        val str = if(user != null) {
            val json: Gson =
                GsonBuilder().serializeNulls().create()
            json.toJson(user.userEntity.certificateDerValue)
        } else {
            "[]"
        }
        Log.d(Constants.TAG, "Данные о сертификате: " + str)
        return str
    }

    /**
     * Указание информации, что подпись по документу получена и кэш можно удалять
     */
    @JavascriptInterface
    fun done(docId: String) {
        requireActivity().runOnUiThread {
            hideWaitFragment()
        }

        Log.d(Constants.TAG, "Вызов android функции: rutokenDone")

        val cert = File(requireActivity().cacheDir, "document.cert")
        if (cert.exists()) {
            if(cert.delete()) {
                Log.d(Constants.TAG, "Подпись документа ${docId} удалена.")
            }
        }

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
        Log.d(Constants.TAG, "Вызов android функции: rutokenEnabled")

        return true
    }

    /**
     * включил вход по сертификату для мобильной версии
     */
    @JavascriptInterface
    fun signToken(token: String) {
        Log.d(Constants.TAG, "Вызов android функции: rutokenSignToken('${token}')")

        Thread {
            try {
                requireActivity().runOnUiThread {
                    webClientDocumentStatus("unknown", getString(R.string.sign_сert), false)

                    viewModel.signText(token)
                }
            } catch (e: MalformedURLException) {
                Log.d(Constants.TAG, "Ошибка вызов android функции: rutokenSignToken(${token})")
            }
        }.start()
    }
}