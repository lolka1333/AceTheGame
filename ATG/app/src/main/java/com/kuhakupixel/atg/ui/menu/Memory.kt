package com.kuhakupixel.atg.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuhakupixel.atg.backend.ACE
import com.kuhakupixel.atg.backend.ACE.MatchInfo
import com.kuhakupixel.atg.backend.ACE.NumType
import com.kuhakupixel.atg.backend.ACE.Operator
import com.kuhakupixel.atg.ui.GlobalConf
import com.kuhakupixel.atg.ui.util.CreateTable
import com.kuhakupixel.atg.ui.util.NumberInputField
import com.kuhakupixel.atg.ui.util.OverlayDropDown
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayChoicesDialog
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ColumnScrollbar
import kotlin.math.min


// ======================= drop down options =================
private var defaultValueInitialized: Boolean = false

// ==================== selected scan options ==============================
private var scanInputVal: MutableState<String> = mutableStateOf("")

private val scanTypeSelectedOptionIdx = mutableStateOf(0)
private val valueTypeSelectedOptionIdx = mutableStateOf(0)
private val regionLevelSelectedOptionIdx = mutableStateOf(0)

// ================================================================
private val initialScanDone: MutableState<Boolean> = mutableStateOf(false)
private val isScanOnGoing: MutableState<Boolean> = mutableStateOf(false)

private val valueTypeEnabled: MutableState<Boolean> = mutableStateOf(false)
private val regionLevelEnabled: MutableState<Boolean> = mutableStateOf(false)
private val scanTypeEnabled: MutableState<Boolean> = mutableStateOf(false)

// ===================================== current matches data =========================
private var currentMatchesList: MutableState<List<MatchInfo>> = mutableStateOf(mutableListOf())
private var matchesStatusText: MutableState<String> = mutableStateOf("0 matches")
private val scanProgress: MutableState<Float> = mutableStateOf(0.0f)

// ================================================================

fun getCurrentScanOption(): ScanOptions {

    return ScanOptions(
        inputVal = scanInputVal.value,
        numType = NumType.values()[valueTypeSelectedOptionIdx.value],
        scanType = Operator.values()[scanTypeSelectedOptionIdx.value],
        initialScanDone = initialScanDone.value,
        regionLevel = ACE.RegionLevel.values()[regionLevelSelectedOptionIdx.value]
    )
}

@Composable
fun MemoryMenu(globalConf: GlobalConf?, overlayContext: OverlayContext?) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        _MemoryMenu(
            globalConf = globalConf,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            overlayContext = overlayContext,
        )
    }
}


