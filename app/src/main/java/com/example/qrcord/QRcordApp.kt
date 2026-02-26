package com.example.qrcord

import android.app.Application
import android.util.Log
import com.example.qrcord.BuildConfig
import com.example.qrcord.ads.AdManager
import com.example.qrcord.billing.BillingManager
import com.google.firebase.crashlytics.FirebaseCrashlytics

class QRcordApp : Application() {

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        initializeCrashlytics()
        initializeAds()
        initializeBilling()
    }

    private fun initializeAds() {
        AdManager.getInstance(this).initialize()
    }

    private fun initializeBilling() {
        billingManager = BillingManager(this)
    }

    private fun initializeCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Enable Crashlytics collection (can be disabled for debug builds if needed)
            crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

            // Set custom keys for better crash context
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)

            Log.d(TAG, "Crashlytics initialized successfully")
        } catch (e: Exception) {
            // Log the error but don't crash the app
            Log.e(TAG, "Failed to initialize Crashlytics", e)
        }
    }

    companion object {
        private const val TAG = "QRcordApp"
    }
}
