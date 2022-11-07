package com.yy.sdk.plugin.task

import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Created by nls on 2020/9/13.
 * Nothing.
 */
class ReportTask extends ABoosterTask {

    @TaskAction
    void reportAction() {
        println "-------------- generate compile report --------------"
        project.allprojects { project ->
            def msg = readProjectCompileInfo(project)
            if (!msg.isEmpty()) {
                println msg
            }
        }
        println "-------------- generate compile report finish --------------"
    }

    String readProjectCompileInfo(Project project) {
        File kt = new File(PathUtils.getOutDir(project, "tmp"), "build.kt")
        File java = new File(PathUtils.getOutDir(project, "tmp"), "build.java")
        String msg = FileUtils.readFile(kt)
        if (!msg.isEmpty()) {
            msg += System.lineSeparator()
        }
        msg += FileUtils.readFile(java)
        return msg
    }
}
