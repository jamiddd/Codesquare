package com.jamid.codesquare.listeners

import com.google.android.libraries.places.api.model.Place

interface LocationItemClickListener {
    fun onLocationClick(place: Place)
}