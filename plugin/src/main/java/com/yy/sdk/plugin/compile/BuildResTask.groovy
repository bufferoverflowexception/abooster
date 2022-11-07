package com.yy.sdk.plugin.compile

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.internal.aapt.WorkerExecutorResourceCompilationService
import com.android.build.gradle.internal.res.Aapt2MavenUtils
import com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerService
import com.android.build.gradle.internal.res.namespaced.Aapt2ServiceKey
import com.android.build.gradle.internal.res.namespaced.NamespaceRemover
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.build.gradle.internal.tasks.Workers
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.core.AndroidBuilder
import com.android.builder.model.SourceProvider
import com.android.ide.common.resources.CompileResourceRequest
import com.android.ide.common.resources.CopyToOutputDirectoryResourceCompilationService
import com.android.ide.common.resources.ResourceCompilationService
import com.android.ide.common.workers.WorkerExecutorFacade
import com.google.common.collect.ImmutableSet
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.action.InputFileChangeAction
import com.yy.sdk.plugin.manager.ITaskContainer
import com.yy.sdk.plugin.task.ABoosterTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.workers.WorkerExecutor
import org.jetbrains.annotations.NotNull

import javax.inject.Inject
import java.util.function.Function
import java.util.function.Supplier

/**
 * Created by nls on 2022/8/6
 * Description: BuildResTask
 * AGP会先merge resource merge的产物会放到build/intermediates/incremental/mergeDebugResources/merged.dir
 * 目录下面,最后被送进去compile的resource是mergeDebugResources目录下的resource而不是原资源,这里我们省去merge这步
 * 理论上问题不大,但是如果出现了资源冲突的这种情况的话这里是检查不出来的.
 */
@CacheableTask
public class BuildResTask extends ABoosterTask implements VariantAwareTask {

    String variantName
    boolean processResource
    @Nullable
    AndroidBuilder androidBuilder;
    @Nullable
    FileCollection aapt2FromMaven;
    Supplier<Collection<File>> sourceFolderInputs;
    WorkerExecutorFacade workerExecutorFacade;

    @Inject
    public BuildResTask(WorkerExecutor workerExecutor) {
        this.workerExecutorFacade = Workers.INSTANCE.getWorker(workerExecutor);
    }

    @OutputDirectory
    String getOutDir() {
        return PathUtils.getWorkspace(project) + "res" + File.separator + variantName.toLowerCase()
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public Collection<File> getSourceFolderInputs() {
        return sourceFolderInputs.get();
    }

    @TaskAction
    protected void compile(IncrementalTaskInputs inputs) {
        println "executeTask task name: ${name}, incremental: ${inputs.incremental}"
        if (!inputs.incremental) {
            FileUtils.deleteDir(getOutDir())
            return
        }
        InputFileChangeAction changeAction = new InputFileChangeAction("")
        inputs.outOfDate(changeAction)
        List<File> files = changeAction.getAllChanged()
        if (files.isEmpty() || !inputs.incremental) {
            println "empty change file list with incremental task name: ${name}"
            return
        }

        ResourceCompilationService resourceCompiler =
                getResourceProcessor(
                        getBuilder(),
                        aapt2FromMaven,
                        workerExecutorFacade,
                        ImmutableSet.of(),
                        /*processResource*/ true)

        files.forEach {
            println "build resource with incremental file name: ${it.absolutePath}"
            resourceCompiler.submitCompile(new CompileResourceRequest(
                    it,
                    project.file(getOutDir())))
        }
        resourceCompiler.close()
    }

    protected AndroidBuilder getBuilder() {
        return androidBuilder;
    }

    @NonNull
    private static ResourceCompilationService getResourceProcessor(
            @NonNull AndroidBuilder builder,
            @Nullable FileCollection aapt2FromMaven,
            @NonNull WorkerExecutorFacade workerExecutor,
            ImmutableSet<MergeResources.Flag> flags,
            boolean processResources) {
        // If we received the flag for removing namespaces we need to use the namespace remover to
        // process the resources.
        if (flags.contains(MergeResources.Flag.REMOVE_RESOURCE_NAMESPACES)) {
            return NamespaceRemover.INSTANCE;
        }

        // If we're not removing namespaces and there's no need to compile the resources, return a
        // no-op resource processor.
        if (!processResources) {
            return CopyToOutputDirectoryResourceCompilationService.INSTANCE;
        }

        Aapt2ServiceKey aapt2ServiceKey =
                Aapt2DaemonManagerService.registerAaptService(
                        aapt2FromMaven, builder.getBuildToolInfo(), builder.getLogger());

        return new WorkerExecutorResourceCompilationService(workerExecutor, aapt2ServiceKey);
    }


    static class BuildResTaskAction extends BaseTaskCreationAction<BuildResTask> {

        boolean processResource

        BuildResTaskAction(@NotNull ITaskContainer taskManger,
                           @NotNull VariantScope variantScope,
                           boolean processResource) {
            super(taskManger, variantScope)
            this.processResource = processResource
        }

        @Override
        void configure(@NotNull BuildResTask task) {
            super.configure(task)
            BaseVariantData variantData = variantScope.getVariantData();
            task.processResource = processResource
            task.androidBuilder = variantScope.globalScope.androidBuilder
            task.aapt2FromMaven = Aapt2MavenUtils.getAapt2FromMaven(variantScope.globalScope);
            task.sourceFolderInputs = new Supplier() {
                @Override
                Object get() {
                    return variantData
                            .getVariantConfiguration()
                            .getSourceFiles(new Function<SourceProvider, Collection<File>>() {
                                @Override
                                Collection<File> apply(SourceProvider sourceProvider) {
                                    return sourceProvider.getResDirectories();
                                }
                            });
                }
            }
        }

        @Override
        String getName() {
            return variantScope.getTaskName("build", "Resource")
        }

        @Override
        Class<BuildResTask> getType() {
            return BuildResTask.class
        }
    }
}
