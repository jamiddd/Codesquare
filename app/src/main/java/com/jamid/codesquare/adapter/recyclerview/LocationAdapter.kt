package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.Place
import com.jamid.codesquare.R
import com.jamid.codesquare.listeners.LocationItemClickListener
import com.jamid.codesquare.adapter.comparators.PlaceComparator
import com.jamid.codesquare.databinding.LocationItemBinding

class LocationAdapter(private val locationClickListener: LocationItemClickListener): ListAdapter<Place, LocationAdapter.LocationViewHolder>(PlaceComparator()) {

    inner class LocationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        fun bind(place: Place) {
            val binding = LocationItemBinding.bind(view)
            binding.locationName.text = place.name
            binding.locationAddress.text = place.address

            binding.root.setOnClickListener {
                locationClickListener.onLocationClick(place)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false))
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}