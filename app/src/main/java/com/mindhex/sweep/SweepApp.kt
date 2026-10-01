package com.mindhex.sweep

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SweepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise the Ads SDK once, off the main thread as recommended.
        MobileAds.initialize(this) { }
    }
}
