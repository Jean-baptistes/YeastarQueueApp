package com.yeastar.queuecaller.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistance simple (SharedPreferences) de la configuration PBX et du pool d'extensions.
 *
 * On évite volontairement une base de données : la quantité de données est minime
 * (une config + quelques extensions).
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Configuration PBX ----

    fun loadPbxConfig(): PbxConfig {
        return PbxConfig(
            domain = prefs.getString(KEY_DOMAIN, "") ?: "",
            port = prefs.getInt(KEY_PORT, 5060),
            transport = runCatching {
                SipTransport.valueOf(prefs.getString(KEY_TRANSPORT, SipTransport.UDP.name)!!)
            }.getOrDefault(SipTransport.UDP),
            queueNumber = prefs.getString(KEY_QUEUE, "6400") ?: "6400"
        )
    }

    fun savePbxConfig(config: PbxConfig) {
        prefs.edit()
            .putString(KEY_DOMAIN, config.domain)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_TRANSPORT, config.transport.name)
            .putString(KEY_QUEUE, config.queueNumber)
            .apply()
    }

    // ---- Pool d'extensions ----

    fun loadExtensions(): List<Extension> {
        val raw = prefs.getString(KEY_EXTENSIONS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Extension(
                    number = o.getString("number"),
                    password = o.getString("password"),
                    label = o.optString("label", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveExtensions(extensions: List<Extension>) {
        val arr = JSONArray()
        extensions.forEach { ext ->
            arr.put(
                JSONObject()
                    .put("number", ext.number)
                    .put("password", ext.password)
                    .put("label", ext.label)
            )
        }
        prefs.edit().putString(KEY_EXTENSIONS, arr.toString()).apply()
    }

    /** Numéro de la dernière extension utilisée (pour re-sélection au démarrage). */
    var lastUsedExtension: String?
        get() = prefs.getString(KEY_LAST_EXT, null)
        set(value) = prefs.edit().putString(KEY_LAST_EXT, value).apply()

    companion object {
        private const val PREFS_NAME = "yeastar_queue_caller"
        private const val KEY_DOMAIN = "pbx_domain"
        private const val KEY_PORT = "pbx_port"
        private const val KEY_TRANSPORT = "pbx_transport"
        private const val KEY_QUEUE = "pbx_queue"
        private const val KEY_EXTENSIONS = "extensions"
        private const val KEY_LAST_EXT = "last_used_extension"
    }
}
