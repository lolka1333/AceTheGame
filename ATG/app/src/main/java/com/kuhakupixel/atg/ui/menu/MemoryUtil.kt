package com.kuhakupixel.atg.ui.menu

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kuhakupixel.atg.backend.ACE
import com.kuhakupixel.atg.backend.ACEBaseClient
import com.kuhakupixel.atg.backend.ACEStatusSubscriber
import com.kuhakupixel.atg.backend.ScanProgressData
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

class ScanOptions(
    val inputVal: String,
    val numType: ACE.NumType,
    val scanType: ACE.Operator,
    val regionLevel: ACE.RegionLevel,
    val initialScanDone: Boolean,
) {
}

fun onNextScanClicked(
    scanOptions: ScanOptions,
    ace: ACE,
    onBeforeScanStart: () -> Unit,
    onScanDone: () -> Unit,
    onScanProgress: (progress: Float) -> Unit,
    onScanError: (e: Exception) -> Unit,
) {
    onBeforeScanStart()
    
    // Check if we have a client connection (don't check responsiveness for initial check as server might be busy)
    if (!ace.HasClient()) {
        onScanError(Exception("Not attached to any process. Please attach to a process first."))
        onScanDone()
        return
    }
    
    // For intensive operations like all_read_write, the server might be temporarily unresponsive
    // so we'll skip the initial responsiveness check and let the actual scan operation determine connectivity
    val isIntensiveOperation = scanOptions.regionLevel == ACE.RegionLevel.all_read_write || 
                               scanOptions.regionLevel == ACE.RegionLevel.all
                               
    if (!isIntensiveOperation && !ace.IsServerResponsive()) {
        onScanError(Exception("ACE server is not responding. The target process may have crashed or the connection was lost. Please try reattaching to the process."))
        onScanDone()
        return
    }
    
    if (isIntensiveOperation) {
        Log.i("ATG", "Starting intensive memory operation (${scanOptions.regionLevel}), skipping initial responsiveness check")
    }
    
    val statusPublisherPort = ace.getStatusPublisherPort()
    Log.i("ATG", "Starting scan with options: type=${scanOptions.numType}, operator=${scanOptions.scanType}, value='${scanOptions.inputVal}', region=${scanOptions.regionLevel}")
    
    CompletableFuture.supplyAsync<Unit> {
        try {
            // set the value type
            if (!scanOptions.initialScanDone) {
                Log.d("ATG", "Setting scan configuration: type=${scanOptions.numType}, region=${scanOptions.regionLevel}")
                ace.SetNumType(scanOptions.numType)
                ace.SetRegionLevel(scanOptions.regionLevel)
            }
            
            /**
             * scan against a value if input value
             * is not empty
             * and scan without value otherwise
             * (picking addresses whose value stayed the same, increased and etc)
             * */

            if (scanOptions.inputVal.isBlank()) {
                Log.d("ATG", "Performing scan without value (filter scan) with operator: ${scanOptions.scanType}")
                ace.ScanWithoutValue(scanOptions.scanType)
            } else {
                Log.d("ATG", "Performing scan against value: '${scanOptions.inputVal}' with operator: ${scanOptions.scanType}")
                ace.ScanAgainstValue(
                    scanOptions.scanType,
                    scanOptions.inputVal
                )
            }
            
            Log.i("ATG", "Scan completed successfully")
            onScanDone()
            
        } catch (e: Exception) {
            Log.e("ATG", "Error during scan execution: ${e.message}", e)
            
            // Provide more detailed error messages based on the exception
            val userMessage = when {
                e.message?.contains("Intensive memory scan failed") == true -> 
                    "All Read/Write scan failed: This operation scans all memory regions and may be too resource-intensive for this device or take too long. Try using a more specific region level like 'heap' or 'stack' instead."
                e.message?.contains("all_read_write scan operation timed out") == true -> 
                    "All Read/Write scan timed out: This operation scans all memory regions and took too long to complete. Try using a more specific region level like 'heap' or 'stack' for better performance."
                e.message?.contains("Failed to communicate with ACE server") == true -> 
                    "Lost connection to the target process. The process may have crashed or been terminated. Please reattach to continue."
                e.message?.contains("ACE server not responding") == true -> 
                    "The target process is not responding. It may be frozen or the connection was lost. Please try reattaching."
                e.message?.contains("ACE server connection is not alive") == true -> 
                    "Connection to the target process was lost. Please reattach to the process."
                e.message?.contains("Operation requires attaching") == true -> 
                    "Not attached to any process. Please attach to a process first."
                else -> 
                    "Scan failed: ${e.message ?: "Unknown error"}"
            }
            
            onScanError(Exception(userMessage, e))
            onScanDone()
        }
    }.exceptionally { e ->
        Log.e("ATG", "CompletableFuture exception during scan: ${e.message}", e)
        
        // Extract the root cause from CompletionException
        val rootCause = e.cause ?: e
        val userMessage = when {
            rootCause.message?.contains("Intensive memory scan failed") == true -> 
                "All Read/Write scan failed: This operation scans all memory regions and may be too resource-intensive for this device or take too long. Try using a more specific region level like 'heap' or 'stack' instead."
            rootCause.message?.contains("all_read_write scan operation timed out") == true -> 
                "All Read/Write scan timed out: This operation scans all memory regions and took too long to complete. Try using a more specific region level like 'heap' or 'stack' for better performance."
            rootCause.message?.contains("Failed to communicate with ACE server") == true -> 
                "Lost connection to the target process. The process may have crashed or been terminated. Please reattach to continue."
            rootCause.message?.contains("ACE server not responding") == true -> 
                "The target process is not responding. It may be frozen or the connection was lost. Please try reattaching."
            else -> 
                "Scan failed: ${rootCause.message ?: "Unknown error"}"
        }
        
        onScanError(Exception(userMessage, rootCause))
        onScanDone()
        null
    }

    /**
     * thread to update the progress as the scan goes, with a subscriber
     * that keeps listening to a port until the scan is done
     * */
    thread {
        try {
            val statusSubscriber = ACEStatusSubscriber(statusPublisherPort!!)
            statusSubscriber.use { it: ACEStatusSubscriber ->

                var scanProgressData: ScanProgressData =
                    statusSubscriber.GetScanProgress()
                while (!scanProgressData.is_finished) {
                    onScanProgress(scanProgressData.current.toFloat() / scanProgressData.max.toFloat())
                    scanProgressData = statusSubscriber.GetScanProgress()
                }
                // finished
                onScanProgress(0.0f)
            }

        } catch (e: Exception) {
            Log.e("ATG", "Error " + e.toString())

        }

    }


}