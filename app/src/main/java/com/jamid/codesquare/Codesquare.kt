package com.jamid.codesquare

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class Codesquare: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        Fresco.initialize(applicationContext)
        Places.initialize(applicationContext, getString(R.string.google_maps_key))

        // these are all custom objects, and hence require that firebase is
        // initialized first
        UserManager.initialize()
    }

}