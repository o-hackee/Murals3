package com.example.murals3

import android.app.Application
import timber.log.Timber

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(FileLoggingTree(applicationContext))
    }
}