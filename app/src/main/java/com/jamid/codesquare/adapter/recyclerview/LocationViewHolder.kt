package com.jamid.codesquare.adapter.recyclerview

import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.LocationItemClickListener

class LocationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val locationName: TextView = view.findViewById(R.id.location_name)
    private val locationAddress: TextView = view.findViewById(R.id.location_address)

    private val locationItemClickListener = view.context as LocationItemClickListener

    fun bind(address: Address) {

        val name = address.featureName
        val addressText = address.getAddressLine(0)

        locationName.text = name
        locationAddress.text = addressText



    }

    companion object {

        fun newInstance(parent: ViewGroup): LocationViewHolder {
            return LocationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false))
        }

    }

}