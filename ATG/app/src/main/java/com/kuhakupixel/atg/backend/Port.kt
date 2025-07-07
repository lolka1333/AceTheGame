package com.kuhakupixel.atg.backend

import android.util.Log
import java.net.ServerSocket
import java.net.SocketException

object Port {
    
    fun GetOpenPorts(portCount: Int): List<Int> {
        val maxRetries = 5
        for (attempt in 1..maxRetries) {
            try {
                val sockets: MutableList<ServerSocket> = mutableListOf()
                val openPorts: MutableList<Int> = mutableListOf()
                
                try {
                    for (i in 0 until portCount) {
                        // if we pass 0, a port number will be automatically allocated
                        // https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html#ServerSocket%28int%29
                        val socket = ServerSocket(0)
                        sockets.add(socket)
                        val portNum: Int = socket.getLocalPort()
                        openPorts.add(portNum)
                    }
                } catch (e: SocketException) {
                    // Close any sockets that were opened before the failure
                    for (socket in sockets) {
                        try {
                            socket.close()
                        } catch (ignored: Exception) {
                        }
                    }
                    Log.w("ATG", "Failed to get open ports on attempt $attempt: ${e.message}")
                    if (attempt == maxRetries) {
                        throw RuntimeException("Failed to get $portCount open ports after $maxRetries attempts", e)
                    }
                    Thread.sleep((100 * attempt).toLong()) // Back off before retry
                    continue
                }
                
                // close all the sockets so caller can actually use them :v
                // Note: This creates a race condition, but it's inherent to the design
                for (socket in sockets) {
                    try {
                        socket.close()
                    } catch (e: Exception) {
                        Log.w("ATG", "Warning: Failed to close socket for port ${socket.localPort}: ${e.message}")
                    }
                }
                
                Log.i("ATG", "Successfully allocated $portCount ports: $openPorts")
                return openPorts
                
            } catch (e: Exception) {
                Log.e("ATG", "Exception in GetOpenPorts attempt $attempt: ${e.message}")
                if (attempt == maxRetries) {
                    throw RuntimeException("Failed to get open ports after $maxRetries attempts", e)
                }
                Thread.sleep((100 * attempt).toLong()) // Back off before retry
            }
        }
        
        throw RuntimeException("Failed to get open ports after $maxRetries attempts")
    }

    
    fun GetOpenPort(): Int {
        return GetOpenPorts(1)[0]
    }
    
    /**
     * Check if a port is available for use
     */
    fun IsPortAvailable(port: Int): Boolean {
        return try {
            val socket = ServerSocket(port)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}