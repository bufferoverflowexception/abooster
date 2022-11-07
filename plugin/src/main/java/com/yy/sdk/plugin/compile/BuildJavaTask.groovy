package com.yy.sdk.plugin.compile

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.action.InputChangeAction
import com.yy.sdk.plugin.action.SourceFileChangeProcessor
import com.yy.sdk.plugin.manager.TaskManger
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileTreeInternal
import org.gradle.api.internal.tasks.JavaToolChainFactory
import org.gradle.api.internal.tasks.compile.CompileJavaBuildOperationReportingCompiler
import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec
import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpecFactory
import org.gradle.api.internal.tasks.compile.JavaCompileSpec
import org.gradle.api.internal.tasks.compile.incremental.recomp.CompilationSourceDirs
import org.gradle.api.internal.tasks.compile.incremental.recomp.RecompilationSpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.jvm.internal.toolchain.JavaToolChainInternal
import org.gradle.jvm.platform.JavaPlatform
import org.gradle.jvm.platform.internal.DefaultJavaPlatform
import org.gradle.jvm.toolchain.JavaToolChain
import org.gradle.language.base.internal.compile.Compiler
import org.gradle.language.base.internal.compile.CompilerUtil
import org.gradle.work.InputChanges
import org.jetbrains.annotations.NotNull

import javax.inject.Inject

/**
 * Created by nls on 2020/8/29.
 * Nothing.
 */
@CacheableTask
class BuildJavaTask extends AbsBuildTask {

    CompileOptions compileOptions
    JavaToolChain toolChain
    File destinationDir
    String sourceCompatibility = "1.8"
    String targetCompatibility = "1.8"
    String kotlinClasspath

    BuildJavaTask() {
        CompileOptions compileOptions = getServices().get(ObjectFactory.class).newInstance(CompileOptions.class);
        this.compileOptions = compileOptions;
    }

    @Override
    @OutputDirectory
    String getOutDir() {
        return PathUtils.getWorkspace(project) + "java" + File.separator + variantName.toLowerCase()
    }

    @Override
    protected String getSourceName() {
        return "java"
    }

    @Override
    File getCompileInfoFile() {
        new File(getTmpDirectory(), "build.java")
    }

    @Override
    public void compile(IncrementalTaskInputs inputs) {
        performCompilation(inputs)
    }

    @Nested
    protected JavaPlatform getPlatform() {
        return new DefaultJavaPlatform(JavaVersion.toVersion(getTargetCompatibility()))
    }

    @Nested
    public JavaToolChain getToolChain() {
        return toolChain != null ? toolChain : getJavaToolChainFactory().forCompileOptions(compileOptions)
    }

    @Inject
    protected JavaToolChainFactory getJavaToolChainFactory() {
        throw new UnsupportedOperationException();
    }

    private RecompilationSpec getRecompilationSpec(InputChanges inputs) {

    }

    private void performCompilation(IncrementalTaskInputs inputs) {
        InputChangeAction action = new InputChangeAction(new SourceFileChangeProcessor(false))
        inputs.outOfDate(action)
        RecompilationSpec recompilationSpec = action.getRecompilationSpec()
        if (recompilationSpec.classesToCompile.isEmpty()) {
            println "BuildJavaTask fail! classesToCompile must not be empty"
            return
        }
        writeCompileInfo(recompilationSpec.classesToCompile, false)
        DefaultJavaCompileSpec spec = createSpec()
        spec.setSourceFiles(pathToFile(recompilationSpec.classesToCompile))
        performCompilation(spec, createCompiler(spec))
    }

    private void performCompilation(JavaCompileSpec spec, Compiler<JavaCompileSpec> compiler) {
        WorkResult result = (new CompileJavaBuildOperationReportingCompiler(this, compiler, (BuildOperationExecutor) getServices().get(BuildOperationExecutor.class))).execute(spec)
        this.setDidWork(result.getDidWork());
    }

    private Compiler<JavaCompileSpec> createCompiler(JavaCompileSpec spec) {
        return CompilerUtil.castCompiler(((JavaToolChainInternal) getToolChain()).select(getPlatform()).newCompiler(spec.getClass()))
    }

