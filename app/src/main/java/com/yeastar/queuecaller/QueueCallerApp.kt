package com.yeastar.queuecaller

import android.app.Application
import com.yeastar.queuecaller.sip.SipManager

/**
 * Classe Application : initialise la pile SIP une fois pour toute la durée de vie du processus.
 */
class QueueCallerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SipManager.initialize(this)
    }
}
