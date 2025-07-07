package com.kuhakupixel.atg.backend

import android.content.Context
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

/**
 * to communicate with ACE's engine binary
 * sending input and getting output
 *
 * TODO: NEED TO BE THREAD SAFE
 *       where to put the lock where its the most appropriate? in order to prevent data race
 *       but still responsive (not locked out) when its needed in some case
 *       ex: not freezing the apk when switching menu because of calling a synchronized function
 *       that not need to be synchronized
 */
class ACE(context: Context) {
    /**
     * thrown when an operation requires attach to a process
     * but we haven't
     */
    inner class NoAttachException : RuntimeException {
        constructor() : super() {}
        constructor(msg: String?) : super(msg) {}
    }

    /**
     * thrown when trying to attach when we have attached to a process
     * without deattaching first
     */
    inner class AttachingInARowException : RuntimeException {
        constructor() : super() {}
        constructor(msg: String?) : super(msg) {}
    }

    enum class Operator {
        greater, less, equal, greaterEqual, lessEqual, notEqual, unknown
    }

    enum class NumType {
        _int, _long, _short, _float, _byte;


        @Override
        override fun toString(): String {
            return this.name.replace("_", "")
        }

        companion object {
            fun fromString(s: String): NumType {
                var s = s
                if (s[0] != '_') s = "_$s"
                return valueOf(s)
            }
        }
    }

    enum class RegionLevel {
        heap_stack_executable,
        heap_stack_executable_bss,

        /*
         * all region that has read and write permission
         * */
        all_read_write,
        all,
    }

    inner class MatchInfo(var address: String, var prevValue: String)

    private val context: Context
    private val availableNumTypes: List<NumTypeInfo>

    /**
     * used for use cases that are unrelated to a specific process
     * for example, listing running processes, checking if a certain program is running
     * and etc
     */
    private val aceUtilClient: ACEUtilClient

    private var statusPublisherPort: Int? = null

    /**
     * used when attached to process, to scan and edit its memory
     */
    private var aceAttachClient: ACEAttachClient? = null

    /**
     * the running server thread
     *
     *
     * if null means it isn't attached to anything
     */
    private var serverThread: Thread? = null

    //

    
    fun getStatusPublisherPort(): Int? {
        return statusPublisherPort
    }

    init {
        this.context = context
        aceUtilClient = ACEUtilClient(context)
        availableNumTypes = GetAvailableNumTypes()
    }

    fun IsAttached(): Boolean {
        return aceAttachClient != null && IsServerResponsive()
    }

    /**
     * Check if client exists (regardless of server responsiveness)
     */
    fun HasClient(): Boolean {
        return aceAttachClient != null
    }

    private fun AssertAttached() {
        if (!IsAttached()) throw NoAttachException("Operation requires attaching to a process, but it hasn't been attached")
    }

    private fun AssertNoAttachInARow() {
        if (HasClient()) {
            android.util.Log.w("ATG", "Client exists but may not be responsive, attempting to force detach first")
            ForceDetach()
        }
    }

    /**
     * Force detach without server communication - use when server is unresponsive
     */
    fun ForceDetach() {
        android.util.Log.i("ATG", "Force detaching from process")
        
        if (aceAttachClient != null) {
            try {
                aceAttachClient!!.close()
            } catch (e: Exception) {
                android.util.Log.w("ATG", "Error closing attach client during force detach: ${e.message}")
            }
            aceAttachClient = null
        }
        
        if (serverThread != null) {
            try {
                serverThread!!.interrupt()
            } catch (e: Exception) {
                android.util.Log.w("ATG", "Error interrupting server thread during force detach: ${e.message}")
            }
            serverThread = null
        }
        
        android.util.Log.i("ATG", "Force detach completed")
    }

    fun ConnectToACEServer(port: Int, publisherPort: Int) {
        AssertNoAttachInARow()
        this.statusPublisherPort = publisherPort
        aceAttachClient = ACEAttachClient(port)
    }

