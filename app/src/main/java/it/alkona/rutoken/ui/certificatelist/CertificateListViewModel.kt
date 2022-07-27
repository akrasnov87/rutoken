/*
 * Copyright (c) 2020, Aktiv-Soft JSC. See https://download.rutoken.ru/License_Agreement.pdf.
 * All Rights Reserved.
 */

package it.alkona.rutoken.ui.certificatelist

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import it.alkona.rutoken.R
import it.alkona.rutoken.database.UserEntity
import it.alkona.rutoken.pkcs11.GostObjectFinder
import it.alkona.rutoken.pkcs11.getSerialNumber
import it.alkona.rutoken.repository.User
import it.alkona.rutoken.repository.UserRepository
import it.alkona.rutoken.repository.makeUser
import it.alkona.rutoken.tokenmanager.TokenManager
import it.alkona.rutoken.ui.buildWaitingTokenString
import it.alkona.rutoken.ui.workprogress.WorkProgressView.Status
import it.alkona.rutoken.utils.BusinessRuleCase.CERTIFICATE_NOT_FOUND
import it.alkona.rutoken.utils.BusinessRuleCase.USER_DUPLICATES
import it.alkona.rutoken.utils.BusinessRuleException
import ru.rutoken.pkcs11wrapper.constant.standard.Pkcs11UserType
import ru.rutoken.pkcs11wrapper.main.Pkcs11Token
import java.util.concurrent.ExecutionException

typealias Certificate = User

class CertificateListViewModel(
    private val context: Context,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val tokenPin: String
) : ViewModel() {
    private val _status = MutableLiveData(
        Status(
            context.buildWaitingTokenString(),
            false,
            context.getDrawable(R.drawable.pic_connect)
        )
    )
    val status: LiveData<Status> = _status

    private val _pkcs11Result = MutableLiveData<Result<List<Certificate>>>()
    val pkcs11Result: LiveData<Result<List<Certificate>>> = _pkcs11Result

    private val _addUserResult = MutableLiveData<Result<Unit>>()
    val addUserResult: LiveData<Result<Unit>> = _addUserResult

    init {
        createCertificateList(tokenPin)
    }

    fun getTokenPin(): String {
        return tokenPin
    }

    private fun createCertificateList(tokenPin: String) = viewModelScope.launch {
        try {
            val token = tokenManager.getSingleTokenAsync().await()
            _status.value = Status(context.getString(R.string.processing), true)

            val serialNumber = withContext(Dispatchers.IO) {
                return@withContext token.getSerialNumber()
            }
            requireUniqueTokenSerial(serialNumber)

            val gostContainers = findContainers(token, tokenPin)
            _pkcs11Result.value = Result.success(gostContainers.map {
                makeUser(
                    UserEntity(
                        certificateDerValue = it.certificate.encoded,
                        ckaId = it.ckaId,
                        tokenSerialNumber = serialNumber,
                        pin = null
                    )
                )
            })

            _status.value = Status(context.getString(R.string.done), false)
        } catch (e: Exception) {
            val exception = if (e is ExecutionException) (e.cause ?: e) else e

            _status.value = Status(null, false)
            _pkcs11Result.value = Result.failure(exception)
        }
    }

    private suspend fun findContainers(token: Pkcs11Token, pin: String) =
        withContext(Dispatchers.IO) {
            token.openSession(false).use { session ->
                session.login(Pkcs11UserType.CKU_USER, pin).use {
                    val gostContainers = GostObjectFinder.findContainers(session)

                    if (gostContainers.isEmpty())
                        throw BusinessRuleException(CERTIFICATE_NOT_FOUND)

                    return@withContext gostContainers
                }
            }
        }

    private suspend fun requireUniqueTokenSerial(tokenSerial: String) {
        for (user in userRepository.getUsers()) {
            if (tokenSerial == user.userEntity.tokenSerialNumber)
                throw BusinessRuleException(USER_DUPLICATES)
        }
    }

    fun addUser(certificate: Certificate) = viewModelScope.launch {
        try {
            userRepository.addUser(certificate)
            _addUserResult.value = Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            _addUserResult.value = Result.failure(BusinessRuleException(USER_DUPLICATES))
        }
    }
}