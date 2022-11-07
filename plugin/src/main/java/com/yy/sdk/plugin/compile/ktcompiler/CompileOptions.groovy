package com.yy.sdk.plugin.compile.ktcompiler

import org.gradle.api.file.FileCollection;

/**
 * Create by nls on 2022/7/30
 * description: CompileOptions
 */
class CompileOptions {
    FileCollection classpath
    FileCollection bootClasspath
    String sourceCompatibility
}