    /**
     * this will create an ACE's server that is attached to process [pid]
     */
    
    fun Attach(pid: Long) {
        AssertNoAttachInARow()
        try {
            // start the server
            val ports: List<Int> = Port.GetOpenPorts(2)
            serverThread = ACEServer.GetStarterThread(context, pid, ports[0], ports[1])
            serverThread!!.start()
            
            // Give the server some time to start
            Thread.sleep(1000)
            
            // Check if the server thread is still alive (not crashed)
            if (!serverThread!!.isAlive) {
                throw RuntimeException("ACE server thread died unexpectedly")
            }
            
            ConnectToACEServer(ports[0], ports[1])
            
            // Verify that we can communicate with the server
            try {
                GetAttachedPid()
            } catch (e: Exception) {
                throw RuntimeException("Failed to communicate with ACE server after attach", e)
            }
            
        } catch (e: Exception) {
            // Clean up if attach failed
            if (aceAttachClient != null) {
                try {
                    aceAttachClient!!.close()
                } catch (ignored: Exception) {
                }
                aceAttachClient = null
            }
            if (serverThread != null) {
                try {
                    serverThread!!.interrupt()
                } catch (ignored: Exception) {
                }
                serverThread = null
            }
            throw RuntimeException("Failed to attach to process $pid", e)
        }
    }

    
    fun DeAttach() {
        AssertAttached()
        // tell server to die
        aceAttachClient!!.Request(arrayOf("stop"))
        aceAttachClient!!.close()
        aceAttachClient = null
        // only stop the server if we start one
        if (serverThread != null) {
            // wait for server's thread to finish
            // to make sure we are not attached anymore
            serverThread!!.join()
        }
    }

    fun GetNumTypeBitSize(numType: NumType): Int? {
        var bitSize: Int? = null
        for (typeInfo in availableNumTypes) {
            if (typeInfo.GetName().equals(numType.toString())) bitSize = typeInfo.GetBitSize()
        }
        return bitSize
    }

    fun GetNumTypeAndBitSize(numType: NumType): String {
        val bitSize: Int? = GetNumTypeBitSize(numType)
        return String.format("%s (%d bit)", numType.toString(), bitSize)
    }

    // =============== this commands require attach ===================
    fun CheaterCmd(cmd: Array<String>): String {
        AssertAttached()
        
        try {
            // Check if server is alive before sending command
            if (aceAttachClient is ACEAttachClient && !(aceAttachClient as ACEAttachClient).isServerAlive()) {
                throw RuntimeException("ACE server connection is not alive")
            }
            
            return aceAttachClient!!.Request(cmd)
            
        } catch (e: RuntimeException) {
            android.util.Log.e("ATG", "Error in CheaterCmd: ${e.message}")
            
            // If it's a communication error, we might need to restart the connection
            if (e.message?.contains("Failed to communicate with ACE server") == true || 
                e.message?.contains("ACE server not responding") == true) {
                
                android.util.Log.w("ATG", "ACE server appears to be unresponsive, connection may need to be reset")
                
                // Mark client as disconnected to prevent further attempts
                if (aceAttachClient is ACEAttachClient) {
                    (aceAttachClient as ACEAttachClient).resetConnection()
                }
            }
            
            throw RuntimeException("Failed to execute command on ACE server: ${cmd.joinToString(" ")}", e)
        }
    }

