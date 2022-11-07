package com.yy.sdk.plugin.task

import com.android.SdkConstants
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.TaskInputHelper
import com.android.build.gradle.internal.tasks.VariantAwareTask
import com.android.build.gradle.internal.variant.BaseVariantData
import com.yy.sdk.plugin.action.BaseTaskCreationAction
import com.yy.sdk.plugin.manager.TaskManger
import com.yy.sdk.plugin.utils.PathUtils
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskAction
import org.jetbrains.annotations.NotNull

import java.util.function.Supplier

/**
 * Created by nls on 2020/9/8.
 * Nothing.
 */
class InstallTask extends ABoosterTask implements VariantAwareTask {

    String variantName
    Supplier<String> applicationId
    String launchActivity

    @TaskAction
    void installAction() {
        println "Deploying patch file..."
        def pushFile = new File(PathUtils.getOutDir(project, "out"), "bundle.o")
        if (!pushFile.exists()) {
            println "Deploying fail file not found: ${pushFile.absolutePath}"
            return
        }
        def rootDir = project.rootDir
        String pkg = project.rootProject.ABooster.pkg
        String launchAct = project.rootProject.ABooster.launchActivity
        if (pkg.isEmpty()) {
            pkg = applicationId.get()
        }
        if (launchAct.isEmpty()) {
            launchAct = launchActivity
        }
        def localProperties = new File(rootDir, "local.properties")
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
                    commandLine "$adb", "-s", "$device", "push", "${pushFile.absolutePath}",
                            "/sdcard/Android/data/$pkg/cache/abooster/"
                    ignoreExitValue true
                }

                project.exec {
                    commandLine "$adb", "-s", "$device", "shell", "am", "force-stop", "$pkg"
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

    static class InstallTaskAction extends BaseTaskCreationAction<InstallTask> {

        InstallTaskAction(@NotNull TaskManger taskManger,
                          @NotNull VariantScope variantScope) {
            super(taskManger, variantScope)
        }

        @Override
        void configure(@NotNull InstallTask task) {
            super.configure(task)
            BaseVariantData variantData = variantScope.variantData
            GradleVariantConfiguration config = variantData.variantConfiguration
            InternalArtifactType taskInputType = variantScope.manifestArtifactType
            task.applicationId = TaskInputHelper.memoize(new Supplier<String>() {
                @Override
                String get() {
                    return config.originalApplicationId
                }
            })
            File manifestFiles = new File(variantScope.artifacts.getFinalProduct(
                    taskInputType).get().asFile, SdkConstants.ANDROID_MANIFEST_XML)
            if (manifestFiles.exists()) {
                XmlSlurper xmlSlurper = new XmlSlurper()
                GPathResult result = xmlSlurper.parse(manifestFiles)
                def launchActivity = result.application.activity.find {
                    it."intent-filter".find {
                        filter ->
                            return filter.action.find {
                                filter.action.find {
                                    it.'@android:name'.text() == 'android.intent.action.MAIN'
                                }
                            } && filter.category.find {
                                it.'@android:name'.text() == 'android.intent.category.LAUNCHER'
                            }
                    }
                }.'@android:name'
                task.launchActivity = launchActivity
            }
        }


        @Override
        String getName() {
            return variantScope.getTaskName("install", "BoosterBundle")
        }

        @Override
        Class<InstallTask> getType() {
            return InstallTask.class
        }
    }
}
