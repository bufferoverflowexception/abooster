package com.yy.sdk.plugin

import com.android.build.gradle.internal.tasks.factory.TaskFactoryImpl
import com.yy.sdk.plugin.manager.AppTaskManager
import com.yy.sdk.plugin.manager.LibTaskManager
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.AppliedPlugin

/**
 * Created by nls on 2020/8/23.
 * Nothing.
 */
class BuilderPlugin extends BasePlugin {

    private static final List<String> PREREQ_PLUGIN_OPTIONS = [
            'com.android.application',
            'com.android.library',
            'java-library'
    ]

    private static final int PLUGIN_TYPE_APP = 100
    private static final int PLUGIN_TYPE_LIB = 101
    private static final int PLUGIN_TYPE_JAVA = 102
    private int pluginType = 0
    private boolean wasApplied = false

    @Override
    void apply(Project project) {
        Action<? super AppliedPlugin> applyWithPrerequisitePlugin = { prerequisitePlugin ->
            if (wasApplied) {
                project.logger.warn('The plugin was already applied to the project: ' + project.path
                        + ' and will not be applied again after plugin: ' + prerequisitePlugin.id)
            } else {
                wasApplied = true
                if (prerequisitePlugin.id == "com.android.library") {
                    pluginType = PLUGIN_TYPE_LIB
                } else if (prerequisitePlugin.id == "com.android.application") {
                    pluginType = PLUGIN_TYPE_APP
                } else {
                    pluginType = PLUGIN_TYPE_JAVA
                }
                //super.apply(project)
                project.afterEvaluate {
                    super.apply(project)
                }
            }
        }
        PREREQ_PLUGIN_OPTIONS.each { pluginName ->
            project.pluginManager.withPlugin(pluginName, applyWithPrerequisitePlugin)
        }
    }


    @Override
    void createTaskManager(TaskFactoryImpl taskFactory) {
        if (pluginType == PLUGIN_TYPE_APP) {
            taskManger = new AppTaskManager(taskFactory)
        } else if (pluginType == PLUGIN_TYPE_LIB) {
            taskManger = new LibTaskManager(taskFactory)
        } else {
            //super.createTaskManager(taskFactory)
        }
    }

    @Override
    public void createExtension() {
    }

    @Override
    public void createTask() {
        if (taskManger != null) {
            taskManger.createTaskForBooster(project)
        }
    }

    @Override
    public void configureProject() {
        //java plugin are not support yet
        if (pluginType == PLUGIN_TYPE_JAVA) {
            return
        }
        project.configurations {
            aboosterJars {
                canBeConsumed = true
                canBeResolved = false
                attributes {
//                    attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category, Category.LIBRARY))
//                    attribute(attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
//                            JavaVersion.current().majorVersion.toInteger()))
                    attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.class, "aboosterJars"))
                }
            }
            aboosterClasspath {
                canBeConsumed = false
                canBeResolved = true
                attributes {
//                    attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category, Category.LIBRARY))
//                    attribute(attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
//                            JavaVersion.current().majorVersion.toInteger()))
                    attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.class, "aboosterJars"))
                }
            }
        }

        //def jarTask = project.tasks.withType(BuildJarTask.class)
        def jarTask = taskManger.buildJarTask().get()
        project.artifacts {
            //I have no idea how to support multi variant build
            // multi variant build task maybe remove later.
            aboosterJars(project.file(jarTask.getOutDir())) {
                builtBy(jarTask)
            }
        }

        ["implementation", "api"].each { name ->
            def configuration = project.configurations.getByName(name)
            def projectDependencies = configuration.dependencies.withType(ProjectDependency.class)
            projectDependencies.forEach { dependency ->
                if (!project.rootProject.ABooster.exclude.contains(dependency.name)) {
                    project.dependencies {
                        //aboosterClasspath(project(path: ":${dependency.name}", configuration: 'aboosterJars'))
                        aboosterClasspath(project(path: ":${dependency.name}"))
                    }
                }
            }
        }

        taskManger.buildJavaTask().configure {
            it.inputs.files(project.configurations.aboosterClasspath)
        }

        taskManger.buildKotlinTask().configure {
            it.inputs.files(project.configurations.aboosterClasspath)
        }

        //暂时不开源这个功能
//        if (pluginType == PLUGIN_TYPE_APP) {
//            new GradleHooker().startHook(project)
//        }
    }
}
