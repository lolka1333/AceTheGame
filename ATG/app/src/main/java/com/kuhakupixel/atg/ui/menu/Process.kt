package com.kuhakupixel.atg.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuhakupixel.atg.R
import com.kuhakupixel.atg.backend.ACE
import com.kuhakupixel.atg.backend.ACEPort
import com.kuhakupixel.atg.backend.ProcInfo
import com.kuhakupixel.atg.ui.GlobalConf
import com.kuhakupixel.atg.ui.util.CreateTable
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import com.kuhakupixel.atg.ui.OverlayInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * which process we are currently attached to?
 * */
private var attachedStatusString: MutableState<String> = mutableStateOf("None")

/**
 * Process display modes
 */
enum class ProcessDisplayMode {
    ORIGINAL,     // Using ACE util_client (may truncate names)
    FULL_NAME,    // Full process names using ps -eo pid,comm  
    FULL_COMMAND  // Full command lines using ps -eo pid,args
}

/**
 * Current process display mode
 */
private var processDisplayMode: MutableState<ProcessDisplayMode> = mutableStateOf(ProcessDisplayMode.FULL_NAME)

private suspend fun AttachToProcess(
    ace: ACE?,
    pid: Long,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
    onAttachFailure: (msg: String) -> Unit,
) {
    if (ace == null) {
        onAttachFailure("ACE is not initialized")
        return
    }

    try {
        // Perform all heavy operations in background thread
        withContext(Dispatchers.IO) {
            android.util.Log.d("ATG", "Starting attach process for PID: $pid")
            
            // check if its still alive
            if (!ace.IsPidRunning(pid)) {
                withContext(Dispatchers.Main) {
                    onProcessNoExistAnymore()
                }
                return@withContext
            }
            
            // DeAttach first if we have been attached previously
            if (ace.IsAttached()) {
                try {
                    android.util.Log.d("ATG", "Detaching from previous process before reattach")
                    ace.DeAttach()
                } catch (e: Exception) {
                    android.util.Log.w("ATG", "Normal detach failed, using force detach: ${e.message}")
                    try {
                        ace.ForceDetach()
                    } catch (e2: Exception) {
                        android.util.Log.e("ATG", "Force detach also failed: ${e2.message}")
                        // Continue anyway - the attach process will handle cleanup
                    }
                }
            }
            
            // attach - this is the heavy operation that can take time
            try {
                android.util.Log.d("ATG", "Attaching to process $pid...")
                ace.Attach(pid)
                android.util.Log.d("ATG", "Successfully attached to process $pid")
            } catch (e: Exception) {
                android.util.Log.e("ATG", "Failed to attach to process $pid: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onAttachFailure("Failed to attach to process $pid: ${e.message}")
                }
                return@withContext
            }
            
            // Verify attachment
            var attachedPid: Long = -1
            try {
                attachedPid = ace.GetAttachedPid()
                android.util.Log.d("ATG", "Verified attachment to PID: $attachedPid")
            } catch (e: Exception) {
                android.util.Log.e("ATG", "Unable to verify attachment to process $pid: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onAttachFailure("Unable to verify attachment to process $pid: ${e.message}")
                }
                return@withContext
            }
            
            // final check to see if we are attached
            // to the correct process
            if (attachedPid == pid) {
                withContext(Dispatchers.Main) {
                    onAttachSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onAttachFailure("Attachment verification failed: expected PID $pid, but got $attachedPid")
                }
            }
        }
        
    } catch (e: Exception) {
        // Catch any unexpected exceptions
        android.util.Log.e("ATG", "Unexpected error in AttachToProcess: ${e.message}", e)
        onAttachFailure("Unexpected error: ${e.message}")
    }
}

