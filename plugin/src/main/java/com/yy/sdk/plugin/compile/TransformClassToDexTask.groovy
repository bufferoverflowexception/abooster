package com.yy.sdk.plugin.compile

import com.android.annotations.NonNull
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.builder.dexing.ClassFileEntry
import com.android.builder.dexing.ClassFileInput
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.android.ide.common.blame.Message
import com.android.ide.common.blame.MessageReceiver
import com.android.ide.common.blame.ParsingProcessOutputHandler
import com.android.ide.common.blame.parser.DexParser
import com.android.ide.common.blame.parser.ToolOutputParser
import com.android.ide.common.process.ProcessOutputHandler
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.manager.ITaskContainer
import com.yy.sdk.plugin.task.ABoosterTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.annotations.NotNull

import java.nio.file.Path
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Create by nls on 2022/8/11
 * description: TransformClassToDex
 */
@CacheableTask
public class TransformClassToDexTask extends ABoosterTask implements VariantAwareTask {

    private static final LoggerWrapper loggerWrapper =
            LoggerWrapper.getLogger(TransformClassToDexTask.class)

    String variantName
    int minSdkVersion
    //DexOptions dexOptions
    //DexByteCodeConverter converter = null
    boolean enableDesugar
    FileCollection bootClasspath
    MessageReceiver messageReceiver

    File runtimeJar
    Set<String> runtimeLibrary

    @OutputDirectory
    File getDexOutputDir() {
        return PathUtils.getOutDir(project, "dex")
    }

    @Incubating
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    ImmutableList<File> getLibraryArtifact() {
        ImmutableCollection.Builder<File> builder = new ImmutableList.Builder()
        runtimeLibrary.forEach {
            File resFile = new File(getFullDepDirectory(it))
            if (resFile.exists()) {
                builder.add(resFile)
            }
        }
        if (runtimeJar.exists()) {
            builder.add(runtimeJar)
        }
        builder.build()
    }

    String getFullDepDirectory(String parent) {
        return parent + "build" + PathUtils.ABOOSTER_OUT_DIR + "jar"
    }

    @TaskAction
    protected void transform() {
        ProcessOutputHandler outputHandler =
                new ParsingProcessOutputHandler(
                        new ToolOutputParser(new DexParser(), Message.Kind.ERROR, loggerWrapper),
                        new ToolOutputParser(new DexParser(), loggerWrapper),
                        messageReceiver)
        //for test
        //Collection<File> inputs = new ArrayList<>()
        //inputs.add(new File("/Users/nls/Desktop/job/ABooster/app/build/intermediates/abooster/jar/0.zip"))
        //inputs.add(new File("/Users/nls/Desktop/job/ABooster/app/build/intermediates/abooster/jar/1.zip"))
        Collection<File> inputs = collectionFiles()
        if (!inputs.isEmpty()) {
            convertByteCode(toStream(collectionFiles()), Collections.emptyList(), getDexOutputDir())
        } else {
            println "could not find jar file in dir: " + getLibraryArtifact()
        }
    }

    protected void convertByteCode(Stream<ClassFileEntry> inputJar, List<Path> classpath, File outputFolder)
            throws IOException {
        ClassFileProviderFactory bootClasspathProvider =
                new ClassFileProviderFactory(getBootClasspath());
        ClassFileProviderFactory libraryClasspathProvider =
                new ClassFileProviderFactory(classpath)
        DexArchiveBuilder d8DexBuilder =
                DexArchiveBuilder.createD8DexBuilder(
                        minSdkVersion,
                        true,
                        bootClasspathProvider,
                        libraryClasspathProvider,
                        enableDesugar,
                        messageReceiver);
        d8DexBuilder.convert(inputJar, outputFolder.toPath(), false);
    }


    private Stream<ClassFileEntry> toStream(List<File> inputs) {
        List<ClassFileEntry> classFileEntries = new ArrayList<>()
        inputs.each {
            ClassFileInput input = ClassFileInputs.fromPath(it.toPath())
            def classFileEntryStream = input.entries(new Predicate<String>() {
                @Override
                boolean test(String s) {
                    return true
                }
            })
            classFileEntryStream.each {
                classFileEntries.add(it)
            }
        }
        return classFileEntries.stream()
    }


    private Collection<File> collectionFiles() {
        Collection<String> jarFilesPath = new ArrayList<>()
        Collection<File> jarFiles = new ArrayList<>()
        getLibraryArtifact().forEach {
            FileUtils.collectionFilesFromDir(jarFilesPath, it, ".jar")
        }

        jarFilesPath.forEach { path ->
            jarFiles.add(project.file(path))
        }

        return jarFiles
    }

    @NonNull
    private List<Path> getBootClasspath() {
        if (!enableDesugar) {
            return Collections.emptyList();
        }
        return bootClasspath.getFiles().stream().map(new Function<File, Path>() {
            @Override
            Path apply(File file) {
                return file.toPath()
            }
        }).collect(Collectors.toList())
    }

    static class TransformClassToDexAction extends
            BaseTaskCreationAction<TransformClassToDexTask> {

        private int minSdkVersion
        private boolean enableDesugar
        private FileCollection bootClasspath
        private MessageReceiver messageReceiver
        private Set<String> runtimeLibrary

        TransformClassToDexAction(@NotNull ITaskContainer taskManger,
                                  @NotNull VariantScope variantScope) {
            super(taskManger, variantScope)
        }

        @Override
        void preConfigure(@NotNull String taskName) {
            super.preConfigure(taskName)
            bootClasspath = variantScope.bootClasspath
            minSdkVersion = variantScope.getMinSdkVersion().getApiLevel()
            enableDesugar = variantScope.getJava8LangSupportType() == VariantScope.Java8LangSupport.D8
            messageReceiver = variantScope.getGlobalScope().androidBuilder.messageReceiver
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
        void configure(@NotNull TransformClassToDexTask task) {
            super.configure(task)
            task.minSdkVersion = minSdkVersion
            task.enableDesugar = enableDesugar
            task.messageReceiver = messageReceiver
            task.bootClasspath = bootClasspath
            task.runtimeLibrary = runtimeLibrary
            def jarTask = taskManger.buildJarTask().get()
            task.runtimeJar = new File(jarTask.getOutDir())
            task.dependsOn(jarTask)
        }

        @Override
        String getName() {
            return variantScope.getTaskName("transform", "ClassToDex")
        }

        @Override
        Class<TransformClassToDexTask> getType() {
            return TransformClassToDexTask.class
        }
    }
}
