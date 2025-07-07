package org.billinghack

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.android.vending.billing.IInAppBillingService
import com.google.gson.Gson
import org.billinghack.google.util.IabHelper

/**
 * have to use nullable type for the functions of AIDL, because
 * the generated code is java and java may have null in that type
 *
 * if a null variable is passed into kotlin function, then it will throw an error
 * */
// have to use String?, because some of the parameter can be nullable, and
// I think the aidl generated java class will catch exception if passing null to this function
// so error won't be visible in the logcat

// kotlin param may not accept null !
class BillingService : Service() {
    companion object {
        const val savedFileJsonName = "BillingStorage.json"
        var billingStorage: BillingStorage? = null
        const val TAG = "BillingHack"
        const val MODIFIED_PRICE = 5.00
        const val MODIFIED_PRICE_MICRO = MODIFIED_PRICE * 10000000

        // New response codes from latest billing library
        const val BILLING_RESPONSE_RESULT_OK = 0
        const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
        const val BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2
        const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
        const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
        const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
        const val BILLING_RESPONSE_RESULT_ERROR = 6
        const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7
        const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8
        const val BILLING_RESPONSE_RESULT_NETWORK_ERROR = 12
    }

    init {
        billingStorage = BillingStorage(this)
    }

    fun logBundle(bundle: Bundle?) {
        Log.d(TAG, "printing bundle")
        for (key in bundle!!.keySet()) {
            Log.d(TAG, key + " = \"" + bundle.get(key) + "\"")
        }
    }