@Composable
fun ProcessTable(
    processList: SnapshotStateList<ProcInfo>,
    onProcessSelected: (pid: Long, procName: String) -> Unit,
) {


    CreateTable(modifier = Modifier.padding(16.dp),
        colNames = listOf("Pid", "Name"),
        colWeights = listOf(0.2f, 0.8f),
        itemCount = processList.size,
        minEmptyItemCount = 50,
        onRowClicked = { rowIndex: Int ->
            onProcessSelected(
                processList[rowIndex].GetPidStr().toLong(),
                processList[rowIndex].GetName(),
            )

        },
        drawCell = { rowIndex: Int, colIndex: Int ->
            if (colIndex == 0) {
                Text(text = processList[rowIndex].GetPidStr())
            }
            if (colIndex == 1) {
                Text(
                    text = processList[rowIndex].GetName(),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
            }
        })
}


@Composable
private fun _ProcessMenuContent(
    runningProcState: SnapshotStateList<ProcInfo>,
    onRefreshClicked: () -> Unit,
    onConnectToACEServerClicked: () -> Unit,
    onAttach: (pid: Long, procName: String) -> Unit,
    buttonContainer: @Composable (
        content: @Composable () -> Unit
    ) -> Unit

) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Text("Selected process: ${attachedStatusString.value}")
        Text("Display mode: ${
            when (processDisplayMode.value) {
                ProcessDisplayMode.ORIGINAL -> "Original"
                ProcessDisplayMode.FULL_NAME -> "Full Name"
                ProcessDisplayMode.FULL_COMMAND -> "Full Command"
            }
        }")
    }
    buttonContainer {

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Text("Selected process: ${attachedStatusString.value}")
        }
        
        Button(
            onClick = { 
                // Cycle through display modes
                processDisplayMode.value = when (processDisplayMode.value) {
                    ProcessDisplayMode.ORIGINAL -> ProcessDisplayMode.FULL_NAME
                    ProcessDisplayMode.FULL_NAME -> ProcessDisplayMode.FULL_COMMAND
                    ProcessDisplayMode.FULL_COMMAND -> ProcessDisplayMode.ORIGINAL
                }
                onRefreshClicked() // Refresh the list with new mode
            },
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Text(
                text = when (processDisplayMode.value) {
                    ProcessDisplayMode.ORIGINAL -> "Mode: Original"
                    ProcessDisplayMode.FULL_NAME -> "Mode: Full Name"
                    ProcessDisplayMode.FULL_COMMAND -> "Mode: Full Command"
                }
            )
        }
        
        Button(onClick = onRefreshClicked, modifier = Modifier.padding(start = 10.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = "Refresh",
            )
        }

        Button(
            onClick = onConnectToACEServerClicked,
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Text("Connect to ACE Server")
        }
    }


    ProcessTable(
        processList = runningProcState,
        onProcessSelected = onAttach,
    )

}

@Composable
private fun _ProcessMenu(
    runningProcState: SnapshotStateList<ProcInfo>,
    onRefreshClicked: () -> Unit,
    onConnectToACEServerClicked: () -> Unit,
    onAttach: (pid: Long, procName: String) -> Unit,


    ) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                _ProcessMenuContent(
                    runningProcState = runningProcState,
                    onRefreshClicked = onRefreshClicked,
                    onConnectToACEServerClicked = onConnectToACEServerClicked,
                    onAttach = onAttach,
                    buttonContainer = { content ->
                        Row(content = { content() })
                    }
                )
            }

        } else {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                _ProcessMenuContent(
                    runningProcState = runningProcState,
                    onRefreshClicked = onRefreshClicked,
                    onConnectToACEServerClicked = onConnectToACEServerClicked,
                    onAttach = onAttach,
                    buttonContainer = { content ->
                        Column(content = { content() })
                    }
                )
            }

        }
    }
}


fun refreshProcList(ace: ACE?, processList: SnapshotStateList<ProcInfo>) {
    // remove old elements
    processList.clear()
    
    if (ace == null) {
        android.util.Log.e("ATG", "ACE is null in refreshProcList")
        return
    }
    
    try {
        // grab new one and add to the list using the selected display mode
        val runningProcs: List<ProcInfo>? = when (processDisplayMode.value) {
            ProcessDisplayMode.ORIGINAL -> ace.ListRunningProc()
            ProcessDisplayMode.FULL_NAME -> ace.ListRunningProcFull()
            ProcessDisplayMode.FULL_COMMAND -> ace.ListRunningProcWithArgs()
        }
        
        if (runningProcs != null && runningProcs.isNotEmpty()) {
            for (proc in runningProcs) {
                processList.add(proc)
            }
            android.util.Log.d("ATG", "Successfully loaded ${runningProcs.size} processes")
        } else {
            android.util.Log.w("ATG", "No processes found or null result from process list")
        }
    } catch (e: Exception) {
        android.util.Log.e("ATG", "Error refreshing process list: ${e.message}", e)
        // Try to fallback to original method if we were using an alternative mode
        if (processDisplayMode.value != ProcessDisplayMode.ORIGINAL) {
            try {
                android.util.Log.i("ATG", "Trying fallback to original process listing method")
                val fallbackProcs = ace.ListRunningProc()
                if (fallbackProcs != null) {
                    for (proc in fallbackProcs) {
                        processList.add(proc)
                    }
                    android.util.Log.d("ATG", "Fallback successful, loaded ${fallbackProcs.size} processes")
                }
            } catch (e2: Exception) {
                android.util.Log.e("ATG", "Fallback also failed: ${e2.message}", e2)
            }
        }
    }
}

