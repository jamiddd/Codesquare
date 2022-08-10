package com.jamid.codesquare.adapter.comparators

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.google.android.libraries.places.api.model.Place

class PlaceComparator: DiffUtil.ItemCallback<Place>() {

    override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
        return oldItem.id == newItem.id && oldItem.latLng == newItem.latLng
    }

    override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
        return oldItem.latLng != newItem.latLng
    }

    init {
        Log.d("Something", "Simple: ")
    }

}