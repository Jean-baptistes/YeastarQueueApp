package com.yeastar.queuecaller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yeastar.queuecaller.data.AppSettings
import com.yeastar.queuecaller.data.CallState
import com.yeastar.queuecaller.data.Extension
import com.yeastar.queuecaller.data.PbxConfig
import com.yeastar.queuecaller.data.RegistrationState
import com.yeastar.queuecaller.sip.CallForegroundService
import com.yeastar.queuecaller.sip.SipManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fait le lien entre l'UI Compose, la persistance (AppSettings) et la pile SIP (SipManager).
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = AppSettings(app)

    private val _pbxConfig = MutableStateFlow(settings.loadPbxConfig())
    val pbxConfig: StateFlow<PbxConfig> = _pbxConfig.asStateFlow()

    private val _extensions = MutableStateFlow(settings.loadExtensions())
    val extensions: StateFlow<List<Extension>> = _extensions.asStateFlow()

    private val _selectedExtension = MutableStateFlow<Extension?>(null)
    val selectedExtension: StateFlow<Extension?> = _selectedExtension.asStateFlow()

    // Réexposition directe des états de la pile SIP
    val registrationState: StateFlow<RegistrationState> = SipManager.registrationState
    val callState: StateFlow<CallState> = SipManager.callState
    val lastError: StateFlow<String?> = SipManager.lastError
    val muted: StateFlow<Boolean> = SipManager.muted
    val speakerOn: StateFlow<Boolean> = SipManager.speakerOn

    init {
        // Suivre l'état d'appel pour démarrer/arrêter le service de premier plan.
        viewModelScope.launch {
            SipManager.callState.collect { state ->
                when (state) {
                    CallState.OUTGOING_INIT,
                    CallState.OUTGOING_RINGING,
                    CallState.CONNECTED -> CallForegroundService.start(getApplication())
                    else -> CallForegroundService.stop(getApplication())
                }
            }
        }
    }

    // ---- Configuration PBX ----

    fun updatePbxConfig(config: PbxConfig) {
        _pbxConfig.value = config
        settings.savePbxConfig(config)
    }

    // ---- Pool d'extensions ----

    fun addExtension(ext: Extension) {
        val list = _extensions.value.toMutableList()
        // Remplacer si le numéro existe déjà
        val idx = list.indexOfFirst { it.number == ext.number }
        if (idx >= 0) list[idx] = ext else list.add(ext)
        _extensions.value = list
        settings.saveExtensions(list)
    }

    fun removeExtension(ext: Extension) {
        val list = _extensions.value.filterNot { it.number == ext.number }
        _extensions.value = list
        settings.saveExtensions(list)
        if (_selectedExtension.value?.number == ext.number) {
            _selectedExtension.value = null
        }
    }

    // ---- Sélection & enregistrement ----

    /** Sélectionne une extension du pool et lance son enregistrement SIP. */
    fun selectAndRegister(ext: Extension) {
        val config = _pbxConfig.value
        if (!config.isValid()) {
            return
        }
        _selectedExtension.value = ext
        settings.lastUsedExtension = ext.number
        SipManager.register(ext, config)
    }

    /**
     * Enregistre automatiquement la première extension du pool.
     * (La détection fine de disponibilité — présence/BLF — nécessite l'API Yeastar :
     *  voir le README. Ici, "disponible" = s'enregistre correctement.)
     */
    fun autoSelectFirst() {
        _extensions.value.firstOrNull()?.let { selectAndRegister(it) }
    }

    fun unregister() {
        SipManager.clearRegistration()
        _selectedExtension.value = null
    }

    // ---- Appels ----

    fun callQueue() = SipManager.callQueue(_pbxConfig.value)
    fun hangUp() = SipManager.hangUp()
    fun toggleMute() = SipManager.toggleMute()
    fun toggleSpeaker() = SipManager.toggleSpeaker()
    fun dismissError() = SipManager.clearError()
    fun acknowledgeCallEnded() = SipManager.resetCallState()

    fun restoreLastSelection() {
        val last = settings.lastUsedExtension ?: return
        _extensions.value.firstOrNull { it.number == last }?.let {
            _selectedExtension.value = it
        }
    }
}
