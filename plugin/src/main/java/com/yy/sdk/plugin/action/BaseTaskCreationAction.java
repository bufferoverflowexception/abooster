package com.yy.sdk.plugin.action;

import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.tasks.VariantAwareTask;
import com.android.build.gradle.internal.tasks.factory.VariantTaskCreationAction;
import com.yy.sdk.plugin.manager.ITaskContainer;

import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Create by nls on 2022/7/22
 * description: BaseTaskCreationAction
 */
public abstract class BaseTaskCreationAction<T extends VariantAwareTask & Task> extends VariantTaskCreationAction<T> {

    protected ITaskContainer taskManger;

    public BaseTaskCreationAction(@NotNull ITaskContainer taskManger, @NotNull VariantScope variantScope) {
        super(variantScope);
        this.taskManger = taskManger;
    }
}