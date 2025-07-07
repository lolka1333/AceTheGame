package com.kuhakupixel.atg.backend

import org.apache.commons.lang3.StringUtils
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.Closeable

class ACEAttachClient(port: Int) : ACEBaseClient(), Closeable {
    private val context: ZContext = ZContext()
    private val socket: ZMQ.Socket
    private val serverPort: Int = port
    private var isConnected: Boolean = false
    private var currentSendTimeout: Int = 10000  // Default 10 seconds
    private var currentReceiveTimeout: Int = 10000  // Default 10 seconds

    init {
        socket = context.createSocket(SocketType.REQ)
        // Set connection timeout to prevent hanging - increase timeout for memory operations
        socket.setSendTimeOut(currentSendTimeout)
        socket.setReceiveTimeOut(currentReceiveTimeout)
        
        // need to do networking stuff on new thread to avoid
        // Network on main thread exception
        // https://stackoverflow.com/a/14443056/14073678
        val th = Thread { 
            try {
                socket.connect(String.format("tcp://localhost:%d", port))
                isConnected = true
                android.util.Log.i("ATG", "Successfully connected to ACE server at port $port")
            } catch (e: Exception) {
                isConnected = false
                android.util.Log.e("ATG", "Failed to connect to ACE server at port $port: ${e.message}")
                throw RuntimeException("Failed to connect to ACE server at port $port", e)
            }
        }
        th.start()
        try {
            th.join(10000) // Wait max 10 seconds for connection
            if (th.isAlive) {
                th.interrupt()
                isConnected = false
                throw RuntimeException("Connection to ACE server timed out")
            }
        } catch (e: InterruptedException) {
            th.interrupt()
            isConnected = false
            throw RuntimeException("Connection to ACE server was interrupted", e)
        }
    }

    /**
     * Set timeouts for socket operations
     */
    fun setTimeouts(sendTimeoutMs: Int, receiveTimeoutMs: Int) {
        currentSendTimeout = sendTimeoutMs
        currentReceiveTimeout = receiveTimeoutMs
        socket.setSendTimeOut(sendTimeoutMs)
        socket.setReceiveTimeOut(receiveTimeoutMs)
        android.util.Log.d("ATG", "Socket timeouts updated: send=$sendTimeoutMs ms, receive=$receiveTimeoutMs ms")
    }

    /**
     * Reset timeouts to default values
     */
    fun resetTimeouts() {
        setTimeouts(10000, 10000)
    }

    /**
     * Set extended timeouts for intensive operations like all_read_write scanning
     */
    fun setExtendedTimeouts() {
        setTimeouts(300000, 300000) // 5 minutes for very intensive operations
        android.util.Log.i("ATG", "Extended timeouts set for intensive memory operations")
    }

