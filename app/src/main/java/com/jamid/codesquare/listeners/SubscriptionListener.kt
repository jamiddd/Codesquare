package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Subscription

interface SubscriptionListener {
    fun onSubscriptionSelected(subscription: Subscription, position: Int)
}