package com.jamid.codesquare.adapter

import com.google.android.libraries.places.api.model.Place

interface LocationItemClickListener {
    fun onLocationClick(place: Place)
}