    @Override
    override fun SendCommand(requestCmd: Array<String>): List<String> {
        val requestCmdStr: String = requestCmd.joinToString(separator = " ")
        
        if (!isConnected) {
            throw RuntimeException("Not connected to ACE server")
        }
        
        // Check if this is a scan command with all_read_write region
        val isIntensiveOperation = requestCmdStr.contains("scan") && 
                                   (requestCmdStr.contains("all_read_write") || 
                                    requestCmdStr.contains("all"))
        
        if (isIntensiveOperation) {
            android.util.Log.i("ATG", "Detected intensive memory operation, setting extended timeouts")
            setExtendedTimeouts()
        }
        
        android.util.Log.d("ATG", "Sending command to ACE server: $requestCmdStr")
        
        val maxRetries = if (isIntensiveOperation) 1 else 3 // No retries for intensive operations
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                // Send command
                val sent = socket.send(requestCmdStr.toByteArray(ZMQ.CHARSET), 0)
                if (!sent) {
                    throw RuntimeException("Failed to send command to ACE server (attempt $attempt)")
                }
                
                android.util.Log.d("ATG", "Command sent successfully, waiting for response...")
                
                // For intensive operations, show progress message
                if (isIntensiveOperation) {
                    android.util.Log.i("ATG", "Performing intensive memory scan (all_read_write), this may take several minutes...")
                }
                
                // Receive response
                val reply: ByteArray? = socket.recv(0)
                
                if (reply == null) {
                    val errorMsg = if (isIntensiveOperation) {
                        "Intensive memory scan timed out (${currentReceiveTimeout / 1000}s) - the operation may be too resource-intensive for this device"
                    } else {
                        "No response from ACE server (attempt $attempt/$maxRetries) - possible timeout"
                    }
                    android.util.Log.w("ATG", errorMsg)
                    
                    if (attempt == maxRetries) {
                        isConnected = false
                        if (isIntensiveOperation) {
                            throw RuntimeException("Intensive memory scan failed: The all_read_write scan operation timed out after ${currentReceiveTimeout / 1000} seconds. This operation scans all memory regions and may be too resource-intensive. Try using a more specific region level like 'heap' or 'stack' instead.")
                        } else {
                            throw RuntimeException("ACE server not responding after $maxRetries attempts. Server may have crashed or become unresponsive.")
                        }
                    }
                    
                    // Wait before retry
                    Thread.sleep((500 * attempt).toLong())
                    continue
                }
                
                val outStr = String(reply, ZMQ.CHARSET)
                android.util.Log.d("ATG", "Received response from ACE server: ${outStr.take(100)}${if (outStr.length > 100) "..." else ""}")
                
                if (isIntensiveOperation) {
                    android.util.Log.i("ATG", "Intensive memory scan completed successfully")
                    resetTimeouts() // Reset timeouts after intensive operation
                }
                
                if (StringUtils.isEmpty(outStr)) {
                    return ArrayList<String>()
                } else {
                    val output: MutableList<String> = outStr.split("\n").toMutableList()
                    // need to remove last item if its empty
                    // because its gonna play badly with other functions in this project
                    // (happens when at last line it still contains "\n")
                    // https://stackoverflow.com/questions/48697300/difference-between-kotlin-and-java-string-split-with-regex
                    if (output.isNotEmpty() && output[output.size - 1] == "")
                        output.removeAt(output.size - 1)
                    return output
                }
                
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("ATG", "Error in SendCommand attempt $attempt: ${e.message}")
                
                if (attempt == maxRetries) {
                    break
                }
                
                // Wait before retry
                Thread.sleep((500 * attempt).toLong())
            }
        }
        
        // All retries failed
        isConnected = false
        if (isIntensiveOperation) {
            resetTimeouts() // Reset timeouts even on failure
        }
        android.util.Log.e("ATG", "Failed to communicate with ACE server after $maxRetries attempts")
        
        if (isIntensiveOperation) {
            throw RuntimeException("Intensive memory scan failed: The all_read_write scan operation failed after ${currentReceiveTimeout / 1000} seconds. This operation scans all memory regions and may be too resource-intensive for this device. Try using a more specific region level like 'heap' or 'stack' instead. Error: ${lastException?.message}", lastException)
        } else {
            throw RuntimeException("Failed to communicate with ACE server after $maxRetries attempts. Last error: ${lastException?.message}", lastException)
        }
    }

    /**
     * Check if the connection to ACE server is alive
     */
    fun isServerAlive(): Boolean {
        return isConnected
    }
    
    /**
     * Reset connection state (call this if you suspect the server has died)
     */
    fun resetConnection() {
        isConnected = false
        resetTimeouts() // Reset timeouts when connection is reset
        android.util.Log.w("ATG", "ACE server connection reset")
    }

    override fun close() {
        android.util.Log.i("ATG", "Closing ACE client connection to port $serverPort")
        isConnected = false
        resetTimeouts() // Reset timeouts when closing
        
        try {
            socket.close()
        } catch (e: Exception) {
            android.util.Log.e("ATG", "Error closing socket: ${e.message}")
        }
        try {
            context.close()
        } catch (e: Exception) {
            android.util.Log.e("ATG", "Error closing context: ${e.message}")
        }
    }
}