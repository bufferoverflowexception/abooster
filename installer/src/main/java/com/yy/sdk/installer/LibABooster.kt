package com.yy.sdk.installer

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Created by nls on 2020/8/23.
 * Nothing.
 */
class LibABooster {

    companion object {
        private const val TAG = "LibABooster"
        private const val PATH = "abooster"
        private const val BUNDLE_FILE = "bundle.o"
        private const val PATCH_DIR = "patch"


        //路径例如： /SD卡/Android/data/程序的包名/cache/abooster
        private fun getDiskCacheDir(context: Context, uniqueName: String): File {
            val cachePath: String = if (Environment.MEDIA_MOUNTED == Environment
                    .getExternalStorageState() || !Environment.isExternalStorageRemovable()
            ) {
                context.externalCacheDir!!.path
            } else {
                context.cacheDir.path
            }
            return File(cachePath + File.separator + uniqueName)
        }

        fun initialize(app: Application) {
            val workspace = getDiskCacheDir(app, PATH)
            if (!workspace.exists()) {
                workspace.mkdirs()
            }
            val patchDir = File(workspace, PATCH_DIR)
            val bundleFile = File(workspace, BUNDLE_FILE)
            when {
                bundleFile.exists() -> {
                    Log.d(TAG, "initialize: install patch file...")
                    BundleInstaller(app).install(bundleFile, patchDir.absolutePath)
                }
                patchDir.exists() -> {
                    Log.d(TAG, "initialize: try load patch file..")
                    BundleInstaller(app).tryLoad(patchDir.absolutePath)
                }
                else -> {
                    Log.d(TAG, "initialize: do nothing...")
                }
            }
        }
    }
}