package com.stem.app

import android.app.Application
import com.stem.core.models.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch



class StemApplication : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(this)

        // One-shot, idempotent: encrypts any API keys still stored in plaintext by a version
        // prior to Keystore encryption. No-op on every subsequent launch.
        applicationScope.launch {
            preferencesRepository.migrateLegacyPlaintextKeysIfNeeded()
        }
    }

    companion object {
        lateinit var instance: StemApplication
            private set
    }
}
