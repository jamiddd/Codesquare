package com.jamid.codesquare

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.google.firebase.functions.FirebaseFunctions
import org.json.JSONObject

class PlayBillingController(private val mContext: Context): LifecycleEventObserver {

    var billingClient: BillingClient? = null
    val isPurchaseAcknowledged = MutableLiveData<Boolean>()
    val premiumState = MutableLiveData<PremiumState>()
    val purchaseDetails = MutableLiveData<List<SkuDetails>>()

    private fun verifyPurchase(purchase: Purchase) {

        val data = mapOf(
            PURCHASE_TOKEN to purchase.purchaseToken,
            PURCHASE_TIME to purchase.purchaseTime,
            PURCHASE_ORDER_ID to purchase.orderId,
            USER_ID to UserManager.currentUserId,
            PRODUCT_ID to JSONObject(purchase.originalJson).get(PRODUCT_ID)
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable(VERIFY_PURCHASE)
            .call(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    try {
                        val purchaseInfoFromServer = JSONObject(it.result.data.toString())
                        if (purchaseInfoFromServer.getBoolean(IS_VALID)) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams
                                .newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()

                            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { it1 ->
                                if (it1.responseCode == BillingClient.BillingResponseCode.OK) {
                                    isPurchaseAcknowledged.postValue(true)
                                } else {
                                    Log.e(TAG, "verifyPurchase: ${it1.debugMessage}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "verifyPurchase: ${e.localizedMessage}")
                    }
                } else {
                    mContext.toast("Something went wrong: " + it.exception?.localizedMessage.orEmpty())
                }
            }
    }

    private fun querySubscriptions() {
        billingClient?.queryPurchasesAsync(BillingClient.SkuType.SUBS) { p0, p1 ->
            if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                if (p1.isNotEmpty()) {
                    // check for subscriptions here
                    onPurchasesFetched(p1)
                } else {
                    premiumState.postValue(PremiumState.STATE_NO_PURCHASE)
                    updatePremiumState(PremiumState.STATE_NO_PURCHASE.state)
                }
            }
        }
    }

    private fun updatePremiumState(state: Long) {
        FireUtility.updateUser2(mapOf(PREMIUM_STATE to state)) {
            if (it.isSuccessful) {
                if (state == PremiumState.STATE_NO_PURCHASE.state) {
                    FireUtility.removeSubscriptions {
                        Log.d(TAG, "Deleted purchases")
                    }
                }
            } else {
                Log.e(TAG, "updatePremiumState: ${it.exception?.localizedMessage}")
            }
        }
    }

    private fun onPurchasesFetched(purchases: List<Purchase>) {
        try {
            if (purchases.size > 1) {
                throw IllegalStateException("There cannot be more than one subscription at a time.")
            } else {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        verifyPurchase(purchase)
                    } else {
                        when (JSONObject(purchase.originalJson).get(PRODUCT_ID)) {
                            HALF_SUBSCRIPTION -> {
                                updatePremiumState(PremiumState.STATE_HALF_PURCHASE.state)
                            }
                            FULL_SUBSCRIPTION -> {
                                updatePremiumState(PremiumState.STATE_FULL_PURCHASE.state)
                            }
                            else -> {
                                //
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onPurchasesFetched: ${e.localizedMessage}")
        }
    }

    private fun connectToGooglePlayBilling() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                connectToGooglePlayBilling()
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    getSubscriptionDetails()
                    querySubscriptions()
                }
            }
        })
    }

    private fun getSubscriptionDetails() {
        val subscriptionIds = mutableListOf<String>()

        subscriptionIds.add(HALF_SUBSCRIPTION)
        subscriptionIds.add(FULL_SUBSCRIPTION)

        val query = SkuDetailsParams.newBuilder()
            .setSkusList(subscriptionIds)
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient?.querySkuDetailsAsync(
            query
        ) { p0, p1 ->
            if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                purchaseDetails.postValue(p1)
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_CREATE -> {
                billingClient = BillingClient.newBuilder(mContext)
                    .enablePendingPurchases()
                    .setListener { p0, p1 ->
                        if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                            for (purchase in p1) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                    verifyPurchase(purchase)
                                }
                            }
                        }
                    }
                    .build()

                connectToGooglePlayBilling()

            }
            ON_RESUME -> {
                querySubscriptions()
            }
            else -> {
                Log.d(TAG, "onStateChanged: state not in use")
            }
        }
    }

    enum class PremiumState(val state: Long) {
        STATE_NO_PURCHASE(-1), STATE_HALF_PURCHASE(0), STATE_FULL_PURCHASE(1)
    }

    companion object {
        private const val TAG = "PlayBillingController"
        private const val HALF_SUBSCRIPTION = "remove_ads_subscription"
        private const val FULL_SUBSCRIPTION = "premium_membership_subscription"
        private const val PRODUCT_ID = "productId"
        private const val PREMIUM_STATE = "premiumState"
        private const val PURCHASE_TOKEN = "purchaseToken"
        private const val PURCHASE_TIME = "purchaseTime"
        private const val PURCHASE_ORDER_ID = "purchaseOrderId"
        private const val USER_ID = "userId"
        private const val VERIFY_PURCHASE = "verifyPurchase"
        private const val IS_VALID = "isValid"
    }

}