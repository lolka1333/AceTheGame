package com.kuhakupixel.atg.backend

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File

object ACEServer {
    /*
     * Get thread to start the server
     * */
    fun GetStarterThread(
        context: Context,
        pid: Long,
        portNum: Int,
        statusPublisherPort: Int
    ): Thread {
        return Thread {
            try {
                Log.i("ATG", String.format("Running engine server at port %d", portNum))
                var path = ""
                path = Binary.GetBinPath(context, Binary.Type.server)
                
                if (!File(path).exists()) {
                    Log.e("ATG", "ACE server binary not found at: $path")
                    throw RuntimeException("ACE server binary not found at: $path")
                }
                
                System.out.println("Binary path is $path")
                val cmds = arrayOf(
                    path,
                    "attach-pid",
                    pid.toString(),
                    "--port",
                    portNum.toString(),
                    "--status_publisher_port",
                    statusPublisherPort.toString()
                )
                val cmd_string: String = cmds.joinToString(separator = " ")
                Log.i("ATG", String.format("Running command %s\n", cmd_string))
                
                val res: Shell.Result = Shell.cmd(cmd_string).exec()
                
                // Check if command was successful
                if (!res.isSuccess) {
                    Log.e("ATG", "ACE server failed to start. Exit code: ${res.code}")
                    val output: MutableList<String> = mutableListOf()
                    output.addAll(res.getOut())
                    output.addAll(res.getErr())
                    val errorMsg = output.joinToString(separator = "\n")
                    Log.e("ATG", "ACE server error output: $errorMsg")
                    throw RuntimeException("ACE server failed to start. Exit code: ${res.code}. Error: $errorMsg")
                }
                
                // add output
                val output: MutableList<String> = mutableListOf()
                output.addAll(res.getOut())
                output.addAll(res.getErr())
                // print
                Log.i("ATG", "ACE server started successfully")
                Log.i("ATG", output.joinToString(separator = "\n"))
                
            } catch (e: Exception) {
                Log.e("ATG", "Exception in ACE server thread: ${e.message}", e)
                throw RuntimeException("Failed to start ACE server", e)
            }
        }
    }
}