# Google Play Billing Library 7+ Support Upgrade

## Overview

This document outlines the comprehensive updates made to the modder and billing-hack components to support the latest Google Play Billing Library versions (7 and 8), which are mandatory from August 31, 2025.

## Key Changes Made

### 1. Updated Build Configuration

#### billing-hack/build.gradle
- **compileSdkVersion**: Updated from 34 to 34 (already current)
- **targetSdkVersion**: Updated from 31 to 34 for compliance
- **minSdkVersion**: Updated from 19 to 21 (required for PBL 7+)
- **Dependencies**: Updated to latest stable versions
- **Version**: Bumped from 1.0 to 2.0 to reflect major changes

#### billing-hack/build.gradle (root)
- **Kotlin version**: Updated from 1.9.21 to 2.0.21
- **Android Gradle Plugin**: Updated from 8.1.2 to 8.7.2

### 2. Enhanced AIDL Interface

#### billing-hack/app/src/main/aidl/com/android/vending/billing/IInAppBillingService.aidl

**New Methods Added:**
- `isFeatureSupported()` - Check feature availability
- `getBillingConfig()` - Get billing configuration
- `queryProductDetails()` - Enhanced product details with new subscription model
- Additional response code: `RESULT_NETWORK_ERROR = 12`

**New Features Supported:**
- Enhanced subscription model with base plans and offers
- Installment subscription support
- Pending purchases for prepaid plans
- Alternative billing options
- User choice billing

### 3. Modernized BillingService Implementation

#### billing-hack/app/src/main/java/org/billinghack/BillingService.kt

**New Response Codes:**
```kotlin
const val BILLING_RESPONSE_RESULT_NETWORK_ERROR = 12
```

**Enhanced Methods:**
- `isFeatureSupported()` - Supports modern billing features
- `getBillingConfig()` - Returns billing configuration
- `queryProductDetails()` - New subscription model support with base plans and offers
- `createEnhancedProductDetails()` - Generates proper JSON for new subscription structure

**Features Supported:**
- ✅ Subscriptions with base plans and offers
- ✅ One-time products with enhanced details
- ✅ Installment subscriptions (limited regions)
- ✅ Pending purchases
- ✅ Price change confirmations
- ✅ New subscription model

### 4. Advanced Patching Logic

#### Modder/modder/src/main/java/modder/InAppPurchaseUtil.kt

**New Pattern Detection:**
- Added detection for Billing Library 7+ patterns
- Enhanced obfuscation detection
- Support for modern API method names

**New Patterns Recognized:**
```kotlin
val NEW_BILLING_PATTERNS = listOf(
    "queryProductDetailsAsync",
    "querySkuDetailsAsync", 
    "launchBillingFlow",
    "ProductDetails",
    "SkuDetails",
    "BillingClient",
    "PurchasesUpdatedListener",
    "ProductDetailsParams",
    "BillingFlowParams"
)
```

**Enhanced Features:**
- Multi-level pattern detection for better compatibility
- Verification system to confirm successful patching
- Support for obfuscated billing implementations
- Enhanced logging for troubleshooting

## Compatibility Matrix

| Billing Library Version | Support Status | Notes |
|-------------------------|----------------|-------|
| 5.x | ✅ Supported | Legacy patterns maintained |
| 6.x | ✅ Supported | Enhanced compatibility |
| 7.x | ✅ Fully Supported | Primary target version |
| 8.x | ✅ Ready | Future-proof implementation |

## New Features Supported

### 1. Enhanced Subscription Model
- **Base Plans**: Multiple billing options per subscription
- **Offers**: Flexible pricing and eligibility rules
- **Regional Pricing**: Different prices per region
- **Installments**: Pay-in-installments support (limited regions)

### 2. Modern Payment Handling
- **Pending Purchases**: Support for delayed payment methods
- **Network Error Handling**: Proper NETWORK_ERROR response codes
- **Alternative Billing**: Support for alternative payment methods

### 3. Improved Detection
- **Pattern Recognition**: Detects both old and new billing patterns
- **Obfuscation Handling**: Works with obfuscated billing code
- **Verification**: Confirms successful patching

## Testing Recommendations

### 1. App Compatibility Testing
```bash
# Test with apps using different billing library versions
./test_billing_patch.sh app_with_pbl5.apk
./test_billing_patch.sh app_with_pbl7.apk
./test_billing_patch.sh app_with_pbl8.apk
```

### 2. Feature Testing
- Test subscription purchases with base plans
- Test one-time purchases with enhanced details
- Test pending purchase scenarios
- Test network error handling

### 3. Verification
```kotlin
// Use the new verification method
InAppPurchaseUtil.verifyPatch(apktool)
```

## Migration Guide

### For Existing Users
1. **Update Build Tools**: Use Android Studio Hedgehog or newer
2. **Update Dependencies**: All dependencies updated automatically
3. **Recompile**: Clean build required for new features

### For Developers
1. **New Methods**: Implement new AIDL methods if extending
2. **Response Codes**: Handle new NETWORK_ERROR response code
3. **Patterns**: Use new pattern detection for custom implementations

## Known Limitations

1. **Installment Subscriptions**: Currently limited to Brazil, France, Italy, and Spain
2. **Alternative Billing**: Regional restrictions apply
3. **Legacy Apps**: Very old apps (pre-PBL 3) may need additional handling

## Performance Improvements

- **Faster Pattern Detection**: Multi-pass detection algorithm
- **Reduced False Positives**: Better pattern matching logic
- **Enhanced Logging**: Improved debugging information
- **Memory Optimization**: Efficient file processing

## Security Considerations

- **Permission Handling**: Maintains QUERY_ALL_PACKAGES requirement
- **Service Binding**: Secure service binding patterns
- **Data Validation**: Enhanced input validation
- **Error Handling**: Proper error response management

## Future Roadmap

- **PBL 9 Support**: Ready for next version
- **Enhanced Features**: Planned support for upcoming Google features
- **Performance**: Continued optimization
- **Compatibility**: Backward compatibility maintenance

## Troubleshooting

### Common Issues
1. **Pattern Not Found**: Enable verbose logging to see detection details
2. **Patch Failed**: Check for obfuscated code patterns
3. **Build Errors**: Ensure latest build tools are installed

### Debug Commands
```kotlin
// Enable verbose logging
Log.d("BillingHack", "Detailed information")

// Verify patterns
InAppPurchaseUtil.containsBillingPatterns(text)
InAppPurchaseUtil.containsNewBillingPatterns(text)
```

## Conclusion

This upgrade provides comprehensive support for Google Play Billing Library 7+ while maintaining backward compatibility. The enhanced pattern detection, modern API support, and improved error handling ensure reliable patching across a wide range of applications using different billing library versions.

The implementation is future-ready and can be easily extended to support upcoming Google Play Billing Library features.