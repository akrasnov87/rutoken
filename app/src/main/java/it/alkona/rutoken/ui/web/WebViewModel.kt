package it.alkona.rutoken.ui.web

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.cert.X509CertificateHolder
import it.alkona.rutoken.R
import it.alkona.rutoken.bouncycastle.signature.CmsSigner
import it.alkona.rutoken.bouncycastle.signature.Signature
import it.alkona.rutoken.bouncycastle.signature.makeSignatureByHashOid
import it.alkona.rutoken.pkcs11.GostObjectFinder
import it.alkona.rutoken.pkcs11.getSerialNumber
import it.alkona.rutoken.repository.User
import it.alkona.rutoken.repository.UserRepository
import it.alkona.rutoken.tokenmanager.TokenManager
import it.alkona.rutoken.ui.buildWaitingTokenString
import it.alkona.rutoken.ui.logger
import it.alkona.rutoken.ui.workprogress.WorkProgressView
import it.alkona.rutoken.utils.BusinessRuleCase
import it.alkona.rutoken.utils.BusinessRuleException
import ru.rutoken.pkcs11wrapper.`object`.certificate.Pkcs11X509PublicKeyCertificateObject
import ru.rutoken.pkcs11wrapper.`object`.key.Pkcs11GostPrivateKeyObject
import ru.rutoken.pkcs11wrapper.attribute.Pkcs11ByteArrayAttribute
import ru.rutoken.pkcs11wrapper.constant.standard.Pkcs11AttributeType
import ru.rutoken.pkcs11wrapper.constant.standard.Pkcs11UserType
import ru.rutoken.pkcs11wrapper.main.Pkcs11Session
import ru.rutoken.pkcs11wrapper.main.Pkcs11Token
import java.io.*
import java.net.URL
import java.util.concurrent.ExecutionException