    fun CheaterCmdAsList(cmd: Array<String>): List<String> {
        AssertAttached()
        
        try {
            // Check if server is alive before sending command
            if (aceAttachClient is ACEAttachClient && !(aceAttachClient as ACEAttachClient).isServerAlive()) {
                throw RuntimeException("ACE server connection is not alive")
            }
            
            return aceAttachClient!!.RequestAsList(cmd)
            
        } catch (e: RuntimeException) {
            android.util.Log.e("ATG", "Error in CheaterCmdAsList: ${e.message}")
            
            // If it's a communication error, we might need to restart the connection
            if (e.message?.contains("Failed to communicate with ACE server") == true || 
                e.message?.contains("ACE server not responding") == true) {
                
                android.util.Log.w("ATG", "ACE server appears to be unresponsive, connection may need to be reset")
                
                // Mark client as disconnected to prevent further attempts
                if (aceAttachClient is ACEAttachClient) {
                    (aceAttachClient as ACEAttachClient).resetConnection()
                }
            }
            
            throw RuntimeException("Failed to execute command on ACE server: ${cmd.joinToString(" ")}", e)
        }
    }

    fun GetAttachedPid(): Long {
        val pidStr = CheaterCmd(arrayOf("pid"))
        return pidStr.toLong()
    }
    
    /**
     * Check if the ACE server is responsive
     */
    fun IsServerResponsive(): Boolean {
        if (aceAttachClient == null) {
            return false
        }
        
        try {
            // Quick responsiveness check - try to get PID
            val pidStr = aceAttachClient!!.Request(arrayOf("pid"))
            return pidStr.isNotEmpty() && pidStr.toLongOrNull() != null
        } catch (e: Exception) {
            android.util.Log.w("ATG", "ACE server responsiveness check failed: ${e.message}")
            return false
        }
    }
    
    /**
     * Get server status information for debugging
     */
    fun GetServerStatus(): String {
        return if (!IsAttached()) {
            "Not attached to any process"
        } else {
            try {
                val pid = GetAttachedPid()
                val alive = if (aceAttachClient is ACEAttachClient) {
                    (aceAttachClient as ACEAttachClient).isServerAlive()
                } else {
                    "Unknown"
                }
                "Attached to PID: $pid, Connection alive: $alive"
            } catch (e: Exception) {
                "Attached but server not responding: ${e.message}"
            }
        }
    }

    
    fun SetNumType(type: NumType) {
        CheaterCmd(arrayOf("config", "type", type.toString()))
    }

    /**
     * get current type that ACE use
     */
    
    fun GetNumType(): NumType {
        val typeStr = CheaterCmd(arrayOf("config", "type"))
        return NumType.fromString(typeStr)
    }

    
    fun SetRegionLevel(regionLevel: RegionLevel) {
        CheaterCmd(arrayOf("config", "region_level", regionLevel.toString()))
    }

    
    fun GetRegionLevel(): RegionLevel {
        val regionLevelStr = CheaterCmd(arrayOf("config", "region_level"))
        return RegionLevel.valueOf(regionLevelStr)

    }


    /**
     * run code/function when type is set to [numType]
     * after done, the type will be set to the previous one
     */
    
    fun ActionOnType(numType: NumType, action: () -> Unit) {
        val prevType = GetNumType()
        // set type first before writing
        if (prevType != numType) SetNumType(numType)
        action()
        if (prevType != numType) SetNumType(prevType)
    }

    
    fun ScanAgainstValue(operator: Operator, numValStr: String) {
        CheaterCmd(arrayOf("scan", operatorEnumToSymbolBiMap.get(operator)!!, numValStr))
    }

    
    fun ScanWithoutValue(operator: Operator) {
        CheaterCmd(arrayOf("filter", operatorEnumToSymbolBiMap.get(operator)!!))
    }

    
    fun WriteValueAtAddress(numType: NumType, address: String, value: String) {
        ActionOnType(numType) {
            this.CheaterCmd(arrayOf("writeat", address, value))
        }
    }

    
    fun FreezeAtAddress(numType: NumType, address: String) {
        ActionOnType(numType) {
            this.CheaterCmd(arrayOf("freeze at", address))
        }
    }

    
    fun FreezeValueAtAddress(numType: NumType, address: String, value: String) {
        ActionOnType(numType) {
            this.CheaterCmd(
                arrayOf(
                    "freeze at",
                    address,
                    "--value",
                    value
                )
            )
        }
    }

    
    fun UnFreezeAtAddress(numType: NumType, address: String) {
        ActionOnType(numType) {
            this.CheaterCmd(arrayOf("unfreeze at", address))
        }
    }

    
    fun GetMatchCount(): Int {
        return CheaterCmd(arrayOf<String>("matchcount")).toInt()
    }

    
    fun ResetMatches() {
        CheaterCmd(arrayOf("reset"))
    }

    
    fun ListMatches(maxCount: Int): List<MatchInfo> {
        /**
         * get list of matches with list command
         * which will return a list of [address] - [prev value] one per each line
         */
        val matches: MutableList<MatchInfo> = mutableListOf<MatchInfo>()
        val matchesStr = CheaterCmdAsList(arrayOf("list", "--max-count", maxCount.toString()))
        for (s: String in matchesStr) {
            val splitted: List<String> = s.split(" ")
            if (splitted.size != 2) {
                throw IllegalArgumentException(
                    String.format(
                        "unexpected Output when listing matches: \"%s\"",
                        s
                    )
                )
            }
            matches.add(MatchInfo(splitted[0], splitted[1]))
        }
        return matches
    }

