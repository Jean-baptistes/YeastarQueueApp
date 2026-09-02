package com.yeastar.queuecaller.sip

import android.content.Context
import android.util.Log
import com.yeastar.queuecaller.data.CallState
import com.yeastar.queuecaller.data.Extension
import com.yeastar.queuecaller.data.PbxConfig
import com.yeastar.queuecaller.data.RegistrationState
import com.yeastar.queuecaller.data.SipTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState as LinphoneRegistrationState
import org.linphone.core.TransportType

/**
 * Gère toute la logique SIP via le SDK Linphone :
 *  - enregistrement d'une extension Yeastar (le "poste" choisi dans le pool)
 *  - suivi réactif de l'état d'enregistrement et de l'appel
 *  - appel de la file d'attente (queue 6400) et raccrochage
 *
 * C'est un singleton : une seule pile SIP (Core) pour toute l'application.
 */
object SipManager {

    private const val TAG = "SipManager"

    private lateinit var core: Core
    private var initialized = false

    private var currentAccount: Account? = null
    private var currentCall: Call? = null

    // --- États exposés à l'UI ---

    private val _registrationState = MutableStateFlow(RegistrationState.NONE)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    private val _registeredExtension = MutableStateFlow<Extension?>(null)
    val registeredExtension: StateFlow<Extension?> = _registeredExtension.asStateFlow()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    // --- Écouteur d'événements du Core Linphone ---

    private val coreListener = object : CoreListenerStub() {

        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: LinphoneRegistrationState?,
            message: String
        ) {
            Log.i(TAG, "Enregistrement: $state ($message)")
            when (state) {
                LinphoneRegistrationState.Progress -> _registrationState.value = RegistrationState.PROGRESS
                LinphoneRegistrationState.Ok -> {
                    _registrationState.value = RegistrationState.REGISTERED
                    _lastError.value = null
                }
                LinphoneRegistrationState.Cleared -> _registrationState.value = RegistrationState.CLEARED
                LinphoneRegistrationState.Failed -> {
                    _registrationState.value = RegistrationState.FAILED
                    _lastError.value = "Échec d'enregistrement : $message"
                }
                else -> { /* None */ }
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            Log.i(TAG, "Appel: $state ($message)")
            currentCall = call
            when (state) {
                Call.State.OutgoingInit,
                Call.State.OutgoingProgress -> _callState.value = CallState.OUTGOING_INIT
                Call.State.OutgoingRinging,
                Call.State.OutgoingEarlyMedia -> _callState.value = CallState.OUTGOING_RINGING
                Call.State.Connected,
                Call.State.StreamsRunning -> _callState.value = CallState.CONNECTED
                Call.State.End,
                Call.State.Released -> {
                    _callState.value = CallState.ENDED
                    currentCall = null
                    _muted.value = false
                    _speakerOn.value = false
                }
                Call.State.Error -> {
                    _callState.value = CallState.ERROR
                    _lastError.value = "Erreur d'appel : $message"
                    currentCall = null
                }
                else -> { /* Idle, Paused, etc. */ }
            }
        }
    }

    // --- Cycle de vie de la pile SIP ---

    /** À appeler une seule fois, au démarrage de l'application. */
    fun initialize(context: Context) {
        if (initialized) return
        core = Factory.instance().createCore(null, null, context.applicationContext)
        core.isAutoIterateEnabled = true      // le Core s'itère seul (pas de boucle manuelle)
        core.isVideoCaptureEnabled = false    // application audio uniquement
        core.isVideoDisplayEnabled = false
        core.addListener(coreListener)
        core.start()
        initialized = true
        Log.i(TAG, "Core Linphone démarré")
    }

    // --- Enregistrement d'une extension ---

