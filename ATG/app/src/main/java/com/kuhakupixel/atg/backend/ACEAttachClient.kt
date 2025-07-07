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

    init {
        socket = context.createSocket(SocketType.REQ)
        // Set connection timeout to prevent hanging - increase timeout for memory operations
        socket.setSendTimeOut(10000) // 10 seconds for memory operations
        socket.setReceiveTimeOut(10000) // 10 seconds for memory operations
        
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

    @Override
    override fun SendCommand(requestCmd: Array<String>): List<String> {
        val requestCmdStr: String = requestCmd.joinToString(separator = " ")
        
        if (!isConnected) {
            throw RuntimeException("Not connected to ACE server")
        }
        
        android.util.Log.d("ATG", "Sending command to ACE server: $requestCmdStr")
        
        val maxRetries = 3
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                // Send command
                val sent = socket.send(requestCmdStr.toByteArray(ZMQ.CHARSET), 0)
                if (!sent) {
                    throw RuntimeException("Failed to send command to ACE server (attempt $attempt)")
                }
                
                android.util.Log.d("ATG", "Command sent successfully, waiting for response...")
                
                // Receive response
                val reply: ByteArray? = socket.recv(0)
                
                if (reply == null) {
                    val errorMsg = "No response from ACE server (attempt $attempt/$maxRetries) - possible timeout"
                    android.util.Log.w("ATG", errorMsg)
                    
                    if (attempt == maxRetries) {
                        isConnected = false
                        throw RuntimeException("ACE server not responding after $maxRetries attempts. Server may have crashed or become unresponsive.")
                    }
                    
                    // Wait before retry
                    Thread.sleep((500 * attempt).toLong())
                    continue
                }
                
                val outStr = String(reply, ZMQ.CHARSET)
                android.util.Log.d("ATG", "Received response from ACE server: ${outStr.take(100)}${if (outStr.length > 100) "..." else ""}")
                
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
        android.util.Log.e("ATG", "Failed to communicate with ACE server after $maxRetries attempts")
        throw RuntimeException("Failed to communicate with ACE server after $maxRetries attempts. Last error: ${lastException?.message}", lastException)
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
        android.util.Log.w("ATG", "ACE server connection reset")
    }

    override fun close() {
        android.util.Log.i("ATG", "Closing ACE client connection to port $serverPort")
        isConnected = false
        
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