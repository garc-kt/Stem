package com.veggiebit.sprout.app

import android.app.Application
import com.veggiebit.sprout.features.settings.data.PreferencesRepository

class SproutApplication : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(this)
    }

    companion object {
        lateinit var instance: SproutApplication
            private set
    }
}
