package com.aricneto.twistytimer.utils

import android.content.res.Resources
import android.os.Environment
import androidx.annotation.RawRes
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Created by Ari on 24/03/2016.
 */
object StoreUtils {
    @JvmStatic
    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    @JvmStatic
    fun getStringFromRaw(res: Resources, @RawRes rawFile: Int): String {
        return try {
            res.openRawResource(rawFile).use { inputStream ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                var inputByte: Int
                while (inputStream.read().also { inputByte = it } != -1) {
                    byteArrayOutputStream.write(inputByte)
                }
                byteArrayOutputStream.toString()
            }
        } catch (e: IOException) {
            throw Error("Could not read from raw file")
        }
    }
}