    private DefaultJavaCompileSpec createSpec() {
        DefaultJavaCompileSpec spec = (DefaultJavaCompileSpec) (new DefaultJavaCompileSpecFactory(compileOptions)).create()
        File destDir = project.file(getOutDir())
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        spec.destinationDir = destDir
        spec.workingDir = project.projectDir
        spec.tempDir = temporaryDir
        spec.compileClasspath = classpath.toList()
        spec.annotationProcessorPath = compileOptions.getAnnotationProcessorPath() == null ?
                Collections.emptyList() : compileOptions.getAnnotationProcessorPath().toList()
        spec.targetCompatibility = sourceCompatibility
        spec.sourceCompatibility = targetCompatibility
        spec.compileOptions = compileOptions
        spec.sourceRoots = CompilationSourceDirs.inferSourceRoots((FileTreeInternal) getSource())
        insertExtendClasspath(spec)
        return spec
    }

    private List<File> pathToFile(Collection<String> path) {
        List<File> files = new ArrayList<>()
        path.forEach {
            files.add(new File(it))
        }
        return files
    }

    protected void includePreviousCompilationOutputOnClasspath(JavaCompileSpec spec, File... destinationDir) {
        List<File> classpath = new ArrayList<File>(spec.getCompileClasspath());
        for (int i = 0; i < destinationDir.size(); i++) {
            classpath.add(i, destinationDir[i]);
        }
        spec.setCompileClasspath(classpath);
    }

    private void insertExtendClasspath(JavaCompileSpec spec) {
        Set<String> classpath = getExtendClasspath()
        List<File> classpathFile = new ArrayList<>()
        classpath.forEach { path ->
            classpathFile.add(new File(path))
        }
        classpathFile.add(project.file(kotlinClasspath))
        classpathFile.add(project.file(getOutDir()))
        classpathFile.add(destinationDir)
        includePreviousCompilationOutputOnClasspath(spec, classpathFile.toArray(new File[classpathFile.size()]))
    }

    static class BuildJavaTaskAction extends BaseTaskCreationAction<BuildJavaTask> {

        //private CompileOptions compileOptions
        private String sourceCompatibility = "1.8"
        private String targetCompatibility = "1.8"
        private FileCollection classpath
        private FileCollection bootClasspath
        private File destinationDir
        private Set<String> runtimeLibrary

        BuildJavaTaskAction(@NotNull VariantScope variantScope, @NotNull TaskManger taskManger) {
            super(taskManger, variantScope)
        }

        @Override
        void preConfigure(@NotNull String taskName) {
            super.preConfigure(taskName)
            def compileOptions = variantScope.globalScope.extension.compileOptions
            this.sourceCompatibility = compileOptions.sourceCompatibility.toString()
            this.targetCompatibility = compileOptions.targetCompatibility.toString()
            classpath = variantScope.getJavaClasspath(
                    AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                    AndroidArtifacts.ArtifactType.CLASSES, null)
            bootClasspath = variantScope.bootClasspath
            String javaTaskName = variantScope.getTaskName("compile", "JavaWithJavac")
            destinationDir = variantScope.artifacts.appendArtifact(
                    InternalArtifactType.JAVAC, javaTaskName, "classes")

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
        void configure(@NotNull BuildJavaTask task) {
            super.configure(task)
            //task.compileOptions = this.compileOptions
            task.classpath = classpath
            task.bootClasspath = bootClasspath
            task.destinationDir = destinationDir
            task.sourceCompatibility = this.sourceCompatibility
            task.targetCompatibility = this.targetCompatibility
            task.setSource(task.project.android.sourceSets.main.java.srcDirs)
            task.dependsOn(taskManger.buildKotlinTask())
            task.compileOptions.bootstrapClasspath = bootClasspath
            task.aboosterRunTimeLib = runtimeLibrary
            task.kotlinClasspath = taskManger.buildKotlinTask().get().outDir
        }

        @Override
        String getName() {
            return variantScope.getTaskName("build", "Java")
        }

        @Override
        Class<BuildJavaTask> getType() {
            return BuildJavaTask.class
        }
    }
}
