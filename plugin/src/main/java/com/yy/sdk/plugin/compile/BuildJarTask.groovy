package com.yy.sdk.plugin.compile

import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.manager.TaskManger
import com.yy.sdk.plugin.task.ABoosterTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Incubating
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.annotations.NotNull

import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by nls on 2020/9/6.
 * Nothing.
 */
@CacheableTask
class BuildJarTask extends /*Jar*/ ABoosterTask implements VariantAwareTask {

    String variantName
    def findClass = false

    @Incubating
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    List<String> workDirs = new ArrayList<>()

    @OutputDirectory
    String getOutDir() {
        return PathUtils.getOutDir(project, "jar")
    }

    @TaskAction
    void buildJarAction(IncrementalTaskInputs inputs) {
        println "executeTask task name: ${name}, incremental: ${inputs.incremental}"
        if (!inputs.incremental) {
            FileUtils.deleteDir(getOutDir())
            return
        }
        String output = getOutDir() + File.separator
        for (int i = 0; i < workDirs.size(); i++) {
            findClass = false
            def outJar = output + i + ".jar"
            compress(workDirs[i], outJar)
            if (!findClass) {
                FileUtils.deleteFile(outJar)
            }
        }
    }

    static final int BUFFER = 8192;

    void compress(String srcPath, String dstPath) throws IOException {
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.exists()) {
            println "$srcPath not found"
            return
        }

        FileOutputStream out = null;
        ZipOutputStream zipOut = null;
        try {
            out = new FileOutputStream(dstFile);
            CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32());
            zipOut = new ZipOutputStream(cos);
            String baseDir = "";
            compress(srcFile, zipOut, baseDir, false);
        }
        finally {
            if (null != zipOut) {
                zipOut.close();
                out = null;
            }

            if (null != out) {
                out.close();
            }
        }
    }

    void compress(File file, ZipOutputStream zipOut, String baseDir, Boolean append) throws IOException {
        if (file.isDirectory()) {
            compressDirectory(file, zipOut, baseDir, append);
        } else {
            compressFile(file, zipOut, baseDir);
        }
    }

    /** 压缩一个目录 */
    private void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir, Boolean append) throws IOException {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            compress(files[i], zipOut, append ? baseDir + dir.getName() + "/" : baseDir, true);
        }
    }

    /** 压缩一个文件 */
    private void compressFile(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (file.exists() && FileUtils.hasExtension(file, ".class")) {
            findClass = true
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                ZipEntry entry = new ZipEntry(baseDir + file.getName());
                zipOut.putNextEntry(entry);
                int count;
                def data = new byte[BUFFER];
                while ((count = bis.read(data, 0, BUFFER)) != -1) {
                    zipOut.write(data, 0, count);
                }

            } finally {
                if (null != bis) {
                    bis.close();
                }
            }
        }
    }

    static class BuildJarTaskAction extends BaseTaskCreationAction<BuildJarTask> {


        BuildJarTaskAction(@NotNull VariantScope variantScope, @NotNull TaskManger taskManger) {
            super(taskManger, variantScope)
        }

        @Override
        void configure(@NotNull BuildJarTask task) {
            super.configure(task)
            String javaOutputs = taskManger.buildJavaTask().get().outDir
            String kotlinOutputs = taskManger.buildKotlinTask().get().outDir
            task.dependsOn(taskManger.buildJavaTask().get())
            task.workDirs.add(javaOutputs)
            task.workDirs.add(kotlinOutputs)
        }

        @Override
        String getName() {
            return variantScope.getTaskName("build", "Jar")
        }

        @Override
        Class<BuildJarTask> getType() {
            return BuildJarTask.class
        }
    }
}
