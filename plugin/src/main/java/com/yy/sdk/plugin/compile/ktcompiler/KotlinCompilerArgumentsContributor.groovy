package com.yy.sdk.plugin.compile.ktcompiler


import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsConfigurationFlag
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor

/**
 * Created by nls on 2022/7/24
 * Description: KotlinCompilerArgumentsContributor
 */
public class KotlinCompilerArgumentsContributor implements
        CompilerArgumentsContributor<K2JVMCompilerArguments> {

    protected CompileOptions compileOptions

    KotlinCompilerArgumentsContributor(CompileOptions compileOptions) {
        this.compileOptions = compileOptions
    }

    @Override
    void contributeArguments(@NotNull K2JVMCompilerArguments arg,
                             @NotNull Collection<? extends CompilerArgumentsConfigurationFlag> collection) {

        StringBuilder sb = new StringBuilder()
        compileOptions.classpath.forEach {
            sb.append(it.path + File.pathSeparator)
        }
        compileOptions.bootClasspath.forEach {
            sb.append(it.path + File.pathSeparator)
        }
        arg.classpath = sb.toString()
        arg.jvmTarget = compileOptions.sourceCompatibility
        arg.noStdlib = true
        arg.noReflect = true
        arg.moduleName = "app_debug"
        arg.noJdk = true
    }
}
