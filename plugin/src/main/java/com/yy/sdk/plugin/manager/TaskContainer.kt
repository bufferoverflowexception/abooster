package com.yy.sdk.plugin.manager

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Create by nls on 2022/7/22
 * description: TaskContainer
 */
interface TaskContainer {

    val buildKotlinTask: TaskProvider<out Task>
    val buildJavaTask: TaskProvider<out Task>
    val buildJarTask: TaskProvider<out Task>
    val buildBundleTask: TaskProvider<out Task>
    val installBundleTask: TaskProvider<out Task>
}