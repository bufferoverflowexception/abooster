package com.yy.sdk.plugin.compile

import com.android.SdkConstants
import com.android.annotations.Nullable
import com.android.build.api.artifact.BuildableArtifact
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.api.artifact.BuildableArtifactUtil
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.dsl.AaptOptions
import com.android.build.gradle.internal.dsl.DslAdaptersKt
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.res.Aapt2MavenUtils
import com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerService
import com.android.build.gradle.internal.res.namespaced.Aapt2ServiceKey
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.TaskInputHelper
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.VariantType
import com.android.builder.internal.aapt.AaptPackageConfig
import com.android.builder.internal.aapt.v2.Aapt2DaemonManager
import com.android.sdklib.IAndroidTarget
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.manager.ITaskContainer
import com.yy.sdk.plugin.task.ABoosterTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Incubating
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.annotations.NotNull

import java.util.function.Supplier

/**
 * Created by nls on 2022/8/7
 * Description: LinkResTask
 */
@CacheableTask
public class LinkResTask extends ABoosterTask implements VariantAwareTask {

    String variantName
    AaptOptions aaptOptions
    File textSymbolOutputDir
    File sourceOutputDir
    File incrementalFolder
    VariantType variantType
    Provider<Directory> manifestFiles
    Supplier<String> originalApplicationId
    FileCollection dependenciesFileCollection
    FileCollection aapt2FromMaven;
    boolean processResource
    File runtimeResource
    Set<String> runtimeLibraryResource
    @Nullable
    AndroidBuilder androidBuilder
    BuildableArtifact inputResourcesDir

    @OutputDirectory
    String getOutDir() {
        return PathUtils.getWorkspace(project) + "res" + File.separator + "out"
    }

    @Incubating
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    ImmutableList<File> getLibraryArtifact() {
        ImmutableCollection.Builder<File> builder = new ImmutableList.Builder()
        runtimeLibraryResource.forEach {
            File resFile = new File(getFullDepDirectory(it))
            if (resFile.exists()) {
                builder.add(resFile)
            }
        }
        if (runtimeResource.exists()) {
            builder.add(runtimeResource)
        }
        builder.build()
    }

    @Incubating
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    private String getTmpDir() {
        return PathUtils.getWorkspace(project) + "res" + File.separator +
                "tmp" + File.separator + variantName.toLowerCase()
    }

    String getOutputFile() {
        return getOutDir() + File.separator + "out.ap_"
    }

    String getFullDepDirectory(String parent) {
        return parent + "build" + PathUtils.ABOOSTER_OUT_DIR +
                "res" + File.separator + variantName.toLowerCase()
    }

    /**
     * 资源的增量编译比较简单,但是要做资源的增量链接比较麻烦,因为涉及到了增量链接的依赖问题
     * 但本身搞增量资源包的意义不太大,因为增量资源包最终也得merge为全量资源包宿主才能使用,
     * 因此这里直接就链接成全量的资源包,宿主也能省去merge步骤,数据线传输的话包体大一点也没关系.
     */
    private void prepareFlatToLink() {
        File destDir = new File(getTmpDir())
        if (!destDir.exists()) {
            destDir.mkdirs()
            //FileUtils.deleteDir(getTmpDir())
        }

        //为了不影响原来的构建,我们这里把编译后的资源拷贝到一个临时目录来再做链接操作.
        if (destDir.listFiles().length <= 1) {
            // PATH: /Users/nls/Desktop/job/abooster/testlib/build/intermediates/res/merged/debug
            com.android.utils.FileUtils.copyDirectoryContentToDirectory(
                    BuildableArtifactUtil.singleFile(inputResourcesDir), destDir)
        }

        getLibraryArtifact().forEach {
            File[] files = it.listFiles()
            for (File f : files) {
                if (f.name.endsWith(".flat")) {
                    com.android.utils.FileUtils.copyFile(f, new File(destDir, "z_$f.name"))
                }
            }
        }
    }

