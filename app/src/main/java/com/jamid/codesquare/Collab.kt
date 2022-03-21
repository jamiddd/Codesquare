package com.jamid.codesquare

import android.app.Application
import androidx.preference.PreferenceManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class Collab: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        Fresco.initialize(applicationContext)
        Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_KEY)

        // these are all custom objects, and hence require that firebase is
        // initialized first
        UserManager.initialize()
        ChatManager.initialize()

    }

}