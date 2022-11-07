package com.yy.sdk.plugin.compile.ktcompiler

import org.gradle.api.GradleException
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.logging.GradleBufferingMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.SL4JKotlinLogger
import org.jetbrains.kotlin.gradle.tasks.TasksUtilsKt
import org.jetbrains.kotlin.gradle.utils.ErrorUtilsKt
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.rmi.RemoteException

/**
 * Created by nls on 2020/8/31.
 */
class KotlinCompilerWork {

    public static final COMPILE_MODE_OUT_PROCESS = 0
    public static final COMPILE_MODE_IN_PROCESS = 1
    public static final COMPILE_MODE_WITH_DAEMON = 2

    int compileMode = COMPILE_MODE_WITH_DAEMON
    private KotlinLogger logger
    private GradleKotlinCompilerWorkArguments args

    KotlinCompilerWork(GradleKotlinCompilerWorkArguments arguments) {
        //super(arguments)
        this.args = arguments
    }

    //@Override
    void run() {
        switch (compileMode) {
            case COMPILE_MODE_WITH_DAEMON:
                compileWithDaemon()
                break
        //下面两种构建先不开源.
//            case COMPILE_MODE_OUT_PROCESS:
//                compileOutProcess()
//                break
//            case COMPILE_MODE_IN_PROCESS:
//                compileInProcess()
//                break
        }
    }

    //系统的daemon构建失败后会自动走全量构建,我们不要走全量,这里全部重写吧.
    private void compileWithDaemon() {
        List<String> additionalJvmParams = new ArrayList<>()
        File clientIsAliveFlagFile = args.projectFiles.getClientIsAliveFlagFile()
        File sessionFlagFile = args.projectFiles.getSessionFlagFile()
        MessageCollector messageCollector =
                new GradlePrintingMessageCollector(getLogger(), false)
        CompilerId compilerId = CompilerId.makeCompilerId(args.compilerFullClasspath)
        String[] array = (String[]) additionalJvmParams.toArray(new String[0])
        CompileServiceSession connection = KotlinCompilerRunnerUtils.newDaemonConnection(
                compilerId,
                clientIsAliveFlagFile,
                sessionFlagFile,
                messageCollector,
                false,
                DaemonParamsKt.configureDaemonOptions(),
                array)

        if (connection == null) {
            throw new GradleException("fail to new kotlin daemon connection")
        }
        IncrementalCompilationEnvironment icEnv = args.incrementalCompilationEnvironment
        ChangedFiles.Known knownChangedFiles = icEnv.changedFiles
        if (knownChangedFiles == null) {
            throw new GradleException("build fail! unknown change sources file")
        }

        IncrementalCompilationOptions options = new IncrementalCompilationOptions(
                true,
                knownChangedFiles.modified,
                knownChangedFiles.removed,
                icEnv.workingDir,
                CompilerMode.INCREMENTAL_COMPILER,
                CompileService.TargetPlatform.JVM,
                reportCategories(),
                ReportSeverity.INFO.ordinal(),
                requestCompileRequest(),
                icEnv.usePreciseJavaTracking,
                args.outputFiles,
                icEnv.multiModuleICSettings,
                args.incrementalModuleInfo,
                args.kotlinScriptExtensions)

        GradleBufferingMessageCollector collector = new GradleBufferingMessageCollector()
        GradleCompilationResults compilationResults =
                new GradleCompilationResults(getLogger(), args.projectFiles.projectRootFile)
        GradleIncrementalCompilerServicesFacadeImpl servicesFacade =
                new GradleIncrementalCompilerServicesFacadeImpl(getLogger(), collector, 0)

        CompileService.CallResult<Integer> result
        try {
            result = connection.compileService.compile(
                    connection.sessionId, args.compilerArgs, options, servicesFacade, compilationResults)
            collector.flush(messageCollector)
        } catch (Throwable throwable) {
            collector.flush(messageCollector)
            getLogger().error("Compilation with Kotlin compile daemon was not successful")
            getLogger().error(ErrorUtilsKt.stackTraceAsString(throwable))
        }

        try {
            connection.compileService.clearJarCache()
        } catch (RemoteException e) {
            getLogger().warn("Unable to clear jar cache after compilation, " +
                    "maybe daemon is already down: $e")
        }
        if (result == null) {
            throw new GradleException("build kotlin with daemon fail!")
        }
        int code = result.get()
        ExitCode exitCode = ExitCode.values().find {
            it.code == code
        }
        TasksUtilsKt.throwGradleExceptionIfError(exitCode)
    }

    private Integer[] requestCompileRequest() {
        Integer[] req = new Integer[1]
        req[0] = CompilationResultCategory.IC_COMPILE_ITERATION.ordinal()
        return req
    }

    private Integer[] reportCategories() {
//        List<Integer> list = new ArrayList<>()
//        ReportCategory.values().each {
//            list.add(it.code)
//        }
//        Integer [] array = (Integer[])list.toArray(new Integer[0])
//        return array
        Integer[] report = new Integer[1]
        report[0] = ReportCategory.COMPILER_MESSAGE.ordinal()
        return report
    }

    private KotlinLogger createLogger() {
        Logger logger = LoggerFactory.getLogger("GradleKotlinCompilerWork")
        if (logger instanceof org.gradle.api.logging.Logger) {
            return new GradleKotlinLogger(logger)
        } else {
            return new SL4JKotlinLogger(logger)
        }
    }

    private KotlinLogger getLogger() {
        if (logger == null) {
            logger = createLogger()
        }
        return logger
    }
}
