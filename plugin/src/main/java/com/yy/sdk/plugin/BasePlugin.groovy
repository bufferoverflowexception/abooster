package com.yy.sdk.plugin

import com.android.build.gradle.internal.tasks.factory.TaskFactoryImpl
import com.yy.sdk.plugin.manager.TaskManger
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by nls on 2020/8/23.
 * Nothing.
 */
abstract class BasePlugin implements Plugin<Project> {

    protected Project project
    protected TaskFactoryImpl taskFactory
    protected TaskManger taskManger

    @Override
    void apply(Project project) {
        this.project = project
        taskFactory = new TaskFactoryImpl(project.getTasks())
        createTaskManager(taskFactory)
        createExtension()
        createTask()
        configureProject()
    }

    abstract void createTaskManager(TaskFactoryImpl taskFactory)

    abstract void createExtension()

    abstract void createTask()

    abstract void configureProject()
}
