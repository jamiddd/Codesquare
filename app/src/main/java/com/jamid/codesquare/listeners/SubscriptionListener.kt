package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.OneTimeProduct
// something simple
interface SubscriptionListener {
    fun onSubscriptionSelected(oneTimeProduct: OneTimeProduct, position: Int)
}