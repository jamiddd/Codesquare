package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.OneTimeProduct

interface SubscriptionListener {
    fun onSubscriptionSelected(oneTimeProduct: OneTimeProduct, position: Int)
}