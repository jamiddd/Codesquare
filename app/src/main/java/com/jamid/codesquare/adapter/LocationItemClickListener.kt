package com.jamid.codesquare.adapter

import android.location.Address

interface LocationItemClickListener {
    fun onLocationClick(address: Address)
}