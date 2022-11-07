package com.yy.sdk.plugin.task

import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.DefaultTask

/**
 * Created by nls on 2020/9/13.
 * Nothing.
 */
abstract class ABoosterTask extends DefaultTask {

    ABoosterTask() {
        group = "abooster"
    }

    File getTmpDirectory() {
        return PathUtils.getOutDir(project, "tmp")
    }
}
