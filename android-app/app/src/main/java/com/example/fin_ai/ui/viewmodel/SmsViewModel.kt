package com.example.fin_ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.SmsPhoneRequest
import com.example.fin_ai.data.toUserMessage
import com.example.fin_ai.sms.BlockedSms
import com.example.fin_ai.sms.ReviewSms
import com.example.fin_ai.sms.SmsCapture
import com.example.fin_ai.sms.SmsStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which step of turning on SMS capture is waiting for the user. */
enum class SmsSetupStep { NONE, SWITCH_OWNER, ENTER_PHONE, REQUEST_PERMISSION }

data class SmsUiState(
    val enabled: Boolean = false,
    val working: Boolean = false,
    val status: String? = null,
    val blocked: List<BlockedSms> = emptyList(),
    val review: List<ReviewSms> = emptyList(),
    /** Number linked to the signed-in account, e.g. "+919876543210". */
    val linkedPhone: String? = null,
    /** SMS capture on this phone belongs to a different FinAI account. */
    val ownedByOtherAccount: Boolean = false,
    val step: SmsSetupStep = SmsSetupStep.NONE,
    val phoneError: String? = null,
)

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val store = SmsStore(application)
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _state = MutableStateFlow(SmsUiState())
    val state: StateFlow<SmsUiState> = _state.asStateFlow()

    /** Called after anything is imported so the ledger can reload. */
    var onImported: (() -> Unit)? = null

    init {
        reload()
        viewModelScope.launch { syncLinkedPhone() }
        // Push anything captured while the app was closed / offline.
        if (isOwner() && store.enabled && store.pending().isNotEmpty()) upload(statusPrefix = null)
    }

    private fun isOwner() = uid != null && store.ownerUid == uid

    fun reload() {
        val owner = store.ownerUid
        _state.value = _state.value.copy(
            enabled = isOwner() && store.enabled && SmsCapture.hasSmsPermissions(app),
            ownedByOtherAccount = owner != null && owner != uid,
            linkedPhone = if (isOwner()) store.linkedPhone ?: _state.value.linkedPhone else _state.value.linkedPhone,
            blocked = if (isOwner()) store.blocked() else emptyList(),
            review = if (isOwner()) store.review() else emptyList(),
        )
    }

    /** The server is the authority on the linked number (it may have been unlinked on the web). */
    private suspend fun syncLinkedPhone() {
        val phone = runCatching { ApiClient.api.getSmsPhone().phone }.getOrElse { return }
        if (isOwner()) {
            store.linkedPhone = phone
            if (phone == null) store.enabled = false
        }
        _state.value = _state.value.copy(linkedPhone = phone)
        reload()
    }

    // ---- Turning capture on: owner check -> phone number -> permission -> import ----

    fun requestEnable() {
        _state.value = _state.value.copy(
            phoneError = null,
            step = when {
                _state.value.ownedByOtherAccount -> SmsSetupStep.SWITCH_OWNER
                _state.value.linkedPhone == null -> SmsSetupStep.ENTER_PHONE
                else -> SmsSetupStep.REQUEST_PERMISSION
            }
        )
    }

    /** User confirmed moving SMS capture on this phone to the signed-in account. */
    fun confirmSwitchOwner() {
        store.resetOwnership()
        reload()
        _state.value = _state.value.copy(
            ownedByOtherAccount = false,
            step = if (_state.value.linkedPhone == null) SmsSetupStep.ENTER_PHONE else SmsSetupStep.REQUEST_PERMISSION
        )
    }

    fun linkPhone(number: String) {
        if (_state.value.working) return
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, phoneError = null)
            runCatching { ApiClient.api.linkSmsPhone(SmsPhoneRequest(number)) }
                .onSuccess { res ->
                    _state.value = _state.value.copy(working = false, linkedPhone = res.phone, step = SmsSetupStep.REQUEST_PERMISSION)
                }
                .onFailure {
                    _state.value = _state.value.copy(working = false, phoneError = it.toUserMessage("Couldn't link that number."))
                }
        }
    }

    fun cancelSetup() {
        _state.value = _state.value.copy(step = SmsSetupStep.NONE, phoneError = null)
    }

    /** Result of the system permission dialog. */
    fun onPermissionResult() {
        _state.value = _state.value.copy(step = SmsSetupStep.NONE)
        if (!SmsCapture.hasSmsPermissions(app)) {
            _state.value = _state.value.copy(status = "SMS permission is needed to capture bank alerts.")
            reload()
            return
        }
        store.ownerUid = uid
        store.linkedPhone = _state.value.linkedPhone
        store.enabled = true
        reload()
        importInbox(days = 30)
    }

    fun disable() {
        store.enabled = false
        _state.value = _state.value.copy(status = "Auto-capture turned off.")
        reload()
    }

    /** Removes the number from this account and stops capture on this phone. */
    fun unlinkPhone() {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true)
            runCatching { ApiClient.api.unlinkSmsPhone() }
                .onSuccess {
                    if (isOwner()) store.resetOwnership()
                    _state.value = _state.value.copy(working = false, linkedPhone = null, status = "Number unlinked. SMS capture is off.")
                }
                .onFailure { _state.value = _state.value.copy(working = false, status = it.toUserMessage("Couldn't unlink.")) }
            reload()
        }
    }

    fun importInbox(days: Int = 30) {
        if (_state.value.working || !isOwner() || !SmsCapture.hasSmsPermissions(app)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, status = "Scanning your SMS inbox…")
            val summary = withContext(Dispatchers.Default) {
                SmsCapture.process(app, SmsCapture.readInbox(app, days))
            }
            store.lastImportAt = System.currentTimeMillis()
            val found = buildList {
                add("${summary.genuine} genuine bank alert${if (summary.genuine == 1) "" else "s"}")
                if (summary.blocked > 0) add("${summary.blocked} fake blocked")
                if (summary.review > 0) add("${summary.review} to review")
            }.joinToString(", ")
            upload(statusPrefix = "Last $days days: $found.")
        }
    }

    fun trust(header: String) {
        store.trust(header)
        val items = store.takeReview(header)
        store.enqueue(items.map { it.tx.copy(bankName = header) })
        reload()
        upload(statusPrefix = "Trusted $header.")
    }

    fun dismissReview(item: ReviewSms) {
        store.takeReview(item.header).filterNot { it.tx.hash == item.tx.hash }.forEach(store::addReview)
        reload()
    }

    fun clearBlocked() {
        store.clearBlocked()
        reload()
    }

    private fun upload(statusPrefix: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true)
            val status = runCatching { SmsCapture.uploadPending(app) }
                .fold(
                    onSuccess = { res ->
                        if (res != null && res.imported > 0) onImported?.invoke()
                        val added = res?.let {
                            "Added ${it.imported} new transaction${if (it.imported == 1) "" else "s"}" +
                                (if (it.duplicates > 0) " (${it.duplicates} already in FinAI)" else "") +
                                (if (it.otherAccount > 0) ". ${it.otherAccount} skipped: already in another account" else "") + "."
                        }
                        listOfNotNull(statusPrefix, added ?: if (statusPrefix != null) "Nothing new to add." else null)
                            .joinToString(" ").ifBlank { null }
                    },
                    onFailure = {
                        SmsCapture.scheduleUpload(app) // retry in the background
                        listOfNotNull(statusPrefix, it.toUserMessage("Upload failed") + " — will retry automatically.")
                            .joinToString(" ")
                    },
                )
            _state.value = _state.value.copy(working = false, status = status ?: _state.value.status)
            reload()
        }
    }
}