@Composable
fun _MemoryMenu(
    globalConf: GlobalConf?,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    overlayContext: OverlayContext?
) {
    val ace: ACE = (globalConf?.getAce())!!
    // ==================================
    // initialize default value for scanning options
    if (!defaultValueInitialized) {
        // init default
        valueTypeSelectedOptionIdx.value = NumType.values().indexOf(ATGSettings.defaultNumType)
        scanTypeSelectedOptionIdx.value = Operator.values().indexOf(ATGSettings.defaultScanType)
        regionLevelSelectedOptionIdx.value =
            ACE.RegionLevel.values().indexOf(ATGSettings.defaultRegionLevel)

        defaultValueInitialized = true
    }
    val isAttached: Boolean = ace.IsAttached()
    valueTypeEnabled.value = isAttached && !initialScanDone.value
    regionLevelEnabled.value = isAttached && !initialScanDone.value
    scanTypeEnabled.value = isAttached

    // =================================
    // for showing scan error
    // have to use this hacky solution because OverlayInfoDiaog.show
    // cannot be called in another thread
    val showErrorDialog = remember { mutableStateOf(false) }
    val errorDialogMsg = remember { mutableStateOf("") }
    if (showErrorDialog.value) {
        OverlayInfoDialog(overlayContext!!).show(
            title = "Error",
            text = errorDialogMsg.value,
            onConfirm = {
            },
            onClose = {

                showErrorDialog.value = false
            }

        )
    }


//
    val content: @Composable (matchesTableModifier: Modifier, matchesSettingModifier: Modifier) -> Unit =
        { matchesTableModifier, matchesSettingModifier ->

            MatchesTable(
                modifier = matchesTableModifier,
                matches = currentMatchesList.value,
                matchesStatusText = matchesStatusText.value,
                scanProgress = scanProgress.value,
                onMatchClicked = { matchInfo: MatchInfo ->
                    //
                    val valueType: NumType = getCurrentScanOption().numType
                    AddressTableAddAddress(matchInfo = matchInfo, numType = valueType)
                    //
                    coroutineScope.launch() {
                        snackbarHostState.showSnackbar(
                            message = "Added ${matchInfo.address} to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"

                        )
                    }
                },
                onCopyAllMatchesToAddressTable = {
                    val valueType: NumType = getCurrentScanOption().numType
                    for (matchInfo in currentMatchesList.value)
                        AddressTableAddAddress(matchInfo = matchInfo, numType = valueType)
                    coroutineScope.launch() {
                        snackbarHostState.showSnackbar(
                            message = "Added all matches to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"

                        )
                    }

                }
            )
            MatchesSetting(
                modifier = matchesSettingModifier,
                ace = ace,
                //
                scanTypeEnabled = scanTypeEnabled,
                scanTypeSelectedOptionIdx = scanTypeSelectedOptionIdx,

                //
                regionLevelEnabled = regionLevelEnabled,
                regionLevelSelectedOptionIdx = regionLevelSelectedOptionIdx,
                //
                scanInputVal = scanInputVal,
                // only allow to change Value type before any scan is done
                valueTypeEnabled = valueTypeEnabled,
                valueTypeSelectedOptionIdx = valueTypeSelectedOptionIdx,
                //
                nextScanEnabled = isAttached && !isScanOnGoing.value,
                nextScanClicked = fun() {
                    onNextScanClicked(
                        scanOptions = getCurrentScanOption(),
                        ace = ace,
                        onBeforeScanStart = {
                            // disable next and new scan
                            isScanOnGoing.value = true
                        },
                        onScanDone = {
                            isScanOnGoing.value = false
                            
                            // Only update matches if we're still connected
                            if (ace.IsAttached() && ace.IsServerResponsive()) {
                                // set initial scan to true
                                initialScanDone.value = true
                                // update matches table
                                UpdateMatches(ace = ace)
                                android.util.Log.d("ATG", "Scan completed successfully, matches updated")
                            } else {
                                android.util.Log.w("ATG", "Scan completed but server connection lost, skipping match update")
                                currentMatchesList.value = emptyList()
                                matchesStatusText.value = "Connection lost during scan"
                            }
                        },
                        onScanProgress = { progress: Float ->
                            scanProgress.value = progress
                        },
                        onScanError = { e: Exception ->
                            android.util.Log.e("ATG", "Scan error occurred: ${e.message}", e)
                            showErrorDialog.value = true
                            
                            // Provide user-friendly error messages based on the exception
                            errorDialogMsg.value = when {
                                e.message?.contains("Lost connection to the target process") == true -> 
                                    "Connection to the target process was lost. The process may have crashed or been terminated.\n\nPlease reattach to the process to continue."
                                e.message?.contains("ACE server is not responding") == true -> 
                                    "The target process is not responding. It may be frozen or the connection was lost.\n\nPlease try reattaching to the process."
                                e.message?.contains("Not attached to any process") == true -> 
                                    "You are not attached to any process.\n\nPlease attach to a process first before scanning."
                                e.message?.contains("Connection to the target process was lost") == true -> 
                                    "Connection to the target process was lost.\n\nPlease reattach to the process."
                                e.message?.contains("Failed to execute command") == true -> 
                                    "Communication with the target process failed.\n\nThe process may have crashed. Please try reattaching."
                                else -> 
                                    "Scan failed: ${e.message ?: "Unknown error"}\n\nPlease check the connection to the target process and try again."
                            }
                        }


                    )
                },

                //
                newScanEnabled = isAttached && initialScanDone.value && !isScanOnGoing.value,
                newScanClicked = {
                    try {
                        if (!ace.IsAttached()) {
                            android.util.Log.w("ATG", "Cannot start new scan: not attached to any process")
                            currentMatchesList.value = emptyList()
                            matchesStatusText.value = "Not attached to any process"
                        } else if (!ace.IsServerResponsive()) {
                            android.util.Log.w("ATG", "Cannot start new scan: ACE server is not responsive")
                            currentMatchesList.value = emptyList()
                            matchesStatusText.value = "Server not responding"
                        } else {
                            ace.ResetMatches()
                            UpdateMatches(ace = ace)
                            initialScanDone.value = false
                            android.util.Log.d("ATG", "New scan started successfully")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ATG", "Error starting new scan: ${e.message}", e)
                        currentMatchesList.value = emptyList()
                        matchesStatusText.value = "Error starting new scan: ${e.message ?: "Unknown error"}"
                    }
                },
                overlayContext = overlayContext!!,
            )

        }


// switch to column when portrait and row when landscape
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            content(
                matchesTableModifier = Modifier
                    .weight(0.6f)
                    .padding(16.dp),
                matchesSettingModifier = Modifier
                    .weight(0.4f)
                    .padding(10.dp),
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            content(
                matchesTableModifier = Modifier
                    .weight(0.6f)
                    .padding(16.dp),
                matchesSettingModifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun MatchesTable(
    modifier: Modifier = Modifier,
    matches: List<MatchInfo>,
    matchesStatusText: String,
    scanProgress: Float,
    onMatchClicked: (matchInfo: MatchInfo) -> Unit,
    onCopyAllMatchesToAddressTable: () -> Unit,
) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween
        ) {
            Text(matchesStatusText)

            Button(onClick = onCopyAllMatchesToAddressTable) {
                Icon(
                    Icons.Filled.ArrowForward,
                    "Copy all matches to address table",
                )
            }
        }
        LinearProgressIndicator(progress = scanProgress)
        CreateTable(colNames = listOf("Address", "Previous Value"),
            colWeights = listOf(0.4f, 0.6f),
            itemCount = matches.size,
            minEmptyItemCount = 50,
            onRowClicked = { rowIndex: Int ->
                onMatchClicked(matches[rowIndex])
            },
            drawCell = { rowIndex: Int, colIndex: Int ->
                if (colIndex == 0) {
                    Text(text = matches[rowIndex].address)
                }
                if (colIndex == 1) {
                    Text(text = matches[rowIndex].prevValue)
                }
            })
    }

}

private fun UpdateMatches(ace: ACE) {
    try {
        // Check if we're still attached and server is responsive
        if (!ace.IsAttached()) {
            android.util.Log.w("ATG", "Cannot update matches: not attached to any process")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Not attached to any process"
            return
        }
        
        if (!ace.IsServerResponsive()) {
            android.util.Log.w("ATG", "Cannot update matches: ACE server is not responsive")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Server not responding"
            return
        }
        
        val matchesCount: Int = ace.GetMatchCount()
        val shownMatchesCount: Int = min(matchesCount, ATGSettings.maxShownMatchesCount)
        
        // update ui
        currentMatchesList.value = ace.ListMatches(ATGSettings.maxShownMatchesCount)
        matchesStatusText.value = "$matchesCount matches (showing ${shownMatchesCount})"
        
        android.util.Log.d("ATG", "Successfully updated matches: $matchesCount total, showing $shownMatchesCount")
        
    } catch (e: Exception) {
        android.util.Log.e("ATG", "Error updating matches: ${e.message}", e)
        currentMatchesList.value = emptyList()
        matchesStatusText.value = "Error getting matches: ${e.message ?: "Unknown error"}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchesSetting(
    modifier: Modifier = Modifier,
    ace: ACE,
    //
    scanTypeEnabled: MutableState<Boolean>,
    scanTypeSelectedOptionIdx: MutableState<Int>,
    //
    regionLevelEnabled: MutableState<Boolean>,
    regionLevelSelectedOptionIdx: MutableState<Int>,
    //
    scanInputVal: MutableState<String>,
    //
    valueTypeEnabled: MutableState<Boolean>,
    valueTypeSelectedOptionIdx: MutableState<Int>,
    //
    nextScanEnabled: Boolean,
    nextScanClicked: () -> Unit,
    //
    newScanEnabled: Boolean,
    newScanClicked: () -> Unit,
    overlayContext: OverlayContext,
) {
    @Composable
    fun ScanInputField(scanValue: MutableState<String>) {
        NumberInputField(
            value = scanValue.value,
            onValueChange = { value ->
                scanValue.value = value
            },
            label = "Scan For",
            placeholder = "value ...",
        )
    }

    @Composable
    fun ScanButton(
        modifier: Modifier = Modifier,
        nextScanEnabled: Boolean,
        newScanEnabled: Boolean,
        onNextScan: () -> Unit,
        onNewScan: () -> Unit
    ) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(enabled = newScanEnabled, onClick = onNewScan) {
                Text("New Scan")
            }
            Button(enabled = nextScanEnabled, onClick = onNextScan) {
                Text("Next Scan")
            }
        }

    }

    @Composable
    fun ScanTypeDropDown(
        selectedOptionIndex: MutableState<Int>,
        enabled: MutableState<Boolean>,
        overlayContext: OverlayContext,
    ) {
        val expanded = remember { mutableStateOf(false) }
        // default to "exact scan (=)"
        OverlayDropDown(
            enabled = enabled,
            label = "Scan Type",
            expanded = expanded,
            options = Operator.values().map { op: Operator ->
                ACE.operatorEnumToSymbolBiMap[op]!!
            },
            selectedOptionIndex = selectedOptionIndex.value,
            onShowOptions = fun(options: List<String>) {
                OverlayChoicesDialog(overlayContext!!).show(
                    title = "Value: ",
                    choices = options,
                    onConfirm = { index: Int, value: String ->
                        selectedOptionIndex.value = index
                    },
                    onClose = {
                        // after choice dialog is closed
                        // we should also set expanded to false
                        // so drop down will look closed
                        expanded.value = false

                    },
                    chosenIndex = selectedOptionIndex.value
                )
            }
        )
    }

    @Composable
    fun ValueTypeDropDown(
        selectedOptionIndex: MutableState<Int>,
        enabled: MutableState<Boolean>,
        overlayContext: OverlayContext,
    ) {
        val expanded = remember { mutableStateOf(false) }
        OverlayDropDown(
            enabled = enabled,
            label = "Value Type",
            expanded = expanded,
            options = NumType.values().map { numType: NumType ->
                ace.GetNumTypeAndBitSize(numType)
            },
            selectedOptionIndex = selectedOptionIndex.value,
            onShowOptions = fun(options: List<String>) {
                OverlayChoicesDialog(overlayContext!!).show(
                    title = "Value: ",
                    choices = options,
                    onConfirm = { index: Int, value: String ->
                        selectedOptionIndex.value = index
                    },
                    onClose = {
                        // after choice dialog is closed
                        // we should also set expanded to false
                        // so drop down will look closed
                        expanded.value = false

                    },
                    chosenIndex = selectedOptionIndex.value
                )
            }
        )
    }

    @Composable
    fun RegionLevelDropDown(
        selectedOptionIndex: MutableState<Int>,
        enabled: MutableState<Boolean>,
        overlayContext: OverlayContext,
    ) {
        val expanded = remember { mutableStateOf(false) }
        OverlayDropDown(
            enabled = enabled,
            label = "Region Level",
            expanded = expanded,
            options = ACE.RegionLevel.values().map { regionLevel: ACE.RegionLevel ->
                regionLevel.toString()
            },
            selectedOptionIndex = selectedOptionIndex.value,
            onShowOptions = fun(options: List<String>) {
                OverlayChoicesDialog(overlayContext!!).show(
                    title = "Value: ",
                    choices = options,
                    onConfirm = { index: Int, value: String ->
                        selectedOptionIndex.value = index
                    },
                    onClose = {
                        // after choice dialog is closed
                        // we should also set expanded to false
                        // so drop down will look closed
                        expanded.value = false

                    },
                    chosenIndex = selectedOptionIndex.value
                )
            }
        )
    }

    Column(modifier = modifier) {

        Box(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .weight(0.8f)
        ) {
            val columnState = rememberScrollState()
            ColumnScrollbar(state = columnState, alwaysShowScrollBar = true) {
                Column(
                    modifier.verticalScroll(columnState),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ScanInputField(scanValue = scanInputVal)
                    ScanTypeDropDown(
                        scanTypeSelectedOptionIdx,
                        enabled = scanTypeEnabled,
                        overlayContext = overlayContext,
                    )
                    ValueTypeDropDown(
                        valueTypeSelectedOptionIdx,
                        // only allow to change type during initial scan
                        enabled = valueTypeEnabled,
                        overlayContext = overlayContext,
                    )

                    RegionLevelDropDown(
                        selectedOptionIndex = regionLevelSelectedOptionIdx,
                        enabled = regionLevelEnabled,
                        overlayContext = overlayContext,
                    )
                }

            }

        }
        ScanButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f),
            nextScanEnabled = nextScanEnabled,
            // new scan can only be done if we have done at least one scan
            newScanEnabled = newScanEnabled,
            //
            onNextScan = nextScanClicked,
            onNewScan = newScanClicked,
        )

    }
}

@Composable
@Preview
fun MemoryMenuPreview() {
    MemoryMenu(null, null)
}