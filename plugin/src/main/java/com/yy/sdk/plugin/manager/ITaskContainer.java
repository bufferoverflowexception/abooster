package com.yy.sdk.plugin.manager;

import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

/**
 * Create by nls on 2022/7/23
 * description: ITaskContainer
 */
public interface ITaskContainer {

    <T extends Task> TaskProvider<T> buildKotlinTask();

    <T extends Task> TaskProvider<T> buildJavaTask();

    <T extends Task> TaskProvider<T> buildJarTask();

    <T extends Task> TaskProvider<T> buildResTask();

    <T extends Task> TaskProvider<T> transformClassToDexTask();

    <T extends Task> TaskProvider<T> linkResTask();

    <T extends Task> TaskProvider<T> buildBundleTask();

    <T extends Task> TaskProvider<T> installBundleTask();
}
