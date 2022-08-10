package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.jamid.codesquare.R
import com.jamid.codesquare.databinding.LocationItemBinding
import com.jamid.codesquare.listeners.LocationItemClickListener

class PlaceAdapter(
    private val locationClickListener: LocationItemClickListener,
    private val items: List<Place>
): RecyclerView.Adapter<LocationViewHolder>() {
    init {
        Log.d("Something", "Simple: ")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false), locationClickListener)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

class AutoCompletePredictionAdapter(
    private val locationClickListener: LocationItemClickListener,
    private val items: List<AutocompletePrediction>
): RecyclerView.Adapter<LocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false), locationClickListener)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

class LocationViewHolder(
    val view: View,
    private val locationClickListener: LocationItemClickListener
): RecyclerView.ViewHolder(view) {

    fun bind(place: Place) {
        val binding = LocationItemBinding.bind(view)
        binding.locationName.text = place.name
        binding.locationAddress.text = place.address

        binding.root.setOnClickListener {
            locationClickListener.onLocationClick(place)
        }
    }

    fun bind(autoCompletePrediction: AutocompletePrediction) {
        val binding = LocationItemBinding.bind(view)
        binding.locationName.text = autoCompletePrediction.getPrimaryText(null)
        binding.locationAddress.text = autoCompletePrediction.getSecondaryText(null)

        binding.root.setOnClickListener {
            locationClickListener.onLocationClick(autoCompletePrediction)
        }
    }

}