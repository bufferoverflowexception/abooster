package com.yy.sdk.plugin.compile

import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileType
import org.gradle.api.model.ReplacedBy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges

import java.util.concurrent.Callable

/**
 * Created by nls on 2020/8/29.
 * Nothing.
 */
abstract class AbsBuildTask extends SourceTask implements VariantAwareTask {

    String variantName
    Set<String> aboosterRunTimeLib
    FileCollection classpath
    FileCollection bootClasspath
    private final FileCollection stableSources = getProject().files(new Callable<FileTree>() {
        @Override
        public FileTree call() {
            return getSource()
        }
    });

    AbsBuildTask() {
        group = "abooster"
    }

    abstract String getOutDir()

    /**
     * The sources for incremental change detection.
     *
     * @since 6.0
     */
    @Incubating
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    protected FileCollection getStableSources() {
        return stableSources;
    }

    @Override
    @ReplacedBy("stableSources")
    public FileTree getSource() {
        return super.getSource();
    }


    File getTmpDirectory() {
        return PathUtils.getOutDir(project, "tmp")
    }

    File getCompileInfoFile() {
        return new File(getTmpDirectory(), "build.info")
    }

    final List<String> getModified(InputChanges inputs, String suffix) {
        final List<File> modified = new ArrayList()
        inputs.getFileChanges(getSource()).each { change ->
            if (change.fileType == FileType.DIRECTORY) {
                return
            }
            if (change.changeType == ChangeType.MODIFIED ||
                    change.changeType == ChangeType.ADDED) {
                if (FileUtils.hasExtension(change.file, suffix)) {
                    modified.add(change.file)
                }
            }
        }
        return modified
    }

    final List<String> getRemoved(InputChanges inputs, String suffix) {
        return getInputChangeFile(inputs, ChangeType.REMOVED, suffix);
    }

    final List<String> getAdd(InputChanges inputs, String suffix) {
        return getInputChangeFile(inputs, ChangeType.ADDED, suffix);
    }

    final List<String> getInputChangeFile(InputChanges inputs, ChangeType fileType, String suffix) {
        final List<File> changeFiles = new ArrayList()
        inputs.getFileChanges(getStableSources()).each { change ->
            if (change.fileType == FileType.DIRECTORY) {
                return
            }
            if (change.changeType == fileType) {
                if (FileUtils.hasExtension(change.file, suffix)) {
                    changeFiles.add(change.file)
                }
            }
        }
        return changeFiles
    }

    @TaskAction
    protected void executeTask(IncrementalTaskInputs inputs) {
        println "executeTask task name: ${name}, incremental: ${inputs.incremental}"
        if (inputs.incremental) {
            compile(inputs)
        } else {
            FileUtils.deleteDir(getOutDir())
            File build = getCompileInfoFile()
            if (!build.exists()) {
                build.createNewFile()
            }
            FileWriter writer = new FileWriter(build)
            writer.append("detect ${getSourceName()} source code change but incremental: false, project: $project.name")
            writer.flush()
            writer.close()
        }
    }

    void writeCompileInfo(Collection<String> klass, boolean append) {
        StringBuilder sb = new StringBuilder()
        sb.append("detect ${getSourceName()} source code change and incremental: true, project: $project.name")
        sb.append(System.lineSeparator())
        klass.forEach {
            sb.append("source file: $it")
            sb.append(System.lineSeparator())
        }
        File build = getCompileInfoFile()
        FileUtils.writeFile(build, sb.toString())
    }

    Set<String> getExtendClasspath() {
        //这样写拿直接依赖是不会有问题的,但是传递依赖就会有问题了
        //譬如A->B->C C里面创建新类,A去引用 由于C不在A的classpath里
        //这样就会报错了...
        Set<String> classpath = new ArrayList<>()
        Collection<String> inputs = project.configurations.aboosterClasspath.files.path
        for (int i = 0; i < inputs.size(); i++) {
            FileUtils.collectionFilesFromDir(classpath, inputs[i], ".jar")
        }

        if (aboosterRunTimeLib != null && !aboosterRunTimeLib.isEmpty()) {
            aboosterRunTimeLib.forEach {
                FileUtils.collectionFilesFromDir(classpath,
                        "${PathUtils.getWorkspace(it)}jar/", ".jar")
            }
        }
        return classpath
    }

    protected String getSourceName() {
        return "unknown"
    }

    abstract void compile(IncrementalTaskInputs inputs)
}
