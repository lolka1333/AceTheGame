package modder

import apktool.kotlin.lib.Apktool
import java.io.File

class InAppPurchaseUtil {
    companion object {
        val ORIGINAL_BILLING_CLIENT_FOLDER_PATH = "com/android/billingclient/api/"
        val ORIGINAL_PACKAGE_NAME = "\"com.android.vending\""
        val ORIGINAL_SERVICE_TO_CONNECT_TO_NAME = "\"com.android.vending.billing.InAppBillingService.BIND\""

        val BILLING_HACK_PACKAGE_NAME: String = "\"org.billinghack\""
        val BILLING_HACK_SERVICE_TO_CONNECT_TO_NAME = "\"org.billinghack.BillingService.BIND\""

        val PERMISSION_QUERY_ALL_PACKAGE: String = "android.permission.QUERY_ALL_PACKAGES"

        // New patterns for Billing Library 7+
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

        // Additional service patterns for newer billing libraries
        val BILLING_SERVICE_PATTERNS = listOf(
            "com.android.vending.billing.IInAppBillingService",
            "InAppBillingService.BIND",
            "\"com.android.vending\"",
            "BillingClient"
        )

        fun patchStringContent(content: String, redirectToLuckyPatcher: Boolean = false): String {
            // replace the string
            var newPackageName = ""

            val originalServiceToConnectToName = "\"com.android.vending.billing.InAppBillingService.BIND\""
            var newServiceToConnectToName = ""
            if (redirectToLuckyPatcher) {
                newPackageName = "\"com.android.vending.billing.InAppBillingService.BINN\""
                newServiceToConnectToName = "\"com.android.vending.billing.InAppBillingService.BINN\""
            } else {
                // redirect purchases to our own
                newPackageName = BILLING_HACK_PACKAGE_NAME
                newServiceToConnectToName = BILLING_HACK_SERVICE_TO_CONNECT_TO_NAME
            }

            var newText = content.replace(ORIGINAL_PACKAGE_NAME, newPackageName)
            newText = newText.replace(originalServiceToConnectToName, newServiceToConnectToName)
            
            // Additional patterns for newer Billing Library versions
            newText = newText.replace("\"com.android.vending.billing.InAppBillingService.BIND\"", newServiceToConnectToName)
            newText = newText.replace("com.android.vending.billing.InAppBillingService.BIND", newServiceToConnectToName.replace("\"", ""))
            
            return newText
        }

        fun findBillingClientEntryFile(apktool: Apktool): File? {
            var billingClientPurchaseFile: File? = null
            val decompiledFiles: Array<File> = apktool.decompilationFolder!!.listFiles()!!
            
            // ============== begin the patch process ================
            // make sure we found billing client library
            apktool.IterateSmaliClassesFolder {
                val currentFolder = File(it.toString(), ORIGINAL_BILLING_CLIENT_FOLDER_PATH)
                // make sure folder exists
                if (currentFolder.exists()) {
                    val billingClientFiles: Array<File> = currentFolder!!.listFiles()!!
                    // for folder, find the exact and replace
                    for (f in billingClientFiles) {
                        val text: String = f.readText()
                        // Check for both old and new patterns
                        if (containsBillingPatterns(text)) {
                            billingClientPurchaseFile = f
                            println("Found billing file: ${f.name}")
                            // exit early
                            return@IterateSmaliClassesFolder
                        }
                    }
                }
            }

            // still haven't found file to patch in default location
            // try all files to check all files in case the name is obfuscated
            if (billingClientPurchaseFile == null) {
                println("${ORIGINAL_BILLING_CLIENT_FOLDER_PATH} not found, billing client seems to be obfuscated, trying another way ... ")
                apktool.IterateSmaliClassesFolder {
                    File(it.toString()).walkTopDown().forEach { f: File ->
                        if (f.isFile && f.extension == "smali") {
                            val text: String = f.readText()
                            if (containsBillingPatterns(text)) {
                                billingClientPurchaseFile = f
                                println("Found obfuscated billing file: ${f.name}")
                                // exit early
                                return@IterateSmaliClassesFolder
                            }
                        }
                    }
                }
            }
            
            // Additional search for newer billing library patterns
            if (billingClientPurchaseFile == null) {
                println("Searching for newer billing library patterns...")
                apktool.IterateSmaliClassesFolder {
                    File(it.toString()).walkTopDown().forEach { f: File ->
                        if (f.isFile && f.extension == "smali") {
                            val text: String = f.readText()
                            if (containsNewBillingPatterns(text)) {
                                billingClientPurchaseFile = f
                                println("Found new billing library pattern in: ${f.name}")
                                return@IterateSmaliClassesFolder
                            }
                        }
                    }
                }
            }
            
            return billingClientPurchaseFile
        }

        private fun containsBillingPatterns(text: String): Boolean {
            // Check for traditional patterns
            val hasTraditionalPatterns = text.contains(ORIGINAL_PACKAGE_NAME) && 
                                       text.contains(ORIGINAL_SERVICE_TO_CONNECT_TO_NAME)
            
            if (hasTraditionalPatterns) return true
            
            // Check for additional service patterns
            val hasServicePatterns = BILLING_SERVICE_PATTERNS.any { pattern ->
                text.contains(pattern)
            }
            
            // Check for at least 2 billing-related patterns to reduce false positives
            val billingPatternCount = BILLING_SERVICE_PATTERNS.count { pattern ->
                text.contains(pattern)
            }
            
            return hasServicePatterns && billingPatternCount >= 2
        }

        private fun containsNewBillingPatterns(text: String): Boolean {
            // Check for new billing library patterns (need at least 2 to reduce false positives)
            val patternCount = NEW_BILLING_PATTERNS.count { pattern ->
                text.contains(pattern)
            }
            
            // Also check for service binding patterns which are essential
            val hasServiceBinding = text.contains("InAppBillingService") || 
                                  text.contains("BillingClient") ||
                                  text.contains("com.android.vending")
            
            return patternCount >= 2 && hasServiceBinding
        }

        fun patchApk(apktool: Apktool, redirectToLuckyPatcher: Boolean = false): Boolean {
            val billingClientEntryFile = findBillingClientEntryFile(apktool)
            if (billingClientEntryFile == null) {
                println("No billing client entry file found. This app might not use Google Play Billing.")
                return false
            }
            
            println("Patching billing file: ${billingClientEntryFile.absolutePath}")
            val text: String = billingClientEntryFile.readText()
            var newText = patchStringContent(text, redirectToLuckyPatcher)
            
            // Additional patching for newer billing library versions
            newText = patchEnhancedBillingContent(newText, redirectToLuckyPatcher)
            
            // write back changes when successful
            if (text != newText) {
                println("Writing patched content to ${billingClientEntryFile.absolutePath}")
                billingClientEntryFile.printWriter().use { out ->
                    out.print(newText)
                }
                
                println("Successfully patched billing implementation")
            } else {
                println("No changes made - file might already be patched or use different patterns")
                return false
            }

            println("Injecting permission")
            // inject permission so our patched apk can make requests to purchase server
            // https://stackoverflow.com/questions/17316232/how-to-start-android-service-from-another-android-app
            // https://stackoverflow.com/questions/65629268/queryintentactivities-returning-empty-list-in-android-11
            apktool.injectPermissionName(PERMISSION_QUERY_ALL_PACKAGE)
            return true
        }

        private fun patchEnhancedBillingContent(content: String, redirectToLuckyPatcher: Boolean): String {
            var newText = content
            
            // Additional patterns for newer billing libraries
            val additionalPatterns = mapOf(
                "\"com.android.vending\"" to if (redirectToLuckyPatcher) "\"com.android.vending.billing.InAppBillingService.BINN\"" else BILLING_HACK_PACKAGE_NAME,
                "Lcom/android/vending/billing/IInAppBillingService" to if (redirectToLuckyPatcher) "Lcom/android/vending/billing/IInAppBillingService" else "Lorg/billinghack/BillingService",
                "com.android.vending.billing.IInAppBillingService.BIND" to if (redirectToLuckyPatcher) "com.android.vending.billing.InAppBillingService.BINN" else "org.billinghack.BillingService.BIND"
            )
            
            for ((oldPattern, newPattern) in additionalPatterns) {
                if (newText.contains(oldPattern)) {
                    newText = newText.replace(oldPattern, newPattern)
                    println("Replaced pattern: $oldPattern -> $newPattern")
                }
            }
            
            return newText
        }

        fun verifyPatch(apktool: Apktool): Boolean {
            println("Verifying patch...")
            var foundPatchedFiles = 0
            
            apktool.IterateSmaliClassesFolder {
                File(it.toString()).walkTopDown().forEach { f: File ->
                    if (f.isFile && f.extension == "smali") {
                        val text: String = f.readText()
                        if (text.contains(BILLING_HACK_PACKAGE_NAME) || text.contains(BILLING_HACK_SERVICE_TO_CONNECT_TO_NAME)) {
                            foundPatchedFiles++
                            println("Verified patch in: ${f.name}")
                        }
                    }
                }
            }
            
            val isPatched = foundPatchedFiles > 0
            println("Patch verification: ${if (isPatched) "SUCCESS" else "FAILED"} ($foundPatchedFiles files patched)")
            return isPatched
        }
    }
}