    @TaskAction
    protected void doLink(IncrementalTaskInputs inputs) {
        println "executeTask task name: ${name}, incremental: ${inputs.incremental}"
        if (!inputs.incremental) {
            FileUtils.deleteFile(getOutputFile())
            FileUtils.deleteDir(getTmpDir())
            return
        }
        prepareFlatToLink()
        File manifest = new File(manifestFiles.get().asFile, SdkConstants.ANDROID_MANIFEST_XML)
        //File manifest = new File(getTmpDir() + File.separator + SdkConstants.ANDROID_MANIFEST_XML)
        AaptPackageConfig.Builder builder = new AaptPackageConfig.Builder()
                .setManifestFile(manifest)
                .setOptions(DslAdaptersKt.convert(aaptOptions))
                .setCustomPackageForR(originalApplicationId.get())
                .setSymbolOutputDir(textSymbolOutputDir)
        //.setSourceOutputDir(sourceOutputDir)
                .setResourceOutputApk(project.file(getOutputFile()))
                .setVariantType(variantType)
                .setDebuggable(true)
                .setResourceConfigs(ImmutableSet.of())
                .setSplits(Collections.emptyList())
                .setPreferredDensity(null)
                .setPackageId(null)
                .setAllowReservedPackageId(true)
                .setDependentFeatures(Collections.emptyList())
                .setImports(ImmutableList.of())
                .setIntermediateDir(incrementalFolder)
                .setAndroidJarPath(androidBuilder.target.getPath(IAndroidTarget.ANDROID_JAR))
                .setUseConditionalKeepRules(false)
                .setLibrarySymbolTableFiles(dependenciesFileCollection.files)
                .setResourceDir(new File(getTmpDir()))

        Aapt2ServiceKey aapt2ServiceKey =
                Aapt2DaemonManagerService.registerAaptService(
                        aapt2FromMaven, androidBuilder.getBuildToolInfo(), androidBuilder.getLogger())

        Aapt2DaemonManager.LeasedAaptDaemon aaptDaemon
        try {
            aaptDaemon =
                    Aapt2DaemonManagerService.getAaptDaemon(aapt2ServiceKey)
            AndroidBuilder.processResources(aaptDaemon, builder.build(),
                    new LoggerWrapper(Logging.getLogger(LinkResTask.class)))
        } finally {
            if (aaptDaemon != null) {
                aaptDaemon.close()
            }
        }
    }

    private boolean checkLinkResource() {
        boolean canLink = false
        ImmutableList<File> input = getInputFileDir()
        for (file in input) {
            if (file.isDirectory()) {
                File[] files = file.listFiles()
                canLink = files != null && files.length > 0
            }
            if (canLink) {
                break
            }
        }
        return canLink
    }

    static class LinkResTaskAction extends BaseTaskCreationAction<LinkResTask> {
        File sourceOutputDir
        boolean processResource

        LinkResTaskAction(@NotNull ITaskContainer taskManger,
                          @NotNull VariantScope variantScope,
                          boolean processResource) {
            super(taskManger, variantScope)
            this.processResource = processResource
        }

        @Override
        void preConfigure(@NotNull String taskName) {
            super.preConfigure(taskName)
//            sourceOutputDir = variantScope
//                    .artifacts
//                    .appendArtifact(
//                            InternalArtifactType.NOT_NAMESPACED_R_CLASS_SOURCES,
//                            taskName,
//                            "r")
        }

        @Override
        void configure(@NotNull LinkResTask task) {
            super.configure(task)
            BaseVariantData variantData = variantScope.variantData
            GradleVariantConfiguration config = variantData.variantConfiguration
            InternalArtifactType taskInputType = variantScope.manifestArtifactType
            boolean aaptFriendlyManifestsFilePresent = variantScope.artifacts
                    .hasFinalProduct(InternalArtifactType.AAPT_FRIENDLY_MERGED_MANIFESTS)
            if (aaptFriendlyManifestsFilePresent) {
                taskInputType = InternalArtifactType.AAPT_FRIENDLY_MERGED_MANIFESTS
            }
            task.variantType = config.type
            task.processResource = processResource
            //task.sourceOutputDir = sourceOutputDir
            task.incrementalFolder = variantScope.getIncrementalDir(name)
            task.androidBuilder = variantScope.globalScope.androidBuilder
            task.aaptOptions = variantScope.globalScope.extension.aaptOptions
            task.manifestFiles = variantScope.artifacts.getFinalProduct(taskInputType)
            task.originalApplicationId = TaskInputHelper.memoize(new Supplier<String>() {
                @Override
                String get() {
                    return config.originalApplicationId
                }
            })
            task.aapt2FromMaven = Aapt2MavenUtils.getAapt2FromMaven(variantScope.globalScope)
            task.textSymbolOutputDir = new File(variantScope.globalScope.getIntermediatesDir(),
                    "symbols/" + config.getDirName())
            task.inputResourcesDir = variantScope.artifacts
                    .getFinalArtifactFiles(InternalArtifactType.MERGED_RES)
            task.dependenciesFileCollection = variantScope.getArtifactFileCollection(
                    AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.ALL,
                    AndroidArtifacts.ArtifactType.SYMBOL_LIST_WITH_PACKAGE_NAME
            )
            Set<File> javaResFile = variantScope.getArtifactCollection(
                    AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.MODULE,
                    AndroidArtifacts.ArtifactType.JAVA_RES).getArtifactFiles().getFiles()

            Set<String> runtimeResource = new HashSet<>()
            javaResFile.forEach {
                String path = it.getAbsolutePath()
                runtimeResource.add(path.substring(0, path.indexOf("build/intermediates")))
            }
            def buildResTask = taskManger.buildResTask().get()
            task.runtimeLibraryResource = runtimeResource
            task.runtimeResource = new File(buildResTask.getOutDir())
            task.dependsOn(buildResTask)
        }

        @Override
        String getName() {
            return variantScope.getTaskName("link", "Resource")
        }

        @Override
        Class<LinkResTask> getType() {
            return LinkResTask.class
        }
    }
}
