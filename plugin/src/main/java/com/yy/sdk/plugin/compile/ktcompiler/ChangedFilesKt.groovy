package com.yy.sdk.plugin.compile.ktcompiler

import com.yy.sdk.plugin.utils.FileUtils
import kotlin.jvm.internal.Intrinsics
import org.gradle.api.Action
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.incremental.ChangedFiles

/**
 * Created by nls on 2020/9/15.
 * copy from {@link org.jetbrains.kotlin.gradle.incremental.ChangedFilesKt}.
 */
class ChangedFilesKt {
    @NotNull
    public static final ChangedFiles ChangedFiles(@NotNull IncrementalTaskInputs taskInputs) {
        Intrinsics.checkParameterIsNotNull(taskInputs, "taskInputs");
        if (!taskInputs.isIncremental()) {
            return (ChangedFiles) (new ChangedFiles.Unknown());
        } else {
            final List<File> modified = new ArrayList();
            final List<File> removed = new ArrayList();
            taskInputs.outOfDate(new Action<InputFileDetails>() {
                void execute(InputFileDetails inputFileDetails) {
                    File file = inputFileDetails.getFile()
                    if (FileUtils.hasExtension(file, ".kt")) {
                        modified.add(inputFileDetails.getFile());
                    }
                }
            });
            taskInputs.removed(new Action<InputFileDetails>() {
                void execute(InputFileDetails inputFileDetails) {
                    File file = inputFileDetails.getFile()
                    if (FileUtils.hasExtension(file, ".kt")) {
                        modified.add(inputFileDetails.getFile());
                    }
                }
            });
            return (ChangedFiles) (new ChangedFiles.Known(modified, removed));
        }
    }
}
