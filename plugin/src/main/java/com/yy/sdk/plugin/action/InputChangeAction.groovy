package com.yy.sdk.plugin.action

import com.yy.sdk.plugin.utils.FileUtils
import org.gradle.api.Action
import org.gradle.api.internal.tasks.compile.incremental.recomp.RecompilationSpec
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Created by nls on 2020/8/26.
 * Nothing.
 */
class InputChangeAction implements Action<InputFileDetails> {

    private final SourceFileChangeProcessor javaChangeProcessor
    private final RecompilationSpec spec = new RecompilationSpec()

    InputChangeAction(SourceFileChangeProcessor javaChangeProcessor) {
        this.javaChangeProcessor = javaChangeProcessor
    }

    @Override
    public void execute(InputFileDetails inputFileDetails) {
        File file = inputFileDetails.getFile()
        println "InputChangeAction file: ${file.absolutePath}"
        if (FileUtils.hasExtension(file, ".java")) {
            spec.classesToCompile.add(file.absolutePath)
        }
    }

    public RecompilationSpec getRecompilationSpec() {
        return spec
    }
}
