package com.yy.sdk.plugin.manager


import com.android.build.gradle.internal.tasks.factory.TaskFactory
import org.gradle.api.Project
import org.jetbrains.annotations.NotNull

/**
 * Created by nls on 2022/8/20
 * Description: LibTaskManager
 */
public class LibTaskManager extends TaskManger {

    LibTaskManager(TaskFactory taskFactory) {
        super(taskFactory)
    }

    @Override
    void createTaskForBooster(@NotNull Project project) {
        project.android.libraryVariants.all { variant ->
            //only support debug variant
            if (variant.name.contains("debug")) {
                createTaskForBooster(variant)
            }
        }
    }

//    @Override
//    void createLinkResTask(@NotNull VariantScope variantScope) {
//        linkResTask = taskFactory.register(
//                new LinkResTask.LinkResTaskAction(this, variantScope, false)
//        );
//    }
}
