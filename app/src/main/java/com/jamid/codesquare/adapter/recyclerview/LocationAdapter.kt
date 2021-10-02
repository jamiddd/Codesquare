package com.jamid.codesquare.adapter.recyclerview

import android.location.Address
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class LocationAdapter: ListAdapter<Address, LocationViewHolder>(comparator) {

    companion object {
        val comparator = object : DiffUtil.ItemCallback<Address>() {
            override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
                return oldItem.featureName == newItem.featureName
            }

            override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
                return oldItem.getAddressLine(0) == newItem.getAddressLine(0)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder.newInstance(parent)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}