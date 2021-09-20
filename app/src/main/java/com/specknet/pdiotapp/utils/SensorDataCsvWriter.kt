package com.specknet.pdiotapp.utils

import android.content.Context
import android.util.Log
import java.io.*

interface CsvSerializable : Serializable {
    // TODO: generic logic for generating csv headers could be added here
    fun toCsvHeader(): String = ""
//     = CsvSerializable::class.memberProperties.joinToString(", ") { it.name }

    fun toCsvString(): String = ""
//        = CsvSerializable::class.memberProperties.map { it.get(this) }.joinToString(", ")
}

/**
 * A re-usable class for writing data to a csv file
 */
class SensorDataCsvWriter<T : CsvSerializable>(
    val filename: String,
    val header: String,
    val ctx: Context,
    val encrypted: Boolean = true
) {

    val file = when (filename.endsWith(".csv")) {
        true -> File(filename)
        false -> File("$filename.csv")
    }

    private lateinit var writer: BufferedWriter

    init {
        setup()
    }

    fun setup() {
        // this is the first time the file is being written to
        val firstWrite = !file.exists()
        Log.i(TAG, "Initializing... first write = $firstWrite")

        if (firstWrite) {
            // make the directories just in case
            val parent = file.parent ?: ""
            File(parent).mkdirs()
        }

        writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))
        if (firstWrite) {
            writer.write(header)
            writer.newLine()
            writer.flush()
        }
    }

    fun write(data: T) {
        // Write new line to file. If concatenation is split up with append, the second part might not be written,
        // meaning that there will be a line without a line break in the file.
        try {
            var line = data.toCsvString()
            writer.write(line)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            // try to re-create the file
            setup()
        }
    }

    fun close() {
        writer.flush()
        writer.close()
    }

    companion object {
        val TAG = this::class.java.canonicalName
    }

}