    // =============== this commands don't require attach ===================
    
    fun UtilCmdAsList(cmd: Array<String>): List<String> {
        return aceUtilClient.RequestAsList(cmd)
    }

    
    fun UtilCmd(cmd: Array<String>): String {
        return aceUtilClient.Request(cmd)
    }

    
    fun ListRunningProc(): List<ProcInfo> {
        val runningProcs: MutableList<ProcInfo> = mutableListOf()
        // use --reverse so newest process will be shown first
        val runningProcsInfoStr = UtilCmdAsList(arrayOf("ps", "ls", "--reverse"))
        // parse each string
        for (procInfoStr in runningProcsInfoStr) {
            runningProcs.add(ProcInfo(procInfoStr))
        }
        return runningProcs
    }

    /**
     * Get running processes with full names using standard Linux ps command
     * This bypasses the ACE util_client limitation that truncates process names
     */
    fun ListRunningProcFull(): List<ProcInfo> {
        val runningProcs: MutableList<ProcInfo> = mutableListOf()
        try {
            // Use Toybox ps command (Android compatible)
            // -e: all processes, -o: custom format, -w: wide output (don't truncate)
            // -k: sort by field, -pid: sort by PID descending
            val cmd = listOf("ps", "-eo", "pid,comm", "-w", "-k", "-pid")
            val processInfoLines = Root.sudo(cmd)
            
            if (processInfoLines.isEmpty()) {
                android.util.Log.w("ATG", "No output from ps command")
                return ListRunningProc() // Fallback to original method
            }
            
            // Skip the header line (first line contains "PID COMM")
            val dataLines = processInfoLines.drop(1)
            
            for (line in dataLines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("__EXIT_CODE__")) {
                    try {
                        // Split by whitespace, taking first part as PID and rest as process name
                        val parts = trimmedLine.split(Regex("\\s+"), 2)
                        if (parts.size >= 2) {
                            val pid = parts[0]
                            val processName = parts[1]
                            
                            // Validate PID is numeric
                            if (pid.toLongOrNull() != null) {
                                // Create ProcInfo with "pid processname" format
                                runningProcs.add(ProcInfo("$pid $processName"))
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ATG", "Failed to parse process line: $trimmedLine", e)
                        // Continue with other lines
                    }
                }
            }
            
            if (runningProcs.isEmpty()) {
                android.util.Log.w("ATG", "No valid processes found with ps command")
                return ListRunningProc() // Fallback to original method
            }
            
        } catch (e: Exception) {
            // If the new method fails, fall back to the original method
            android.util.Log.w("ATG", "Failed to get full process names, falling back to original method: ${e.message}")
            return ListRunningProc()
        }
        return runningProcs
    }

    /**
     * Get running processes with full command lines (including arguments)
     */
    fun ListRunningProcWithArgs(): List<ProcInfo> {
        val runningProcs: MutableList<ProcInfo> = mutableListOf()
        try {
            // Use Toybox ps command with args to get full command lines
            // -e: all processes, -o: custom format, -w: wide output (don't truncate)
            // -k: sort by field, -pid: sort by PID descending
            val cmd = listOf("ps", "-eo", "pid,args", "-w", "-k", "-pid")
            val processInfoLines = Root.sudo(cmd)
            
            if (processInfoLines.isEmpty()) {
                android.util.Log.w("ATG", "No output from ps command")
                return ListRunningProc() // Fallback to original method
            }
            
            // Skip the header line (first line contains "PID ARGS")
            val dataLines = processInfoLines.drop(1)
            
            for (line in dataLines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("__EXIT_CODE__")) {
                    try {
                        // Split by whitespace, taking first part as PID and rest as command line
                        val parts = trimmedLine.split(Regex("\\s+"), 2)
                        if (parts.size >= 2) {
                            val pid = parts[0]
                            val commandLine = parts[1]
                            
                            // Validate PID is numeric
                            if (pid.toLongOrNull() != null) {
                                // Create ProcInfo with "pid commandline" format
                                runningProcs.add(ProcInfo("$pid $commandLine"))
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ATG", "Failed to parse process line: $trimmedLine", e)
                        // Continue with other lines
                    }
                }
            }
            
            if (runningProcs.isEmpty()) {
                android.util.Log.w("ATG", "No valid processes found with ps command")
                return ListRunningProc() // Fallback to original method
            }
            
        } catch (e: Exception) {
            // If the new method fails, fall back to the original method
            android.util.Log.w("ATG", "Failed to get process command lines, falling back to original method: ${e.message}")
            return ListRunningProc()
        }
        return runningProcs
    }

    
    fun IsPidRunning(pid: Long): Boolean {
        val boolStr = UtilCmd(arrayOf("ps", "is_running", pid.toString()))
        return boolStr.toBooleanStrict()
    }

    /**
     * Get List of available number types and its bit size
     * that ACE engine support, with command `type size`
     * which will return list of "<type name> <bit size>"
     * like "int 32", "short 16" and ect
    </bit></type> */
    fun GetAvailableNumTypes(): List<NumTypeInfo> {
        val numTypeInfos: MutableList<NumTypeInfo> = mutableListOf()
        val out = UtilCmdAsList(arrayOf("info", "type"))
        for (s in out) {
            val splitted: List<String> = s.split(" ")
            assert(2 == splitted.size)
            val typeStr = splitted[0]
            val bitSize: Int = splitted[1].toInt()
            numTypeInfos.add(NumTypeInfo(typeStr, bitSize))
        }
        return numTypeInfos
    }

    
    fun GetAvailableOperatorTypes(): List<Operator> {
        // the output will be a list of supported operators like
        // >
        // <
        // >=
        // etc
        val availableOperators: MutableList<Operator> = mutableListOf()
        val out = UtilCmdAsList(arrayOf("info", "operator"))
        for (s in out) availableOperators.add(operatorEnumToSymbolBiMap.inverse().get(s)!!)
        return availableOperators
    }

    companion object {
        // https://stackoverflow.com/a/507658/14073678
        val operatorEnumToSymbolBiMap: BiMap<Operator, String> = HashBiMap.create()

        init {
            operatorEnumToSymbolBiMap.put(Operator.greater, ">")
            operatorEnumToSymbolBiMap.put(Operator.less, "<")
            operatorEnumToSymbolBiMap.put(Operator.equal, "=")
            operatorEnumToSymbolBiMap.put(Operator.greaterEqual, ">=")
            operatorEnumToSymbolBiMap.put(Operator.lessEqual, "<=")
            operatorEnumToSymbolBiMap.put(Operator.notEqual, "!=")
            operatorEnumToSymbolBiMap.put(Operator.unknown, "?")
        }
    }
}