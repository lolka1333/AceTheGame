package com.kuhakupixel.atg.backend

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object Cmd {
    fun RunCommand(cmds: List<String>): List<String> {
        val res: MutableList<String> = mutableListOf()
        var process: Process? = null
        
        try {
            process = Runtime.getRuntime().exec(cmds.toTypedArray())
            
            val stdInput = BufferedReader(InputStreamReader(process.getInputStream()))
            val stdError = BufferedReader(InputStreamReader(process.getErrorStream()))

            // Read stdout
            stdInput.use { buffedReader: BufferedReader ->
                buffedReader.lineSequence().forEach { s: String ->
                    res.add(s)
                }
            }

            // Read stderr
            stdError.use { buffedReader: BufferedReader ->
                buffedReader.lineSequence().forEach { s: String ->
                    res.add(s)
                }
            }
            
            // Wait for process to complete and check exit code
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val cmdStr = cmds.joinToString(" ")
                Log.w("ATG", "Command '$cmdStr' exited with code $exitCode")
                // Don't throw exception here as some commands might use non-zero exit codes normally
                // But add the exit code information to the output
                res.add("__EXIT_CODE__$exitCode")
            }
            
        } catch (e: IOException) {
            Log.e("ATG", "IOException in RunCommand: ${e.message}")
            throw RuntimeException("Failed to execute command: ${cmds.joinToString(" ")}", e)
        } catch (e: InterruptedException) {
            Log.e("ATG", "InterruptedException in RunCommand: ${e.message}")
            Thread.currentThread().interrupt()
            throw RuntimeException("Command execution was interrupted: ${cmds.joinToString(" ")}", e)
        } finally {
            process?.destroy()
        }
        
        return res
    }
}