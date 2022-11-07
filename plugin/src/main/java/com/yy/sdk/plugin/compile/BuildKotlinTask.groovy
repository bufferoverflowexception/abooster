package com.yy.sdk.plugin.compile

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.VariantScope
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.compile.ktcompiler.ChangedFilesKt
import com.yy.sdk.plugin.compile.ktcompiler.CompileOptions
import com.yy.sdk.plugin.compile.ktcompiler.KotlinCompilerArgumentsContributor
import com.yy.sdk.plugin.compile.ktcompiler.KotlinCompilerRunner
import com.yy.sdk.plugin.manager.TaskManger
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributorKt
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.incremental.ChangedFiles

/**
 * Created by nls on 2020/8/29.
 * Nothing.
 */
@CacheableTask
class BuildKotlinTask extends AbsBuildTask {

    private FilteringSourceRootsContainer sourceRootsContainer
    List<String> sourceFilesExtensions = new ArrayList<>()
    CompilerArgumentsContributor compilerArgumentsContributor
    String javaPackagePrefix
    List<File> computedCompilerClasspath
    String buildHistory

    BuildKotlinTask() {
        sourceRootsContainer = new FilteringSourceRootsContainer()

        sourceFilesExtensions.add("kt")
        sourceFilesExtensions.add("kts")
        sourceFilesExtensions.add("java")
    }

    FileCollection commonSourceSet

    public SourceRoots.ForJvm getSourceRoots() {
        //return SourceRoots.ForJvm.Companion.create(getSource(), sourceRootsContainer, sourceFilesExtensions)
        SourceRoots.ForJvm.Companion companion = new SourceRoots.ForJvm.Companion()
        return companion.create(getSource(), sourceRootsContainer, sourceFilesExtensions)
    }

    @Override
    void setSource(Object source) {
        sourceRootsContainer.add(source)
        super.setSource(source)
    }

    @Override
    void setSource(FileTree source) {
        sourceRootsContainer.set(source)
        super.setSource(source)
    }

    @Override
    SourceTask source(Object... sources) {
        sourceRootsContainer.add(sources)
        return super.source(sources)
    }

    @Override
    protected String getSourceName() {
        return "kotlin"
    }

    @Override
    File getCompileInfoFile() {
        new File(getTmpDirectory(), "build.kt")
    }

    @Override
    @OutputDirectory
    String getOutDir() {
        return PathUtils.getWorkspace(project) + "kotlin" + File.separator + variantName.toLowerCase()
    }

    @Override
    public void compile(IncrementalTaskInputs inputs) {
        ChangedFiles changedFiles = ChangedFilesKt.ChangedFiles(inputs)
        //K2JVMCompilerArguments args = new K2JVMCompilerArguments()
        //args.destination = getOutDir()
        //args.allowNoSourceFiles = true
        //args.compileJava = true
        //args.buildFile = changedFiles.modified
        //K2JVMCompiler compiler = new K2JVMCompiler()

        SourceRoots.ForJvm sourceRoots = getSourceRoots()
        List<File> allKotlinSources = sourceRoots.kotlinSourceFiles
        if (allKotlinSources.isEmpty() || ((ChangedFiles.Known) changedFiles).modified.isEmpty()) {
            // Skip running only if non-incremental run. Otherwise, we may need to do some cleanup.
            println "No Kotlin files found, skipping Kotlin compiler task"
            return
        }

        writeCompileInfo(((ChangedFiles.Known) changedFiles).modified, false)
        K2JVMCompilerArguments args = new K2JVMCompilerArguments()
        setupCompilerArgs(args, false, true)
        args.destination = getOutDir()
        args.allowNoSourceFiles = true
        args.useFir = true
        insertExtendClasspath(args)
        callCompilerAsync(args, sourceRoots, changedFiles)
    }

    void callCompilerAsync(K2JVMCompilerArguments args, SourceRoots.ForJvm sourceRoots, ChangedFiles changedFiles) {
        if (computedCompilerClasspath == null) {
            project.configurations.getByName("kotlinCompilerClasspath") {
                computedCompilerClasspath = it.resolve().toList()
            }
        }

        def modified = ((ChangedFiles.Known) changedFiles).modified
        println "modified: ${modified.toString()}"
        //不能用自己的history cache这会导致第一次的构建走了全量.
        //File buildHistory = new File(getOutDir(), "build-history.bin")
        File buildHistory = new File(buildHistory)
        MultiModuleICSettings multiICSetting = new MultiModuleICSettings(buildHistory, true)
        IncrementalCompilationEnvironment env = new IncrementalCompilationEnvironment(
                changedFiles,
                buildHistory.getParentFile(),
                false,
                false,
                multiICSetting)

        List<String> allSourceSet = new ArrayList<>()
        allSourceSet.addAll(commonSourceSet)
        allSourceSet.addAll(sourceRoots.kotlinSourceFiles)
        GradlePrintingMessageCollector messageCollector = new GradlePrintingMessageCollector(logger, false)
        OutputItemsCollectorImpl outputItemCollector = new OutputItemsCollectorImpl()

        //遇到一些奇奇怪怪的编译报错问题,譬如A.kt依赖了B.kt而B.kt又是继承M.java的,此时如果单编译A.kt时会报错.
        //同时编译A.kt跟B.kt就不会报错,另外除了继承以为还发现如果A.kt调用了B.kt的某个方法 方法中有java类型的参数
        //也报类似的错误, 时间关系先不去研究了,先直接调用系统内部的编译去解决这个问题先.
        KotlinCompilerRunner compilerRunner = new KotlinCompilerRunner(new GradleCompileTaskProvider(this))
        GradleCompilerEnvironment environment = new GradleCompilerEnvironment(
                computedCompilerClasspath, messageCollector, outputItemCollector,
                project.files(getOutDir()), new ReportingSettings(), env,
                sourceFilesExtensions.toArray(new String[sourceFilesExtensions.size()]))
        compilerRunner.runJvmCompilerAsync(
                sourceRoots.kotlinSourceFiles /*modified*/,
                allSourceSet.toList(),
                sourceRoots.javaSourceRoots,
                javaPackagePrefix,
                args,
                environment
        )
    }


