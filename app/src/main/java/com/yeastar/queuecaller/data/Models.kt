package com.yeastar.queuecaller.data

/**
 * Paramètres de connexion au PBX Yeastar P-Series.
 *
 * - [domain] : adresse du PBX (IP ou nom d'hôte), ex. "192.168.1.10" ou "pbx.monentreprise.com"
 * - [port]   : port SIP. UDP/TCP par défaut = 5060, TLS = 5061.
 * - [transport] : Udp / Tcp / Tls. Yeastar P-Series recommande TLS en production.
 * - [queueNumber] : numéro de la file d'attente à appeler (par défaut 6400).
 */
data class PbxConfig(
    val domain: String = "",
    val port: Int = 5060,
    val transport: SipTransport = SipTransport.UDP,
    val queueNumber: String = "6400"
) {
    fun isValid(): Boolean = domain.isNotBlank() && port in 1..65535 && queueNumber.isNotBlank()
}

enum class SipTransport { UDP, TCP, TLS }

/**
 * Une extension du "pool" disponible dans l'application.
 *
 * - [number]   : le numéro d'extension (ex. "1001")
 * - [password] : mot de passe SIP d'enregistrement de l'extension (défini dans le PBX Yeastar)
 * - [label]    : libellé facultatif affiché à l'utilisateur (ex. "Poste accueil")
 */
data class Extension(
    val number: String,
    val password: String,
    val label: String = ""
) {
    val displayName: String
        get() = if (label.isBlank()) number else "$number — $label"
}

/**
 * État d'enregistrement SIP d'une extension.
 */
enum class RegistrationState {
    NONE,        // pas encore tenté
    PROGRESS,    // enregistrement en cours
    REGISTERED,  // enregistré → extension disponible
    FAILED,      // échec (mauvais identifiants, réseau, extension déjà utilisée...)
    CLEARED      // désenregistré
}

/**
 * État d'un appel en cours.
 */
enum class CallState {
    IDLE,
    OUTGOING_INIT,   // composition
    OUTGOING_RINGING,// ça sonne côté file
    CONNECTED,       // en communication
    ENDED,
    ERROR
}
