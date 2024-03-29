package com.jamid.codesquare
// something simple
import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class Collab: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        UserManager.initialize()

        // these are all custom objects, and hence require that firebase is
        // initialized first

        Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_KEY)
        Fresco.initialize(applicationContext)
    }

}