/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package it.alkona.rutoken.tokenmanager

import kotlinx.coroutines.*
import it.alkona.rutoken.pkcs11.Pkcs11Launcher
import it.alkona.rutoken.tokenmanager.slotevent.SlotEvent
import it.alkona.rutoken.tokenmanager.slotevent.SlotEventProvider
import it.alkona.rutoken.utils.BusinessRuleCase.TOO_MANY_TOKENS
import it.alkona.rutoken.utils.BusinessRuleException
import ru.rutoken.pkcs11wrapper.main.Pkcs11Module
import ru.rutoken.pkcs11wrapper.main.Pkcs11Token
import java.util.*

class TokenManager : SlotEventProvider.Listener, Pkcs11Launcher.Listener {
    private val tokens = Collections.synchronizedSet<Pkcs11Token>(mutableSetOf())
    private var waitTokenDeferred: CompletableDeferred<Pkcs11Token>? = null

    private lateinit var eventJob: Job

    override fun onPkcs11Initialized(scope: CoroutineScope, pkcs11Module: Pkcs11Module) {
        eventJob = scope.launch {
            val tokenSlots = withContext(Dispatchers.IO) { pkcs11Module.getSlotList(true) }
            if (tokenSlots.size > 1)
                waitTokenDeferred?.completeExceptionally(BusinessRuleException(TOO_MANY_TOKENS))

            tokenSlots.forEach { addToken(it.token) }

            SlotEventProvider(pkcs11Module).also {
                it.addListener(this@TokenManager)
                it.launchEvents(this)
            }
        }
    }

    override fun beforePkcs11Finalize(scope: CoroutineScope, pkcs11Module: Pkcs11Module) {
        eventJob.cancel()
    }

    override fun onSlotEvent(event: SlotEvent) {
        if (event.slotInfo.isTokenPresent)
            addToken(event.slot.token)
        else
            removeToken(event.slot.token)
    }

    fun getSingleTokenAsync(): Deferred<Pkcs11Token> {
        synchronized(tokens) {
            return when (tokens.size) {
                0 -> {
                    waitTokenDeferred = waitTokenDeferred ?: CompletableDeferred()
                    return waitTokenDeferred!!
                }

                1 -> CompletableDeferred(tokens.first())

                else -> CompletableDeferred<Pkcs11Token>().apply {
                    completeExceptionally(BusinessRuleException(TOO_MANY_TOKENS))
                }
            }
        }
    }

    private fun addToken(token: Pkcs11Token) {
        tokens.add(token)
        waitTokenDeferred?.complete(token)
        waitTokenDeferred = null
    }

    private fun removeToken(token: Pkcs11Token) {
        tokens.remove(token)
    }
}