    void setupCompilerArgs(K2JVMCompilerArguments args, Boolean defaultsOnly, Boolean ignoreClasspathResolutionErrors) {
        compilerArgumentsContributor.contributeArguments(args, CompilerArgumentsContributorKt.compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
        ))
    }

    private void insertExtendClasspath(K2JVMCompilerArguments args) {
        //args.classpath += (File.pathSeparator + "${project.buildDir}/tmp/kotlin-classes/$variantName")
        args.classpath += (File.pathSeparator + getOutDir())
        Set<String> classpath = getExtendClasspath()
        String newClasspath = ""
        classpath.forEach { path ->
            if (newClasspath.isEmpty()) {
                newClasspath = path
            } else {
                newClasspath += (File.pathSeparator + path)
            }
        }
        if (!newClasspath.isEmpty()) {
            newClasspath += (File.pathSeparator + args.classpath)
            args.classpath = newClasspath
        }
    }

    static class BuildKotlinTaskAction extends BaseTaskCreationAction<BuildKotlinTask> {

        private FileCollection classpath
        private FileCollection bootClasspath
        private List<ConfigurableFileTree> fileTrees
        private String sourceCompatibility
        private Set<String> runtimeLibrary

        BuildKotlinTaskAction(@NotNull List<ConfigurableFileTree> fileTrees,
                              @NotNull VariantScope variantScope, @NotNull TaskManger taskManger) {
            super(taskManger, variantScope)
            this.fileTrees = fileTrees
        }

        @Override
        void preConfigure(@NotNull String taskName) {
            super.preConfigure(taskName)
            classpath = variantScope.getJavaClasspath(
                    AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                    AndroidArtifacts.ArtifactType.CLASSES, null)
            def compileOptions = variantScope.globalScope.extension.compileOptions
            this.sourceCompatibility = compileOptions.sourceCompatibility.toString()
            bootClasspath = variantScope.bootClasspath
            Set<File> jarFile = variantScope.getArtifactCollection(
                    AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.MODULE,
                    AndroidArtifacts.ArtifactType.CLASSES).getArtifactFiles().getFiles()

            Set<String> setJar = new HashSet<>()
            jarFile.forEach {
                String path = it.getAbsolutePath()
                setJar.add(path.substring(0, path.indexOf("build/intermediates")))
            }
            runtimeLibrary = setJar
        }

        @Override
        void configure(@NotNull BuildKotlinTask task) {
            super.configure(task)
            Project project = task.project
            task.classpath = classpath
            task.bootClasspath = bootClasspath
            task.computedCompilerClasspath = project.configurations.getByName(
                    "kotlinCompilerClasspath").resolve().toList()
            task.setSource(project.android.sourceSets.main.java.srcDirs)
            task.commonSourceSet = project.android.sourceSets.main.java.sourceFiles
            fileTrees.forEach {
                task.source(it.dir)
            }
            task.aboosterRunTimeLib = runtimeLibrary
            task.compilerArgumentsContributor = new KotlinCompilerArgumentsContributor(
                    createCompileOptions())
            task.dependsOn(taskManger.buildResTask().get())
            task.buildHistory = "${project.buildDir}/kotlin/" +
                    "${variantScope.getTaskName("compile", "Kotlin")}/build-history.bin"
            //task.stableSources = task.project.file(source)
        }

        @Override
        String getName() {
            return variantScope.getTaskName("build", "Kotlin")
        }

        @Override
        Class<BuildKotlinTask> getType() {
            return BuildKotlinTask.class
        }

        private CompileOptions createCompileOptions() {
            CompileOptions compileOptions = new CompileOptions()
            compileOptions.bootClasspath = bootClasspath
            compileOptions.classpath = classpath
            compileOptions.sourceCompatibility = sourceCompatibility
            return compileOptions
        }
    }
}
