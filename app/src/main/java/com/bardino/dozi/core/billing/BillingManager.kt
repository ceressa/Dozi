package com.bardino.dozi.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💳 Billing Manager
 *
 * Manages Google Play Billing operations:
 * - Product queries
 * - Purchase flow
 * - Purchase verification
 * - Subscription management
 *
 * Usage:
 * 1. Initialize billing connection
 * 2. Query available products
 * 3. Launch purchase flow
 * 4. Handle purchase results
 * 5. Verify purchases on server
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs - MUST match Google Play Console configuration
        const val PRODUCT_WEEKLY = "dozi_weekly_premium"
        const val PRODUCT_MONTHLY = "dozi_monthly_premium"
        const val PRODUCT_YEARLY = "dozi_yearly_premium"
        const val PRODUCT_MONTHLY_FAMILY = "dozi_monthly_family_premium"
        const val PRODUCT_YEARLY_FAMILY = "dozi_yearly_family_premium"
    }

    // Billing client instance
    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Available products
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    // Purchase result
    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult: StateFlow<PurchaseResult?> = _purchaseResult.asStateFlow()

    // Billing connection state
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Initialize billing client and connect
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBilling()
    }

    /**
     * Connect to Google Play Billing
     */
    private fun connectToBilling() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing client connected")
                    _isReady.value = true
                    queryProducts()
                    queryPurchases() // Check existing purchases
                } else {
                    Log.e(TAG, "❌ Billing setup failed: ${billingResult.debugMessage}")
                    _isReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing service disconnected. Retrying...")
                _isReady.value = false
                // Retry connection
                connectToBilling()
            }
        })
    }

    /**
     * Query available subscription products from Google Play
     */
    private fun queryProducts() {
        scope.launch {
            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_WEEKLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY_FAMILY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_YEARLY_FAMILY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                withContext(Dispatchers.Main) {
                    billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "✅ Products queried: ${productDetailsList.size}")
                            _products.value = productDetailsList
                        } else {
                            Log.e(TAG, "❌ Product query failed: ${billingResult.debugMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception querying products", e)
            }
        }
    }

    /**
     * Launch purchase flow for a product
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val product = _products.value.find { it.productId == productId }

        if (product == null) {
            Log.e(TAG, "❌ Product not found: $productId")
            _purchaseResult.value = PurchaseResult.Error("Product not found")
            return
        }

        // Get subscription offer token (usually the first one)
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (offerToken == null) {
            Log.e(TAG, "❌ No offer token for product: $productId")
            _purchaseResult.value = PurchaseResult.Error("No subscription offer available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Callback when purchases are updated
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "⚠️ User canceled purchase")
                _purchaseResult.value = PurchaseResult.Cancelled
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "✅ Item already owned")
                _purchaseResult.value = PurchaseResult.AlreadyOwned
            }

            else -> {
                Log.e(TAG, "❌ Purchase failed: ${billingResult.debugMessage}")
                _purchaseResult.value = PurchaseResult.Error(billingResult.debugMessage)
            }
        }
    }

    /**
     * Handle successful purchase
     */
    private fun handlePurchase(purchase: Purchase) {
        scope.launch {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Verify purchase on your server here
                Log.d(TAG, "✅ Purchase successful: ${purchase.products}")

                // Acknowledge purchase if not already acknowledged
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                _purchaseResult.value = PurchaseResult.Success(
                    productId = purchase.products.firstOrNull() ?: "",
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId ?: ""
                )
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Log.d(TAG, "⏳ Purchase pending")
                _purchaseResult.value = PurchaseResult.Pending
            }
        }
    }

    /**
     * Acknowledge purchase (required for subscriptions)
     */
    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        withContext(Dispatchers.Main) {
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Purchase acknowledged")
                } else {
                    Log.e(TAG, "❌ Acknowledge failed: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * Query existing purchases (subscriptions)
     */
    fun queryPurchases() {
        scope.launch {
            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                withContext(Dispatchers.Main) {
                    billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "✅ Existing purchases: ${purchases.size}")
                            purchases.forEach { purchase ->
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    // User has active subscription
                                    Log.d(TAG, "✅ Active subscription: ${purchase.products}")
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ Query purchases failed: ${billingResult.debugMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception querying purchases", e)
            }
        }
    }

    /**
     * End billing connection
     */
    fun endConnection() {
        billingClient?.endConnection()
        _isReady.value = false
        Log.d(TAG, "🔌 Billing connection ended")
    }
}

/**
 * Purchase result sealed class
 */
sealed class PurchaseResult {
    data class Success(
        val productId: String,
        val purchaseToken: String,
        val orderId: String
    ) : PurchaseResult()

    object Pending : PurchaseResult()
    object Cancelled : PurchaseResult()
    object AlreadyOwned : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}
