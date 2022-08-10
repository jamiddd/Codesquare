package com.jamid.codesquare

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.*
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
// something simple
class PlayBillingController(private val mContext: Context): LifecycleEventObserver, PurchasesUpdatedListener {

    var billingClient: BillingClient? = null
    val isPurchaseAcknowledged = MutableLiveData<Boolean>()
    val premiumState = MutableLiveData<PremiumState>()
    val purchaseDetails = MutableLiveData<List<ProductDetails>>()
    val errors = MutableLiveData<Exception>()

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
                                    FireUtility.updateUser2(mapOf("hasTicket" to true), false) { task ->
                                        if (!task.isSuccessful) {
                                            Log.e(TAG, "verifyPurchase: ${task.exception?.localizedMessage}")
                                        }
                                    }

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

    private fun queryProducts() {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("one_time_premium")
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build())
                ).build()

        billingClient?.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
//                    onPurchasesFetched(productDetailsList)
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

    /*private fun onPurchasesFetched(purchases: List<ProductDetails>) {
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
    }*/

    private fun connectToGooglePlayBilling(scope: CoroutineScope) {

        Log.d(TAG, "connectToGooglePlayBilling: connecting to google play")

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected: Disconnected")
                connectToGooglePlayBilling(scope)
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: ${p0.responseCode}")
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch (Dispatchers.IO) {
                        getProductDetails()
                    }
                }
            }
        })
    }

    /*suspend fun processPurchases() {
        val productList = mutableListOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("one_time_premium")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)


        // leverage queryProductDetails Kotlin extension function
        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.queryProductDetails(params.build())
        }

        // Process the result
        productDetailsResult?.let { processProducts(it) }

    }*/

    /*private fun processProducts(productDetailsResult: ProductDetailsResult) {
        if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val product = productDetailsResult.productDetailsList?.get(0)
            if (product != null) {
                // here is the product
                // show the product
            }
        }
    }*/

    private suspend fun getProductDetails() {

        Log.d(TAG, "getProductDetails: Getting product details")

        val productList = mutableListOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("weekly_rank_registration")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)

        // leverage queryProductDetails Kotlin extension function
        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient?.queryProductDetails(params.build())
        }

        if (productDetailsResult != null) {

            Log.d(TAG, "getProductDetails: productdetailsresult is not null")

            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsResult.productDetailsList != null) {
                Log.d(TAG, "getProductDetails: Everything ok")
                purchaseDetails.postValue(productDetailsResult.productDetailsList!!)
            } else {
                if (productDetailsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Not ok ${productDetailsResult.billingResult.responseCode} ${productDetailsResult.productDetailsList}")
                }

                if (productDetailsResult.productDetailsList == null) {
                    Log.d(TAG, "getProductDetails: NULL")
                }

            }
        } else {
            Log.d(TAG, "getProductDetails: productdetailsresult is null")
        }

    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_CREATE -> {
                val purchasesUpdatedListener =
                    PurchasesUpdatedListener { p0, p1 ->
                        if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
                            for (purchase in p1) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                    verifyPurchase(purchase)
                                }
                            }
                        }
                    }

                billingClient = BillingClient.newBuilder(mContext)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases()
                    .build()

                connectToGooglePlayBilling(source.lifecycle.coroutineScope)

            }
            ON_RESUME -> {
                source.lifecycle.coroutineScope.launch (Dispatchers.IO) {
//                    getProductDetails()
                }
            }
            else -> {
                Log.d(TAG, "onStateChanged: state not in use")
            }
        }
    }

    enum class PremiumState(val state: Long) {
        STATE_NO_PURCHASE(-1), STATE_HALF_PURCHASE(0), STATE_FULL_PURCHASE(1)
    }

    private fun handleNonConsumablePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    if (billingResponseCode == BillingClient.BillingResponseCode.OK) {
                        verifyPurchase(purchase)
                    }
                }
            }
        }
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

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handleNonConsumablePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            val exc = Exception("User has canceled the flow")
            errors.postValue(exc)
        } else {
            // Handle any other error codes.
            val exc = Exception("Unknown error occurred")
            errors.postValue(exc)
        }
    }

}