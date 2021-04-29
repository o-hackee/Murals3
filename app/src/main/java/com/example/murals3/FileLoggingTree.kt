package com.example.murals3

import android.content.Context
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// TODO без чистки
class FileLoggingTree(val applicationContext: Context) : DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        try {
            val fileNameTimeStamp: String = SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(Date())
            val logTimeStamp: String = SimpleDateFormat(
                "E MMM dd yyyy 'at' hh:mm:ss:SSS aaa",
                Locale.getDefault()
            ).format(Date())
            val fileName = "$fileNameTimeStamp.html"

            // Create file
            val file: File? = generateLogFile(fileName)

            // If file created or exists save logs
            if (file != null) {
                val writer = FileWriter(file, true)
                writer.append(
                    "<p style=\"background:lightgray;\"><strong "
                            + "style=\"background:lightblue;\">&nbsp&nbsp"
                )
                    .append(logTimeStamp)
                    .append(" :&nbsp&nbsp</strong><strong>&nbsp&nbsp")
                    .append(tag)
                    .append("</strong> - ")
                    .append(message)
                    .append("</p>")
                writer.flush()
                writer.close()
            }
        } catch (e: Exception) {
            Timber.e(FileLoggingTree::class.java.simpleName, "Error while logging into file : $e")
        }
    }

    override fun createStackElementTag(element: StackTraceElement): String {
        // Add log statements line number to the log
        return super.createStackElementTag(element) + " - " + element.lineNumber
    }

    /*  Helper method to create file*/
    private fun generateLogFile(fileName: String): File? {
        val root = File(
            applicationContext.getExternalFilesDir(null)?.absolutePath,
            "Log"
        )
        var dirExists = true
        if (!root.exists()) {
            dirExists = root.mkdirs()
        }
        return if (dirExists) {
            File(root, fileName)
        } else null
    }
}