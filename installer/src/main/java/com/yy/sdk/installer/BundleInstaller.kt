package com.yy.sdk.installer

import android.app.Application
import com.yy.sdk.installer.loader.SystemClassLoaderAdder
import com.yy.sdk.installer.loader.TinkerResourcePatcher
import com.yy.sdk.installer.utils.SharePatchFileUtil
import dalvik.system.PathClassLoader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Created by nls on 2020/9/8.
 * Nothing.
 */
class BundleInstaller(private val app: Application) {

    fun install(bundle: File, dir: String) {
        val destFile = File(dir)
        if (!destFile.exists()) {
            destFile.mkdirs()
        } else {
            SharePatchFileUtil.deleteDir(dir)
        }
        var isSuccess = false
        var inputStream: InputStream? = null
        try {
            val zipFile = ZipFile(bundle)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement()
                val zipName = zipEntry.name
                inputStream = zipFile.getInputStream(zipEntry)
                val file = File(destFile, zipName)
                if (file.isDirectory) {
                    file.mkdirs()
                } else {
                    val parent = file.parentFile
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                    file.createNewFile()
                    writeStreamToFile(inputStream, file)
                }
                inputStream.close()
            }
            isSuccess = true
            bundle.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        if (isSuccess) {
            tryLoad(dir)
        }
    }


    fun tryLoad(dir: String) {
        val patchFile = File(dir)
        if (!patchFile.exists()) return
        patchFile.walk().filter { it.isFile }.forEach {
            if (it.absolutePath.endsWith(".dex")) {
                loadDexPatch(it)
            } else if (it.absolutePath.endsWith(".ap_")) {
                loadResourcePatch(it)
            }
        }
    }

    private fun loadDexPatch(dexFile: File) {
        val classLoader = app.classLoader as PathClassLoader
        val dexOptDir = SharePatchFileUtil.getPatchDirectory(app)
        if (!dexOptDir.exists()) {
            dexOptDir.mkdirs()
        }
        //copy from tinker
        SystemClassLoaderAdder.installDexes(classLoader, dexOptDir, arrayOf(dexFile).toList())
    }

    private fun loadResourcePatch(resourceFile: File) {
        //copy from tinker
        TinkerResourcePatcher.isResourceCanPatch(app)
        TinkerResourcePatcher.monkeyPatchExistingResources(app, resourceFile.absolutePath)
    }

    private fun writeStreamToFile(stream: InputStream, file: File) {
        try {
            val fos = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var len: Int
            var total = 0
            while ((stream.read(buffer).also { len = it }) != -1) {
                fos.write(buffer, 0, len)
                total += len
            }

            fos.flush()
            fos.close()
            stream.close()
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
    }
}