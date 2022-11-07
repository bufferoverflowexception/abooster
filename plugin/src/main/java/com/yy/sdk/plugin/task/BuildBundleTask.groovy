package com.yy.sdk.plugin.task

import com.android.annotations.NonNull
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.google.common.io.Files
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.manager.TaskManger
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.annotations.NotNull

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by nls on 2020/8/23.
 * Nothing.
 */
class BuildBundleTask extends ABoosterTask implements VariantAwareTask {

    String variantName

    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    FileCollection inputDexPath


    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    FileCollection inputResourcePath

    @OutputDirectory
    String getOutDir() {
        return PathUtils.getOutDir(project, "out")
    }


    @TaskAction
    protected void doBuildAction(IncrementalTaskInputs inputs) {
        println "executeTask task name: ${name}, incremental: ${inputs.incremental}"
//        if (!inputs.incremental) {
//            return
//        }
        File properties = new File(getOutDir(), "properties")
        if (properties.exists()) {
            properties.delete()
        }
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(properties))
        BundleBuilder builder = new BundleBuilder(new File(getOutDir(), "bundle.o"))
        try {
            addDexToBundle(builder)
            addResourceToBundle(builder)
            builder.add(properties.getParentFile(), properties)
        } finally {
            builder.close()
            outputStream.close()
        }
    }

    private void addDexToBundle(BundleBuilder builder) {
        File inputDir = new File(PathUtils.getWorkspace(project))
        inputDexPath.asFileTree.each {
            if (it.isFile() && it.getPath().endsWith(".dex")) {
                builder.add(inputDir, it)
            }
        }
    }

    private void addResourceToBundle(BundleBuilder builder) {
        File inputDir = new File(PathUtils.getWorkspace(project))
        inputResourcePath.asFileTree.each {
            if (it.isFile() && it.getPath().endsWith(".ap_")) {
                builder.add(inputDir, it)
            }
        }
    }

    class BundleBuilder implements Closeable {

        final File outputFile
        boolean empty = true
        ZipOutputStream zipOutputStream

        public BundleBuilder(@NonNull File out) {
            outputFile = out
        }

        public void add(File inputDir, File inputFile) {
            if (!inputFile.exists()) {
                return
            }
            if (zipOutputStream == null) {
                zipOutputStream = new ZipOutputStream(
                        new BufferedOutputStream(new FileOutputStream(outputFile)))
            }
            empty = false
            String entryName = inputFile.getPath().substring(inputDir.getPath().length() + 1)
            ZipEntry entry = new ZipEntry(entryName)
            zipOutputStream.putNextEntry(entry)
            Files.copy(inputFile, zipOutputStream)
            zipOutputStream.closeEntry()
        }

        @Override
        void close() throws IOException {
            if (zipOutputStream != null) {
                zipOutputStream.close()
            }
        }
    }

    static class BuildBundleTaskAction extends BaseTaskCreationAction<BuildBundleTask> {


        BuildBundleTaskAction(@NotNull TaskManger taskManger,
                              @NotNull VariantScope variantScope) {
            super(taskManger, variantScope)
        }

        @Override
        void preConfigure(@NotNull String taskName) {
            super.preConfigure(taskName)
        }

        @Override
        void configure(@NotNull BuildBundleTask task) {
            super.configure(task)
            Project project = task.project
            task.inputDexPath = project.files(taskManger.transformClassToDexTask().get().getDexOutputDir())
            task.inputResourcePath = project.files(taskManger.linkResTask().get().getOutputFile())
            task.dependsOn(taskManger.transformClassToDexTask().get())
            task.dependsOn(taskManger.linkResTask().get())
            task.finalizedBy(taskManger.installBundleTask().get())
            task.outputs.upToDateWhen { false }
        }

        @Override
        String getName() {
            return variantScope.getTaskName("build", "BoosterBundle")
        }

        @Override
        Class<BuildBundleTask> getType() {
            return BuildBundleTask.class
        }
    }
}
