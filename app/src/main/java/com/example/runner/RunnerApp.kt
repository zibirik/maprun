package com.example.runner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class RunnerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // MapLibre не требует API-ключа — инициализируем движок карт.
        MapLibre.getInstance(this)
    }
}
