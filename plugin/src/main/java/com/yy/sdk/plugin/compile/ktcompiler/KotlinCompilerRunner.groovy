package com.yy.sdk.plugin.compile.ktcompiler


import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider

/**
 * Created by nls on 2020/8/31.
 * Nothing.
 */
class KotlinCompilerRunner extends GradleCompilerRunner {

    KotlinCompilerRunner(GradleCompileTaskProvider task) {
        super(task);
    }


    @Override
    protected void runCompilerAsync(@NotNull GradleKotlinCompilerWorkArguments workArgs) {
        KotlinCompilerWork work = new KotlinCompilerWork(workArgs)
        work.run()
    }
}