@Composable
fun ProcessMenu(globalConf: GlobalConf?, overlayContext: OverlayContext?) {
    val ace: ACE = globalConf?.getAce()!!
    // list of processes that are gonna be shown
    val currentProcList = remember { SnapshotStateList<ProcInfo>() }
    val coroutineScope = rememberCoroutineScope()
    
    // State for tracking if attach is in progress
    val isAttachInProgress = remember { mutableStateOf(false) }
    
    // initialize the list first
    refreshProcList(ace, currentProcList)
    //
    _ProcessMenu(
        currentProcList,
        onAttach = { pid: Long, procName: String ->
            // Check if attach is already in progress
            if (isAttachInProgress.value) {
                OverlayInfoDialog(overlayContext!!).show(
                    title = "Attach already in progress",
                    text = "Please wait for the current attach operation to complete.",
                    onConfirm = {},
                )
                return@_ProcessMenu
            }
            
            OverlayInfoDialog(overlayContext!!).show(
                title = "Attach to ${pid} - ${procName} ? ", text = "",
                onConfirm = {
                    // Set attach in progress flag
                    isAttachInProgress.value = true
                    
                    // Show immediate status update
                    attachedStatusString.value = "Attaching to ${pid} - ${procName}..."
                    
                    // Launch attach operation in background with timeout
                    coroutineScope.launch {
                        try {
                            // Set timeout to 30 seconds for attach operation
                            val result = withTimeoutOrNull(30000) {
                                AttachToProcess(
                                    ace = ace, pid = pid,
                                    onAttachSuccess = {
                                        // Show success dialog
                                        OverlayInfoDialog(overlayContext).show(
                                            title = "Attaching to ${procName} is successful",
                                            onConfirm = {},
                                            text = "",
                                        )
                                        attachedStatusString.value = "${pid} - ${procName}"
                                        isAttachInProgress.value = false
                                    },
                                    onProcessNoExistAnymore = {
                                        OverlayInfoDialog(overlayContext).show(
                                            title = "Process ${procName} is not running anymore, Can't attach",
                                            onConfirm = {},
                                            text = "",
                                        )
                                        attachedStatusString.value = "None"
                                        isAttachInProgress.value = false
                                    },
                                    onAttachFailure = { msg: String ->
                                        OverlayInfoDialog(overlayContext).show(
                                            title = "Attach Failed",
                                            text = msg,
                                            onConfirm = {},
                                        )
                                        attachedStatusString.value = "None"
                                        isAttachInProgress.value = false
                                    },
                                )
                            }
                            
                            // Check if operation timed out
                            if (result == null) {
                                android.util.Log.e("ATG", "Attach operation timed out after 30 seconds")
                                OverlayInfoDialog(overlayContext).show(
                                    title = "Attach Timeout",
                                    text = "The attach operation timed out after 30 seconds. The process may be unresponsive or the system may be overloaded.",
                                    onConfirm = {},
                                )
                                attachedStatusString.value = "None"
                                isAttachInProgress.value = false
                            }
                        } catch (e: Exception) {
                            // Handle any coroutine exceptions
                            android.util.Log.e("ATG", "Coroutine exception in attach: ${e.message}", e)
                            OverlayInfoDialog(overlayContext).show(
                                title = "Attach Failed",
                                text = "Unexpected error: ${e.message}",
                                onConfirm = {},
                            )
                            attachedStatusString.value = "None"
                            isAttachInProgress.value = false
                        }
                    }
                },
            )
        },
        onRefreshClicked = { refreshProcList(ace, currentProcList) },
        onConnectToACEServerClicked = {

            OverlayInputDialog(overlayContext!!).show(
                "Port: ",
                defaultValue = ACEPort.defaultPort.toString(),
                onConfirm = { input: String ->
                    val port = input.toInt()
                    if (ace.IsAttached())
                        ace.DeAttach()
                    ace.ConnectToACEServer(port,ACEPort.defaultStatusPublisherPort )
                    attachedStatusString.value = "${ace.GetAttachedPid()} - ACE's Engine Server"
                },
            )

        },
    )

}

@Preview(showBackground = true)
@Composable
fun PreviewTable() {
    val myList: SnapshotStateList<ProcInfo> = remember {

        mutableStateListOf(
            ProcInfo("1 init"),
            ProcInfo("2 systemd"),
            ProcInfo("3 daemonSomething"),
        )

    }
    ProcessTable(
        myList,
        { pid: Long, procName: String ->

            // do nothing

        },
    )
}

@Composable
@Preview
fun ProcessMenuPreview() {
    ProcessMenu(null, null)
}