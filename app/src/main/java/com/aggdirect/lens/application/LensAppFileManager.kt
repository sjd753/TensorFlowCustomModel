package com.aggdirect.lens.application

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Base64
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Sajjad Mistri on 3/7/2018.
 */

object LensAppFileManager {

    @Synchronized
    fun makeAppDir(appName: String): File? {
        val storageDir = Environment.getExternalStorageDirectory()

        val appDir = File("$storageDir/$appName")

        var exists = appDir.exists()

        if (!exists)
            exists = appDir.mkdir()
        return if (exists) appDir else null

    }

    @Synchronized
    @Throws(IOException::class)
    fun createImageFile(appName: String): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"

        val appDir = makeAppDir(appName)

        return if (appDir != null) {
            File.createTempFile(
                    imageFileName, /* prefix */
                    ".jpg", /* suffix */
                    appDir      /* directory */
            )
        } else null
    }

    @Synchronized
    @Throws(Exception::class)
    fun copyFileToAppDir(appName: String, fileUri: Uri): File {
        val appDir = makeAppDir(appName)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "IMAGE_" + timeStamp + "_" + fileUri.lastPathSegment

        val saveFile = File(appDir, imageFileName)

        val inStream = FileInputStream(File(fileUri.path!!))
        val outStream = FileOutputStream(saveFile)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()

        return saveFile
    }

    @Synchronized
    @Throws(Exception::class)
    fun copyFileToCacheDir(fileUri: Uri): File {
        val cacheDir = Environment.getDownloadCacheDirectory()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "IMAGE_" + timeStamp + "_" + fileUri.lastPathSegment

        val saveFile = File(cacheDir, imageFileName)

        val inStream = FileInputStream(File(fileUri.path!!))
        val outStream = FileOutputStream(saveFile)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()

        return saveFile
    }

    fun getStringImage(bmp: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    fun getImageBytes(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        return baos.toByteArray()
    }

    fun getBytes(file: File): ByteArray {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return bytes
    }

    fun getBytesFully(file: File): ByteArray {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        try {
            val fis = FileInputStream(file.absolutePath)

            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bytes
    }

    fun getFileSizeInKB(file: File): Long {
        // Get length of file in bytes
        val fileSizeInBytes = file.length()
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        val fileSizeInKB = fileSizeInBytes / 1024
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        val fileSizeInMB = fileSizeInKB / 1024
        //
        return fileSizeInKB
    }
}