    /**
     * Enregistre l'extension [ext] auprès du PBX défini par [config].
     * Si une extension était déjà enregistrée, elle est d'abord retirée.
     */
    fun register(ext: Extension, config: PbxConfig) {
        require(initialized) { "SipManager.initialize() doit être appelé d'abord" }
        clearRegistration()

        _lastError.value = null
        _registrationState.value = RegistrationState.PROGRESS

        val transport = when (config.transport) {
            SipTransport.UDP -> TransportType.Udp
            SipTransport.TCP -> TransportType.Tcp
            SipTransport.TLS -> TransportType.Tls
        }

        // Identifiants SIP : username = numéro d'extension, mot de passe = celui du PBX.
        val authInfo = Factory.instance().createAuthInfo(
            ext.number,      // username
            null,            // userid
            ext.password,    // password
            null,            // ha1
            null,            // realm (laissé au serveur)
            config.domain    // domain
        )
        core.addAuthInfo(authInfo)

        val accountParams = core.createAccountParams()

        val identity = Factory.instance().createAddress("sip:${ext.number}@${config.domain}")
        val serverAddress = Factory.instance().createAddress("sip:${config.domain}:${config.port}")
        if (identity == null || serverAddress == null) {
            _registrationState.value = RegistrationState.FAILED
            _lastError.value = "Adresse PBX invalide : ${config.domain}"
            return
        }
        serverAddress.transport = transport

        accountParams.identityAddress = identity
        accountParams.serverAddress = serverAddress
        accountParams.isRegisterEnabled = true

        val account = core.createAccount(accountParams)
        core.addAccount(account)
        core.defaultAccount = account
        currentAccount = account
        _registeredExtension.value = ext

        core.refreshRegisters()
    }

    /** Retire l'extension courante et vide les identifiants (désenregistrement inclus). */
    fun clearRegistration() {
        if (!initialized) return
        // clearAccounts() envoie automatiquement le désenregistrement au PBX.
        core.clearAccounts()
        core.clearAllAuthInfo()
        currentAccount = null
        _registeredExtension.value = null
        _registrationState.value = RegistrationState.NONE
    }

    // --- Appels ---

    /** Compose la file d'attente (ex. 6400) avec l'extension enregistrée. */
    fun callQueue(config: PbxConfig) {
        require(initialized) { "SipManager.initialize() doit être appelé d'abord" }
        if (_registrationState.value != RegistrationState.REGISTERED) {
            _lastError.value = "Aucune extension enregistrée : impossible d'appeler."
            return
        }
        if (currentCall != null) {
            Log.w(TAG, "Un appel est déjà en cours")
            return
        }
        _lastError.value = null

        val remoteUri = "sip:${config.queueNumber}@${config.domain}"
        val remoteAddress = Factory.instance().createAddress(remoteUri)
        if (remoteAddress == null) {
            _lastError.value = "Numéro de file invalide : ${config.queueNumber}"
            _callState.value = CallState.ERROR
            return
        }

        _callState.value = CallState.OUTGOING_INIT
        // Appel audio uniquement : la vidéo est déjà désactivée au niveau du Core.
        val params = core.createCallParams(null)
        currentCall = if (params != null) {
            core.inviteAddressWithParams(remoteAddress, params)
        } else {
            core.inviteAddress(remoteAddress)
        }
    }

    /** Raccroche l'appel en cours (le cas échéant). */
    fun hangUp() {
        if (!initialized) return
        currentCall?.terminate() ?: core.currentCall?.terminate()
        core.terminateAllCalls()
    }

    fun toggleMute() {
        val call = currentCall ?: return
        val newState = !call.microphoneMuted
        call.microphoneMuted = newState
        _muted.value = newState
    }

    fun toggleSpeaker() {
        if (!initialized) return
        val target = if (_speakerOn.value) AudioDevice.Type.Earpiece else AudioDevice.Type.Speaker
        val device = core.audioDevices.firstOrNull {
            it.type == target && it.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
        }
        if (device != null) {
            currentCall?.outputAudioDevice = device
            core.outputAudioDevice = device
            _speakerOn.value = !_speakerOn.value
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    fun isInCall(): Boolean = currentCall != null

    /** Réinitialise l'état d'appel après la fin d'un appel (pour revenir à IDLE côté UI). */
    fun resetCallState() {
        if (currentCall == null) _callState.value = CallState.IDLE
    }
}