class WebViewModel(private val context: Context,
                   private val tokenManager: TokenManager,
                   private val userRepository: UserRepository
                   ) : ViewModel() {
    private lateinit var documentUri: Uri
    private lateinit var tokenPin: String
    var user: User? = null
    var docId: String = ""
    private val _status = MutableLiveData(
        WorkProgressView.Status(
            context.buildWaitingTokenString(),
            false,
            context.getDrawable(R.drawable.pic_connect)
        )
    )
    val status: LiveData<WorkProgressView.Status> = _status

    private val _result = MutableLiveData<Result<String>>()
    val result: LiveData<Result<String>> = _result

    init {

    }
    /**
     * Загрузка файла в локальный каталог /cache
     * @param activity активити
     * @param url адрес
     */
    fun downloadFile(activity: Activity, url: URL) {
        logger("Загружаем файл $url ...")
        val outputFile = getDocumentFile(activity)

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

        logger("Файл $url загружен...")
    }

    fun getDocumentFile(activity: Activity): File {
        logger("Читаем документ из файловой системы")

        return File(activity.cacheDir, "document.pdf")
    }

    fun sign(id: String, isAttached: Boolean) = viewModelScope.launch {
        logger("Подписываем документ $id, isAttached=$isAttached ...")
        docId = id
        documentUri = Uri.parse("content://it.alkona.rutoken.fileprovider/cache_files/document.pdf")

        try {
            tokenPin = user!!.userEntity.pin.toString()
            logger("Ввод Пин-кода")

            val token = tokenManager.getSingleTokenAsync().await()
            // token.slot.slotInfo.slotDescription Aktiv Rutoken ECP NFC
            val tokenInfo = token.slot.slotInfo.slotDescription
            logger("token info: $tokenInfo")

            _status.value = WorkProgressView.Status(context.getString(R.string.processing), true)

            val signResult = makeSign(user!!, token, isAttached)

            _status.value = WorkProgressView.Status(context.getString(R.string.done), false)
            _result.value = Result.success(signResult)
            logger("Подписание завершено")
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            val exception = if (e is ExecutionException) (e.cause ?: e) else e

            _status.value = WorkProgressView.Status(null, false)
            _result.value = Result.failure(exception)
        }
    }

    fun signText(txt: String) = viewModelScope.launch {
        docId = "unknown"
        logger("Подписываем строки $docId, isAttached=true ...")
        try {
            tokenPin = user!!.userEntity.pin.toString()
            logger("Ввод Пин-кода")

            val token = tokenManager.getSingleTokenAsync().await()

            val tokenInfo = token.slot.slotInfo.slotDescription
            logger("token info: $tokenInfo")
            _status.value = WorkProgressView.Status(context.getString(R.string.processing), true)

            val signResult = makeSignText(user!!, token, txt)

            _status.value = WorkProgressView.Status(context.getString(R.string.done), false)
            _result.value = Result.success(signResult)

            logger("Подписание завершено")
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            val exception = if (e is ExecutionException) (e.cause ?: e) else e

            _status.value = WorkProgressView.Status(null, false)
            _result.value = Result.failure(exception)
        }
    }

    /**
     * Получение информации по пользователю
     */
    suspend fun initCurrentUser() {
        val users = userRepository.getUsers()
        for (u in users) {
            if(u.userEntity.userDefault) {
                user = u

                Firebase.crashlytics.setUserId(u.fullName)
            }
        }
    }

    /**
     * Пользователи не загружены из rutoken
     */
    suspend fun isUserNotLoad(): Boolean {
        val users = userRepository.getUsers()
        return users.isEmpty()
    }

    private fun openDocumentInputStream() = try {
        context.contentResolver.openInputStream(documentUri)
            ?: throw BusinessRuleException(BusinessRuleCase.FILE_UNAVAILABLE)
    } catch (e: FileNotFoundException) {
        throw BusinessRuleException(BusinessRuleCase.FILE_UNAVAILABLE, e)
    }

    private suspend fun makeSign(user: User, token: Pkcs11Token, isAttached: Boolean) = withContext(Dispatchers.IO) {
        if (user.userEntity.tokenSerialNumber != token.getSerialNumber())
            throw BusinessRuleException(BusinessRuleCase.WRONG_RUTOKEN)

        token.openSession(false).use { session ->
            session.login(Pkcs11UserType.CKU_USER, tokenPin).use {
                requireCertificate(session, user.userEntity.certificateDerValue)
                val certificate = X509CertificateHolder(user.userEntity.certificateDerValue)

                val keyPair = try {
                    GostObjectFinder.findKeyPairByCkaId(session, user.userEntity.ckaId)
                } catch (e: IllegalStateException) {
                    throw BusinessRuleException(BusinessRuleCase.KEY_PAIR_NOT_FOUND, e)
                }

                val signature = makeSignatureByHashOid(
                    keyPair.privateKey.getGostR3411ParamsAttributeValue(session).byteArrayValue,
                    session
                )
                openDocumentInputStream().use { documentStream ->
                    return@withContext signCms(
                        documentStream,
                        signature,
                        keyPair.privateKey,
                        certificate,
                        isAttached
                    )
                }
            }
        }
    }

    private suspend fun makeSignText(user: User, token: Pkcs11Token, txt: String) = withContext(Dispatchers.IO) {
        if (user.userEntity.tokenSerialNumber != token.getSerialNumber())
            throw BusinessRuleException(BusinessRuleCase.WRONG_RUTOKEN)

        token.openSession(false).use { session ->
            session.login(Pkcs11UserType.CKU_USER, tokenPin).use {
                requireCertificate(session, user.userEntity.certificateDerValue)
                val certificate = X509CertificateHolder(user.userEntity.certificateDerValue)

                val keyPair = try {
                    GostObjectFinder.findKeyPairByCkaId(session, user.userEntity.ckaId)
                } catch (e: IllegalStateException) {
                    throw BusinessRuleException(BusinessRuleCase.KEY_PAIR_NOT_FOUND, e)
                }

                val signature = makeSignatureByHashOid(
                    keyPair.privateKey.getGostR3411ParamsAttributeValue(session).byteArrayValue,
                    session
                )

                val inputStream: InputStream = txt.byteInputStream()

                inputStream.use { documentStream ->
                    return@withContext signCms(
                        documentStream,
                        signature,
                        keyPair.privateKey,
                        certificate,
                        true
                    )
                }
            }
        }
    }

    private companion object {
        private fun requireCertificate(session: Pkcs11Session, value: ByteArray) {
            val isCertificatePresent = session.objectManager.findObjectsAtOnce(
                Pkcs11X509PublicKeyCertificateObject::class.java,
                listOf(Pkcs11ByteArrayAttribute(Pkcs11AttributeType.CKA_VALUE, value))
            ).size == 1

            if (!isCertificatePresent)
                throw BusinessRuleException(BusinessRuleCase.CERTIFICATE_NOT_FOUND)
        }

        private fun signCms(
            documentStream: InputStream,
            signature: Signature,
            privateKey: Pkcs11GostPrivateKeyObject,
            certificate: X509CertificateHolder,
            isAttached: Boolean
        ) = with(CmsSigner(signature)) {
            initSignature(privateKey, certificate, isAttached).use { stream ->
                documentStream.copyTo(stream)
            }
            finishSignaturePem()
        }
    }
}