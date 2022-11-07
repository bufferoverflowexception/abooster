package com.yy.sdk.plugin.utils

import org.gradle.api.Project

/**
 * Created by nls on 2020/8/26.
 * Nothing.
 */
class PathUtils {

    public static final String ABOOSTER_OUT_DIR = "/intermediates/abooster/"

    public static String getWorkspace(Project project) {
        return project.buildDir.absolutePath + ABOOSTER_OUT_DIR
    }

    public static String getWorkspace(String project) {
        return project + "build" + ABOOSTER_OUT_DIR
    }

    public static File getOutDir(Project project, String dir) {
        String output = project.buildDir.absolutePath + ABOOSTER_OUT_DIR
        if (dir != null && !dir.isEmpty()) {
            output += (File.separator + dir)
        }
        return getOrCreateDir(output)
    }

    public static File getOrCreateDir(String dir) {
        File file = new File(dir)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }
}
