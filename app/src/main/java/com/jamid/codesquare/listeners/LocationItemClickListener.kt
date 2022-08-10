package com.jamid.codesquare.listeners

import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
// something simple
interface LocationItemClickListener {
    fun onLocationClick(place: Place)
    fun onLocationClick(autocompletePrediction: AutocompletePrediction)
}