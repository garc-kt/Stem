package com.veggiebit.sprout.app

import android.app.Application
import com.veggiebit.sprout.features.settings.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SproutApplication : Application() {

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
        lateinit var instance: SproutApplication
            private set
    }
}
