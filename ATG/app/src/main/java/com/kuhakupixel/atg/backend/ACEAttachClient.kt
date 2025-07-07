package com.kuhakupixel.atg.backend

import org.apache.commons.lang3.StringUtils
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.Closeable

class ACEAttachClient(port: Int) : ACEBaseClient(), Closeable {
    private val context: ZContext = ZContext()
    private val socket: ZMQ.Socket

    init {
        socket = context.createSocket(SocketType.REQ)
        // Set connection timeout to prevent hanging
        socket.setSendTimeOut(5000) // 5 seconds
        socket.setReceiveTimeOut(5000) // 5 seconds
        
        // need to do networking stuff on new thread to avoid
        // Network on main thread exception
        // https://stackoverflow.com/a/14443056/14073678
        val th = Thread { 
            try {
                socket.connect(String.format("tcp://localhost:%d", port))
            } catch (e: Exception) {
                // If connection fails, we'll handle it in SendCommand
                android.util.Log.e("ATG", "Failed to connect to ACE server at port $port: ${e.message}")
                throw RuntimeException("Failed to connect to ACE server at port $port", e)
            }
        }
        th.start()
        try {
            th.join(10000) // Wait max 10 seconds for connection
            if (th.isAlive) {
                th.interrupt()
                throw RuntimeException("Connection to ACE server timed out")
            }
        } catch (e: InterruptedException) {
            th.interrupt()
            throw RuntimeException("Connection to ACE server was interrupted", e)
        }
    }

    @Override
    override fun SendCommand(requestCmd: Array<String>): List<String> {
        val requestCmdStr: String = requestCmd.joinToString(separator = " ")
        
        try {
            val sent = socket.send(requestCmdStr.toByteArray(ZMQ.CHARSET), 0)
            if (!sent) {
                throw RuntimeException("Failed to send command to ACE server")
            }
            
            val reply: ByteArray = socket.recv(0)
                ?: throw RuntimeException("Failed to receive response from ACE server")
                
            val outStr = String(reply, ZMQ.CHARSET)
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
            android.util.Log.e("ATG", "Error in SendCommand: ${e.message}")
            throw RuntimeException("Failed to communicate with ACE server", e)
        }
    }

    override fun close() {
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