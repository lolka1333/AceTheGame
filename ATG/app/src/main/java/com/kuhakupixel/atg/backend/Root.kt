package com.kuhakupixel.atg.backend

import android.util.Log
import java.io.IOException

object Root {
    /*
     * Run [strings] as sudo
     * credits: https://stackoverflow.com/a/26654728/14073678
     * */
    fun sudo(cmd: List<String>): List<String> {
        var res: List<String> = listOf()
        
        if (cmd.isEmpty()) {
            Log.w("ATG", "Empty command passed to sudo")
            return res
        }
        
        try {
            val fullCmd: MutableList<String> = mutableListOf("su", "--command")
            // add [cmd] as only one string because we want to pass it as one value
            // to `--command` parameter. Runtime.getRuntime().exec() will surround each string
            // with ("") that will cause [cmd] to be thought as different value from `--command`
            // if we just append [fullCmd] and [cmd]

            // example: if [cmd] is ["/usr/bin/echo", "hi"]
            // if we only append [fullCmd] and [cmd]
            // like [fullCmd] = ["su", "--command","/usr/bin/echo", "hi"]
            // the value "hi" will be thought as part of [su] command instead of [/usr/bin/echo]
            // the correct one should be
            // [fullCmd] = ["su", "--command","/usr/bin/echo hi"]
            val cmdStr = cmd.joinToString(separator = " ")
            fullCmd.add(cmdStr)
            
            Log.d("ATG", "Executing sudo command: $cmdStr")
            res = Cmd.RunCommand(fullCmd)
            
            // Check if there was an exit code error
            val exitCodeLine = res.find { it.startsWith("__EXIT_CODE__") }
            if (exitCodeLine != null) {
                val exitCode = exitCodeLine.substringAfter("__EXIT_CODE__").toIntOrNull() ?: -1
                if (exitCode != 0) {
                    Log.w("ATG", "Sudo command '$cmdStr' exited with code $exitCode")
                    // Remove the exit code line from results
                    res = res.filterNot { it.startsWith("__EXIT_CODE__") }
                }
            }
            
        } catch (e: IOException) {
            Log.e("ATG", "IOException in sudo command: ${e.message}")
            throw RuntimeException("Failed to execute sudo command: ${cmd.joinToString(" ")}", e)
        } catch (e: RuntimeException) {
            Log.e("ATG", "RuntimeException in sudo command: ${e.message}")
            throw RuntimeException("Failed to execute sudo command: ${cmd.joinToString(" ")}", e)
        } catch (e: Exception) {
            Log.e("ATG", "Unexpected exception in sudo command: ${e.message}")
            throw RuntimeException("Unexpected error executing sudo command: ${cmd.joinToString(" ")}", e)
        }
        
        return res
    }
}