package com.yy.sdk.plugin

import com.android.build.gradle.internal.tasks.factory.TaskFactoryImpl
import com.yy.sdk.plugin.extension.ABoosterExtension
import com.yy.sdk.plugin.task.CleanTask
import com.yy.sdk.plugin.task.ReportTask

/**
 * Created by nls on 2020/8/22.
 * Nothing.
 */
class RootPlugin extends BasePlugin {

    @Override
    void createTaskManager(TaskFactoryImpl taskFactory) {

    }

    @Override
    void createExtension() {
        project.extensions.create("ABooster", ABoosterExtension)
    }

    @Override
    void createTask() {
        project.tasks.create(
                name: "cleanBoosterBundle",
                type: CleanTask)

        project.tasks.create(
                name: "generateReport",
                type: ReportTask)
    }

    @Override
    void configureProject() {
        project.afterEvaluate {
            project.subprojects {
                it.apply plugin: BuilderPlugin
            }
        }
    }
}
