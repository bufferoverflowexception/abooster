package com.yy.sdk.plugin.manager

import com.android.build.gradle.api.SourceKind
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.factory.TaskFactory
import com.yy.sdk.plugin.compile.*
import com.yy.sdk.plugin.task.BuildBundleTask
import com.yy.sdk.plugin.task.InstallTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

/**
 * Create by nls on 2022/7/22
 * description: TaskManger
 */
abstract class TaskManger implements ITaskContainer {


    TaskFactory taskFactory;
    TaskProvider<LinkResTask> linkResTask;
    private TaskProvider<BuildBundleTask> buildBundleTask;
    private TaskProvider<InstallTask> installBundleTask;
    private TaskProvider<BuildKotlinTask> buildKotlinTask;
    private TaskProvider<BuildJavaTask> buildJavaTask;
    private TaskProvider<BuildJarTask> buildJarTask;
    private TaskProvider<BuildResTask> buildResTask;
    private TaskProvider<TransformClassToDexTask> transformClassToDexTaskTask;

    public TaskManger(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

//    @Override
//    def <T extends Task> TaskProvider<T> buildKotlinTask() {
//        return buildKotlinTask
//    }
//
//    @Override
//    def <T extends Task> TaskProvider<T> buildJavaTask() {
//        return buildJavaTask
//    }
//
//    @Override
//    def <T extends Task> TaskProvider<T> buildJarTask() {
//        return buildJarTask
//    }
//
//    @Override
//    def <T extends Task> TaskProvider<T> buildBundleTask() {
//        return buildBundleTask
//    }
//
//    @Override
//    def <T extends Task> TaskProvider<T> installBundleTask() {
//        return installBundleTask
//    }
//
    @Override
    public TaskProvider<BuildKotlinTask> buildKotlinTask() {
        return buildKotlinTask;
    }

    @Override
    public TaskProvider<BuildJavaTask> buildJavaTask() {
        return buildJavaTask;
    }

    @Override
    public TaskProvider<BuildJarTask> buildJarTask() {
        return buildJarTask;
    }

    @Override
    def <T extends Task> TaskProvider<T> buildResTask() {
        return buildResTask
    }

    @Override
    def <T extends Task> TaskProvider<T> transformClassToDexTask() {
        return transformClassToDexTaskTask
    }

    @Override
    def <T extends Task> TaskProvider<T> linkResTask() {
        return linkResTask
    }

    @Override
    public TaskProvider<BuildBundleTask> buildBundleTask() {
        return buildBundleTask;
    }

    @Override
    public TaskProvider<InstallTask> installBundleTask() {
        return installBundleTask;
    }

    abstract void createTaskForBooster(@NotNull Project project)

    public void createTaskForBooster(@NotNull def variant) {
        VariantScope variantScope = variant.variantData.scope
        createBuildResTask(variantScope);
        createBuildKotlinTask(variant);
        createBuildJavaTask(variantScope);
        createBuildJarTask(variantScope);
    }

    public void createAssembleTaskForBooster(@NotNull VariantScope variantScope) {
        createLinkResTask(variantScope);
        createDexTransformTask(variantScope);


        installBundleTask = taskFactory.register(
                new InstallTask.InstallTaskAction(this, variantScope)
        );

        buildBundleTask = taskFactory.register(
                new BuildBundleTask.BuildBundleTaskAction(this, variantScope)
        );
    }

    private void createBuildKotlinTask(@NotNull def variant) {
        VariantScope variantScope = variant.variantData.scope
        List<ConfigurableFileTree> javaSourceSet = variant.getSourceFolders(SourceKind.JAVA)
        buildKotlinTask = taskFactory.register(
                new BuildKotlinTask.BuildKotlinTaskAction(javaSourceSet, variantScope, this)
        );
    }

    private void createBuildJavaTask(@NotNull VariantScope variantScope) {
        buildJavaTask = taskFactory.register(
                new BuildJavaTask.BuildJavaTaskAction(variantScope, this)
        );
    }

    private void createBuildJarTask(@NotNull VariantScope variantScope) {
        buildJarTask = taskFactory.register(
                new BuildJarTask.BuildJarTaskAction(variantScope, this)
        );
    }

    private void createDexTransformTask(@NotNull VariantScope variantScope) {
        transformClassToDexTaskTask = taskFactory.register(
                new TransformClassToDexTask.TransformClassToDexAction(this, variantScope)
        );
    }

    private void createBuildResTask(@NotNull VariantScope variantScope) {
        buildResTask = taskFactory.register(
                new BuildResTask.BuildResTaskAction(this, variantScope, true)
        );
    }

    private void createLinkResTask(@NotNull VariantScope variantScope) {
        linkResTask = taskFactory.register(
                new LinkResTask.LinkResTaskAction(this, variantScope, false)
        );
    }
}