    override fun onCreate() {
        Log.d(TAG, "starting service")
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "service binded")
        return mBinder
    }

    private val mBinder: IInAppBillingService.Stub = object : IInAppBillingService.Stub() {
        // by going into IInAppBillingService.Stub, we can find that we can override
        // onTransact :), because the generated code is actually a the child of Binder
        @Synchronized
        @Throws(RemoteException::class)
        override fun onTransact(
            code: Int,
            data: Parcel,
            reply: Parcel?,
            flags: Int
        ): Boolean {
            Log.d(TAG, "entering onTransact:. code: $code")
            return super.onTransact(code, data, reply, flags)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int {
            Log.d(TAG, "isBillingSupported")
            return IabHelper.BILLING_RESPONSE_RESULT_OK
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getSkuDetails(
            apiVersion: Int, packageName: String, type: String?,
            skusBundle: Bundle?
        ): Bundle {
            Log.d(TAG, "getSkuDetails")
            Log.d(TAG, "apiVersion: $apiVersion")
            Log.d(TAG, "packageName: $packageName")
            Log.d(TAG, "type: $type")
            Log.d(TAG, "skusBundle: $skusBundle")

            // https://developer.android.com/google/play/billing/billing_reference#getSkuDetails
            // If getSkuDetails() method is successful, Google Play sends a response Bundle. The
            // query results are stored in the Bundle within a String ArrayList mapped to the
            // DETAILS_LIST key. Each String in the details list contains product details for a
            // single product in JSON format. The fields in the JSON string with the product details
            // are summarized in table 5.
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, IabHelper.BILLING_RESPONSE_RESULT_OK)
            val productDetails = ArrayList<String>()
            val items = skusBundle!!.getStringArrayList("ITEM_ID_LIST")
            val length = items!!.size
            for (i in 0 until length) {
                println(i)
                val item = items[i]

                val productDetailDummy = ProductDetail(
                    productId = item,
                    type = type!!,
                    price = "$${MODIFIED_PRICE}",
                    title = item,
                    description = "dummy description",
                    price_amount_micros = MODIFIED_PRICE_MICRO,
                    price_currency_code = "USD",
                )
                val productDetailJson = Gson().toJson(productDetailDummy)
                productDetails.add(productDetailJson)
            }
            Log.d(TAG, productDetails.toString())
            bundle.putStringArrayList(
                IabHelper.RESPONSE_GET_SKU_DETAILS_LIST,
                productDetails
            )
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getBuyIntent(
            apiVersion: Int, packageName: String, sku: String?, type: String?,
            developerPayload: String?
        ): Bundle {
            Log.d(TAG, "getBuyIntent")
            Log.d(TAG, "apiVersion: $apiVersion")
            Log.d(TAG, "packageName: $packageName")
            Log.d(TAG, "sku: $sku")
            Log.d(TAG, "type: $type")
            Log.d(TAG, "developerPayload: $developerPayload")
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, IabHelper.BILLING_RESPONSE_RESULT_OK)
            val pendingIntent: PendingIntent
            val intent = Intent()
            intent.setClass(applicationContext, BuyActivity::class.java)
            intent.action = BuyActivity.BUY_INTENT
            intent.putExtra(BuyActivity.EXTRA_PACKAGENAME, packageName)
            intent.putExtra(BuyActivity.EXTRA_PRODUCT_ID, sku)
            intent.putExtra(BuyActivity.EXTRA_DEV_PAYLOAD, developerPayload)
            // need flag mutable because of targeting S+ requirements
            pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            bundle.putParcelable(IabHelper.RESPONSE_BUY_INTENT, pendingIntent)
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getPurchases(
            apiVersion: Int, packageName: String, type: String?,
            continuationToken: String?
        ): Bundle {
            Log.d(TAG, "getPurchases")
            Log.d(TAG, "apiVersion: $apiVersion")
            Log.d(TAG, "packageName: $packageName")
            Log.d(TAG, "type: $type")
            Log.d(TAG, "continuationToken: $continuationToken")
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, IabHelper.BILLING_RESPONSE_RESULT_OK)

            if (!billingStorage!!.packageNameToPackagePurchaseDataMap.containsKey(packageName)) {
                return bundle
            }
            // get previous purchases data
            val packagePurchaseData: PackagePurchaseData =
                billingStorage!!.packageNameToPackagePurchaseDataMap[packageName]!!

            // convert first to array list of json
            val inappPurchaseDataStr: ArrayList<String> = ArrayList<String>()
            for (purchaseDetail: PurchaseDetail in packagePurchaseData.inappPurchaseDataList) {
                inappPurchaseDataStr.add(Gson().toJson(purchaseDetail))
            }

            // put to bundle
            bundle.putStringArrayList(
                IabHelper.RESPONSE_INAPP_ITEM_LIST,
                packagePurchaseData.inappPurchaseItemList,
            )

            bundle.putStringArrayList(
                IabHelper.RESPONSE_INAPP_PURCHASE_DATA_LIST,
                inappPurchaseDataStr,
            )
            bundle.putStringArrayList(
                IabHelper.RESPONSE_INAPP_SIGNATURE_LIST,
                packagePurchaseData.inappDataSignatureList,
            )
            Log.d(TAG, "bundle: $bundle")
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun consumePurchase(
            apiVersion: Int,
            packageName: String,
            purchaseToken: String?
        ): Int {
            Log.d(TAG, "consumePurchase")
            return IabHelper.BILLING_RESPONSE_RESULT_OK
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun stub(apiVersion: Int, packageName: String, type: String): Int {
            Log.d(TAG, "stub")
            return 0
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getBuyIntentToReplaceSkus(
            apiVersion: Int,
            packageName: String,
            oldSkus: List<String?>?,
            newSku: String?,
            type: String?,
            developerPayload: String?
        ): Bundle {
            Log.d(TAG, "getBuyIntentToReplaceSkus")
            return getBuyIntent(apiVersion, packageName, newSku, type, developerPayload)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getBuyIntentExtraParams(
            apiVersion: Int,
            packageName: String,
            sku: String?,
            type: String?,
            developerPayload: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getBuyIntentExtraParams")
            return getBuyIntent(apiVersion, packageName, sku, type, developerPayload)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getPurchaseHistory(
            apiVersion: Int,
            packageName: String,
            type: String?,
            continuationToken: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getPurchaseHistory")
            // Return purchases as history - this method is deprecated but still supported
            return getPurchases(apiVersion, packageName, type, continuationToken)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun isBillingSupportedExtraParams(
            apiVersion: Int,
            packageName: String,
            type: String?,
            extraParams: Bundle?
        ): Int {
            Log.d(TAG, "isBillingSupportedExtraParams")
            return BILLING_RESPONSE_RESULT_OK
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getSubscriptionManagementIntent(
            apiVersion: Int,
            packageName: String,
            sku: String?,
            type: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getSubscriptionManagementIntent  $packageName $sku $type")
            logBundle(extraParams)
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
            // Return empty intent for subscription management
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getPurchasesExtraParams(
            apiVersion: Int,
            packageName: String,
            type: String?,
            continuationToken: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getPurchasesExtraParams")
            return getPurchases(apiVersion, packageName, type, continuationToken)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun consumePurchaseExtraParams(
            apiVersion: Int,
            packageName: String,
            purchaseToken: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "consumePurchaseExtraParams with token: ${purchaseToken}")

            if (purchaseToken != null) {
                // remove this purchases
                billingStorage!!.packageNameToPackagePurchaseDataMap[packageName]?.removePurchaseByPurchaseToken(
                    purchaseToken
                )
                billingStorage!!.save(savedFileJsonName)
                Log.d(TAG, "consumed token ${purchaseToken}")
            }

            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, IabHelper.BILLING_RESPONSE_RESULT_OK)
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getSkuDetailsExtraParams(
            apiVersion: Int,
            packageName: String,
            type: String?,
            skusBundle: Bundle?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getSkuDetailsExtraParams")
            return getSkuDetails(apiVersion, packageName, type, skusBundle)
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun acknowledgePurchaseExtraParams(
            apiVersion: Int,
            packageName: String,
            purchaseToken: String?,
            extraParam: Bundle?
        ): Bundle {
            Log.d(TAG, "acknowledgePurchaseExtraParams")
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun isFeatureSupported(
            apiVersion: Int,
            packageName: String,
            feature: String?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "isFeatureSupported: $feature")
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
            
            // Support common features that apps might check for
            when (feature) {
                "subscriptions" -> bundle.putBoolean("SUPPORTED", true)
                "subscriptionsUpdate" -> bundle.putBoolean("SUPPORTED", true)
                "inAppItemsOnVr" -> bundle.putBoolean("SUPPORTED", false)
                "subscriptionsOnVr" -> bundle.putBoolean("SUPPORTED", false)
                "priceChangeConfirmation" -> bundle.putBoolean("SUPPORTED", true)
                "playBillingNewSubscriptionModel" -> bundle.putBoolean("SUPPORTED", true)
                else -> bundle.putBoolean("SUPPORTED", false)
            }
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getBillingConfig(
            apiVersion: Int,
            packageName: String,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "getBillingConfig")
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
            
            // Return billing configuration
            bundle.putString("COUNTRY_CODE", "US")
            bundle.putString("BILLING_COUNTRY", "US")
            return bundle
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun queryProductDetails(
            apiVersion: Int,
            packageName: String,
            productsBundle: Bundle?,
            extraParams: Bundle?
        ): Bundle {
            Log.d(TAG, "queryProductDetails - new subscription model support")
            Log.d(TAG, "apiVersion: $apiVersion")
            Log.d(TAG, "packageName: $packageName")
            
            val bundle = Bundle()
            bundle.putInt(IabHelper.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK)
            
            val productDetailsList = ArrayList<String>()
            val productList = productsBundle?.getStringArrayList("ITEM_ID_LIST")
            val productTypeList = productsBundle?.getStringArrayList("ITEM_TYPE_LIST")
            
            if (productList != null && productTypeList != null) {
                for (i in 0 until minOf(productList.size, productTypeList.size)) {
                    val productId = productList[i]
                    val productType = productTypeList[i]
                    
                    // Create enhanced product details with new subscription model support
                    val productDetails = createEnhancedProductDetails(productId, productType)
                    productDetailsList.add(productDetails)
                }
            }
            
            bundle.putStringArrayList("DETAILS_LIST", productDetailsList)
            return bundle
        }

        private fun createEnhancedProductDetails(productId: String, productType: String): String {
            return when (productType) {
                "subs" -> {
                    // Enhanced subscription details with base plans and offers
                    """
                    {
                        "productId": "$productId",
                        "type": "subs",
                        "title": "$productId Premium Subscription",
                        "description": "Premium features and content",
                        "subscriptionOfferDetails": [
                            {
                                "basePlanId": "monthly-base",
                                "offerId": null,
                                "offerToken": "monthly_offer_token_$productId",
                                "pricingPhases": [
                                    {
                                        "priceAmountMicros": ${MODIFIED_PRICE_MICRO.toLong()},
                                        "priceCurrencyCode": "USD",
                                        "formattedPrice": "$${MODIFIED_PRICE}",
                                        "billingPeriod": "P1M",
                                        "recurrenceMode": 1,
                                        "billingCycleCount": 0
                                    }
                                ]
                            },
                            {
                                "basePlanId": "yearly-base",
                                "offerId": "yearly-discount",
                                "offerToken": "yearly_offer_token_$productId",
                                "pricingPhases": [
                                    {
                                        "priceAmountMicros": ${(MODIFIED_PRICE_MICRO * 10).toLong()},
                                        "priceCurrencyCode": "USD", 
                                        "formattedPrice": "$${MODIFIED_PRICE * 10}",
                                        "billingPeriod": "P1Y",
                                        "recurrenceMode": 1,
                                        "billingCycleCount": 0
                                    }
                                ]
                            }
                        ]
                    }
                    """.trimIndent()
                }
                else -> {
                    // One-time product details
                    """
                    {
                        "productId": "$productId",
                        "type": "inapp",
                        "title": "$productId Premium",
                        "description": "Premium one-time purchase",
                        "oneTimePurchaseOfferDetails": {
                            "priceAmountMicros": ${MODIFIED_PRICE_MICRO.toLong()},
                            "priceCurrencyCode": "USD",
                            "formattedPrice": "$${MODIFIED_PRICE}"
                        }
                    }
                    """.trimIndent()
                }
            }
        }
    }
}