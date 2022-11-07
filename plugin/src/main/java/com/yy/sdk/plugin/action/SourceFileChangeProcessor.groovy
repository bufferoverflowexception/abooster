package com.yy.sdk.plugin.action

import org.gradle.api.internal.tasks.compile.incremental.recomp.RecompilationSpec

/**
 * Created by nls on 2020/8/26.
 * Nothing.
 */
class SourceFileChangeProcessor {

    private boolean checkDependent = false
    private final Collection<String> classesToCompile

    SourceFileChangeProcessor(boolean checkDependent) {
        this.checkDependent = checkDependent
    }

    public void processChange(File inputFiles, Collection<String> className, RecompilationSpec spec) {
        if (!checkDependent) {
            spec.getClassesToCompile().addAll(className)
        }
    }
}
