package com.yy.sdk.plugin.task


import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.yy.sdk.plugin.utils.FileUtils
import com.yy.sdk.plugin.utils.PathUtils
import org.gradle.api.tasks.TaskAction

/**
 * Created by nls on 2020/9/8.
 * Nothing.
 */
class CleanTask extends ABoosterTask implements VariantAwareTask {
    String variantName

    String applicationId
    String launchActivity

    @TaskAction
    void cleanAction() {
        project.subprojects { subproject ->
            String workspace = PathUtils.getWorkspace(subproject)
            FileUtils.deleteFile(workspace)
        }
        def rootDir = project.rootDir
        def localProperties = new File(rootDir, "local.properties")
        String pkg = project.rootProject.ABooster.pkg
        String launchAct = project.rootProject.ABooster.launchActivity
        if (pkg.isEmpty()) {
            pkg = applicationId
        }
        if (launchAct.isEmpty()) {
            launchAct = launchActivity
        }
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream {
                instr -> properties.load(instr)
            }
            def sdkDir = properties.getProperty('sdk.dir')
            def adb = "$sdkDir/platform-tools/adb"
            def devicesOutPut = new ByteArrayOutputStream()
            project.exec {
                commandLine "$adb", "devices"
                ignoreExitValue true
                standardOutput = devicesOutPut
            }
            println "android devices ${devicesOutPut.toString()}"
            devicesOutPut.toString().trim().split("\n").eachWithIndex { String deviceString, int index ->
                if (index == 0) {
                    return
                }
                def deviceInfo = deviceString.split("\t")
                if (deviceInfo.length < 2 || deviceInfo[1] != "device") {
                    return
                }
                def device = deviceInfo[0]
                project.exec {
                    commandLine "$adb", "-s", "$device", "shell", "rm", "-rf",
                            "/sdcard/Android/data/$pkg/cache/abooster"
                    ignoreExitValue true
                }
                project.exec {
                    commandLine "$adb", "-s", "$device", "shell", "am", "force-stop", "${pkg}"
                    ignoreExitValue true
                }

                project.exec {
                    commandLine "$adb", "-s", "$device", "shell", "am", "start", "-n",
                            "${pkg}/${launchAct}"
                    ignoreExitValue true
                }
            }
        }
    }

//    static class CleanTaskAction extends BaseTaskCreationAction<CleanTask> {
//
//        InstallTaskAction(@NotNull TaskManger taskManger,
//                          @NotNull VariantScope variantScope) {
//            super(taskManger, variantScope)
//        }
//
//
//        @Override
//        void configure(@NotNull CleanTask task) {
//            super.configure(task)
//            InternalArtifactType taskInputType = variantScope.manifestArtifactType
//            File manifestFiles = new File(variantScope.artifacts.getFinalProduct(
//                    taskInputType).get().asFile, SdkConstants.ANDROID_MANIFEST_XML)
//            if (manifestFiles.exists()) {
//                //这样解析简单太多了.
//                ManifestData manifestData = AndroidManifestParser.parse(new FileWrapper(manifestFiles))
//                task.applicationId = manifestData.package
//                task.launchActivity = manifestData.launcherActivity
//            }
//        }
//
//
//        @Override
//        String getName() {
//            return variantScope.getTaskName("clean", "BoosterBundle")
//        }
//
//        @Override
//        Class<CleanTask> getType() {
//            return CleanTask.class
//        }
//    }
}
