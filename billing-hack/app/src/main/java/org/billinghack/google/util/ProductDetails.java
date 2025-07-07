package org.billinghack.google.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an in-app product's or subscription's details for Google Play Billing Library 7+.
 * This class supports the new subscription model with base plans and offers.
 */
public class ProductDetails {
    private String mProductId;
    private String mProductType;
    private String mTitle;
    private String mDescription;
    private String mJson;
    
    // For subscriptions
    private List<SubscriptionOfferDetails> mSubscriptionOfferDetails;
    
    // For one-time products
    private OneTimePurchaseOfferDetails mOneTimePurchaseOfferDetails;

    public ProductDetails(String productType, String jsonProductDetails) throws JSONException {
        mProductType = productType;
        mJson = jsonProductDetails;
        JSONObject o = new JSONObject(mJson);
        
        mProductId = o.optString("productId");
        mTitle = o.optString("title");
        mDescription = o.optString("description");
        
        if ("subs".equals(productType)) {
            mSubscriptionOfferDetails = parseSubscriptionOfferDetails(o);
        } else {
            mOneTimePurchaseOfferDetails = parseOneTimePurchaseOfferDetails(o);
        }
    }

    private List<SubscriptionOfferDetails> parseSubscriptionOfferDetails(JSONObject productJson) throws JSONException {
        List<SubscriptionOfferDetails> offerDetails = new ArrayList<>();
        JSONArray offersArray = productJson.optJSONArray("subscriptionOfferDetails");
        
        if (offersArray != null) {
            for (int i = 0; i < offersArray.length(); i++) {
                JSONObject offerJson = offersArray.getJSONObject(i);
                offerDetails.add(new SubscriptionOfferDetails(offerJson));
            }
        }
        
        return offerDetails;
    }

    private OneTimePurchaseOfferDetails parseOneTimePurchaseOfferDetails(JSONObject productJson) throws JSONException {
        JSONObject offerJson = productJson.optJSONObject("oneTimePurchaseOfferDetails");
        if (offerJson != null) {
            return new OneTimePurchaseOfferDetails(offerJson);
        }
        return null;
    }

    public String getProductId() { return mProductId; }
    public String getProductType() { return mProductType; }
    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    public String toString() { return "ProductDetails:" + mJson; }

    public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() {
        return mSubscriptionOfferDetails;
    }

    public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() {
        return mOneTimePurchaseOfferDetails;
    }

    /**
     * Represents subscription offer details with base plans and pricing phases.
     */
    public static class SubscriptionOfferDetails {
        private String mBasePlanId;
        private String mOfferId;
        private String mOfferToken;
        private List<PricingPhase> mPricingPhases;

        public SubscriptionOfferDetails(JSONObject offerJson) throws JSONException {
            mBasePlanId = offerJson.optString("basePlanId");
            mOfferId = offerJson.optString("offerId");
            mOfferToken = offerJson.optString("offerToken");
            
            mPricingPhases = new ArrayList<>();
            JSONArray phasesArray = offerJson.optJSONArray("pricingPhases");
            if (phasesArray != null) {
                for (int i = 0; i < phasesArray.length(); i++) {
                    JSONObject phaseJson = phasesArray.getJSONObject(i);
                    mPricingPhases.add(new PricingPhase(phaseJson));
                }
            }
        }

        public String getBasePlanId() { return mBasePlanId; }
        public String getOfferId() { return mOfferId; }
        public String getOfferToken() { return mOfferToken; }
        public List<PricingPhase> getPricingPhases() { return mPricingPhases; }
    }

    /**
     * Represents one-time purchase offer details.
     */
    public static class OneTimePurchaseOfferDetails {
        private long mPriceAmountMicros;
        private String mPriceCurrencyCode;
        private String mFormattedPrice;

        public OneTimePurchaseOfferDetails(JSONObject offerJson) throws JSONException {
            mPriceAmountMicros = offerJson.optLong("priceAmountMicros");
            mPriceCurrencyCode = offerJson.optString("priceCurrencyCode");
            mFormattedPrice = offerJson.optString("formattedPrice");
        }

        public long getPriceAmountMicros() { return mPriceAmountMicros; }
        public String getPriceCurrencyCode() { return mPriceCurrencyCode; }
        public String getFormattedPrice() { return mFormattedPrice; }
    }

    /**
     * Represents a pricing phase for subscription offers.
     */
    public static class PricingPhase {
        private long mPriceAmountMicros;
        private String mPriceCurrencyCode;
        private String mFormattedPrice;
        private String mBillingPeriod;
        private int mRecurrenceMode;
        private int mBillingCycleCount;

        public PricingPhase(JSONObject phaseJson) throws JSONException {
            mPriceAmountMicros = phaseJson.optLong("priceAmountMicros");
            mPriceCurrencyCode = phaseJson.optString("priceCurrencyCode");
            mFormattedPrice = phaseJson.optString("formattedPrice");
            mBillingPeriod = phaseJson.optString("billingPeriod");
            mRecurrenceMode = phaseJson.optInt("recurrenceMode");
            mBillingCycleCount = phaseJson.optInt("billingCycleCount");
        }

        public long getPriceAmountMicros() { return mPriceAmountMicros; }
        public String getPriceCurrencyCode() { return mPriceCurrencyCode; }
        public String getFormattedPrice() { return mFormattedPrice; }
        public String getBillingPeriod() { return mBillingPeriod; }
        public int getRecurrenceMode() { return mRecurrenceMode; }
        public int getBillingCycleCount() { return mBillingCycleCount; }
    }
}