# abooster
An android gradle plugin for  enhance compiler and reduce cost time in building

##### 背景
abooster是20年的时候我给hago弄的构建加速插件，当时由于hago是海外项目，需要上架GP的，因此不能像国内那样搞插件化，加上hago整个项目的规模也十分之庞大，几十个modules几十上百万行代码，并且常年不更新agp kgp，所以hago的构建是比较慢的，如果动到一些底层base库那个编译等待时间真的是要命。为了解决编译耗时问题，所以开始着手研究agp跟kgp，开发构建加速插件。不过由于某种原因，这个插件项目刚开发完还没来得及测试就被我弃用了。直到今年有同事希望我能把资源编译也加上，研究下发现也是可行的，功能加上后整理了代码跟资料后，现把项目开源出去，供同样受编译耗时问题困扰的友人们参考参考。

 [原文出处](https://www.jianshu.com/p/411141029d23)

##### Preview
 ![img](https://raw.githubusercontent.com/bufferoverflowexception/abooster/main/preview.gif)

##### 使用介绍
abooster并不是通用的解决方案，要根据自身的项目环境去适配，目前已经适配的agp版本是`3.4.2` 已适配的kgp版本是`1.4.32` (版本是比较低，因为公司的项目还在用 没办法。。)

abooster的使用比较简单，只需要在项目根`build.gradle`下apply下plugin就可以了，如下：
```groovy
dependencies {
      classpath "com.android.tools.build:gradle:${gradle_version}"
      classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
      classpath "com.yy.sdk.abooster:plugin:2.0.0-SNAPSHOT"
}
apply plugin: 'com.yy.sdk.abooster.plugin'
```
使用的时候你可以正常通过`assembleDebug`(或as里的run三角icon)来跑你的代码，abooster拦截了gradle的构建流程，会自动检测是否支持增量，能支持增量编译的话会走abooster内部构建，不支持增量编译会自动跑回系统的默认构建流程(开源版暂不提供此功能)。
又或者是手动执行abooster的`buildDebugBoosterBundle`任务，`buildDebugBoosterBundle`任务会触发kotlin java等代码的编译，或资源的编译打包等等。需要注意的是，只有App模块才会有`buildDebugBoosterBundle`，Android Library模块是没有的，使用的时候也只管跑App模块的任务就可以了，其余模块下的abooster任务可以不管。

abooster只支持增量构建并不支持全量构建，因此第一次执行`buildDebugBoosterBundle`任务的时候所有task状态都会是incremental:false，false的时候abooster默认是不执行构建的并且会清理掉缓存目录，第二次跑`buildDebugBoosterBundle`的时候才会是增量，这个其实也很好理解，增量毕竟是在上一次的基础上diff出来的，所以第一次必然是非增量的。

abooster目前并不支持java library模块或gradle-plugin模块，不支持的模块可以配置`ABoosterExtension`来进行exclude 譬如：
```groovy
ABooster {
    pkg = "com.yy.sdk.abooster"
    launchActivity = ".MainActivity"
    exclude = ['testlib']
}
```
并不是遇到了什么技术难点所以不支持，仅仅是因为我没时间去做适配而已。。。 

`pkg` `launchActivity`是可选配置，不配也没关系，我会通过解析`AndroidManifest.xml`把默认配置读取出来。


##### 原理介绍
abooster能把原来一分钟甚至是几分钟的构建时长缩短到10秒内(当时在hago上面测到的数据是，五六分钟的构建时间可以缩短为8-10秒)，因为abooser的构建是full-incremental，假设你的项目里有上百个类，但是你只修改了a.java b.kt以及test_activity.xml，abooster只会编译这几个修改文件，所以构建速度是很快的，其实agp kgp本身也是支持增量构建的，但官方的编译工具首要核心要求是要稳定，BUG少且能满足各种不同场景下的构建，因此构建流程会很重且很复杂，最终就是总的编译耗时长。

abooster的原理比较简单，跟热修复类似的，先会把修改代码打成补丁包，接着push到手机上，installer会解析并且安装这个补丁包。补丁加载逻辑是从tinker拷过来修改的，这里要感谢下微信团队开源出来的这么优秀的项目以及相关的干货技术文章。

abooster的编译构建逻辑是参考了agp kgp源码后重写出来的，这块有两个核心重点，第一个是**修改文件检测** 第二个才是**编译打包**
- 修改文件检测
不管是agp还是kgp，对于修改文件的检测都是依赖了gradle提供的api，abooster也并不例外，关于gradle增删修改文件的检测可以看我之前写的文章，这里就不再介绍了。这里要吐槽一下3.4版本依赖的gradle版本文件增删修改检测api已经被标记为deprecate了，但是新提供出来的`InputChanges`API有BUG，会有奇奇怪怪的报错问题，花了我很大的精力去解决，最后发现kgp agp也并没有使用最新的`InputChanges`接口，因此abooster这里也是回退使用了旧的API。
目前abooster内部的所有task任务都已经支持增量了，除了`buildDebugBoosterBundle`以及`installDebugBoosterBundle`并不支持以外。

- 编译打包
当检测到文件有修改后abooster会启动对应的task任务去处理，如修改了kt文件会启用kotlin compiler相关的api进行kotlin代码编译，检测到资源有修改会先调用aapt相关接口进行编译以及链接打包。关于java代码的编译，kotlin代码的编译以及资源的编译跟链接过程，我之前写的文章也已经分析过了，这里就不再重复赘述了，详细过程可以参考abooster源码或者翻阅我之前写的博客。

##### 源码介绍
abooster核心的modules有两个`installer` `plugin`，前者负责补丁包的安装，后者是补丁包的打包插件，这里只介绍构建插件。

- **如何调试**
abooster并没有使用`buildSrc`的方式去写插件，而是使用了`includeBuild`方式去开发调试，仓库clone下来如果第一次导入到as里报找不到`com.yy.sdk.abooster:plugin`插件的话，可以先在`build.gradle`屏蔽掉插件，然后修改`setting.gradle`直接include plugin，最后本地发布一次仓库即可，之后都可以通过`includeBuild`的方式来开发调试abooster，不用每次修改代码后都需要发布仓库。

  运行abooster直接跑App模块下的`buildDebugBoosterBundle`任务就可以了，第一次执行会是全量构建，哪怕你是有修改过代码第一次执行也是全量构建，因为没有得跟上一次做对比，abooster会忽略全量构建，你需要再次修改代码文件后再执行`buildDebugBoosterBundle`任务才会走增量编译。
  
  abooster的调试也是比较简单，命令行下执行
```
./gradlew :app::buildDebugBoosterBundle -Dorg.gradle.daemon=false -Dorg.gradle.debug=true --info
```
然后在as上面起个远程配置挂载上去就可以了，之后就跟调试普通代码一样了，下断点单步执行。

- **buildDebugJava任务**
`buildDebugJava`任务负责项目里的Java代码编译，如果修改了Java源码，执行`buildDebugBoosterBundle `任务时会由`buildDebugJava`来负责编译，编译后生成的产物在`/build/intermediates/abooster/java/debug/`目录下

- **buildDebugKotlin**任务
跟上面的Java编译任务类似的，`buildDebugKotlin`负责编译kt代码，编译后生成的产物是在`/build/intermediates/abooster/kotlin/debug/`目录下

- **buildDebugJar**任务
`buildDebugJar`任务是非必要的，它的作用是把前面的Java以及kotlin编译任务编译出来的产物打包成`jar`包，其实这一步是非必要的，也可以不用做，打包成`jar`存粹是为了在多modules项目下，方便把编译后的产物传给它的依赖project，它的生成产物是在`/build/intermediates/abooster/jar/`目录下

- **buildDebugResource**任务
`buildDebugResource`负责资源的编译工作，它会把有改动的资源编译成后缀为`*.flat`的中间产物，大概就类似于vs编译c++后生成的obj文件这样子吧。关于资源的编译以及链接原理也可以翻阅我之前写过的文章。它的生成产物是在`/build/intermediates/abooster/res/debug`目录下

- **linkDebugResource**任务
`linkDebugResource`负责把前面编译好的资源文件链接成资源包，需要主要的是它是个全量的链接而不是增量的。App模块才会有此任务，library模块没有。生成后的产物是`/build/intermediates/abooster/res/debug/out/out.ap_`

- **transformDebugClassToDex**任务
`transformDebugClassToDex`任务会把前面编译好的`*.class`文件链接成`dex`文件，同样的只有App模块才会有此任务，library模块也是并没有的。生成产物是`/build/intermediates/abooster/dex/classes.dex`

- **buildDebugBoosterBundle**任务
`buildDebugBoosterBundle`会把前面打包出来的`dex`文件以及`out.ap_`文件打包成补丁包，输出到`/build/intermediates/abooster/out/bundle.o`下。这个任务暂时并不支持增量，并不是遇到什么技术难点而不支持，仅仅是因为我没时间做而已

- **installDebugBoosterBundle**任务
`installDebugBoosterBundle`任务就比较简单了，它会把前面打包好的补丁包push到手机上，并且重启App，App重启后会执行热修复那套流程，加载补丁。这个任务暂时也不支持增量，原因跟上面的一样。

##### 写在最后
daemon原生构建就支持了依赖检测的，譬如a方法被修改了，b引用了a，那么b也会被带进去重新编译，但是实践过程中发现一个很奇怪的现象，kotlin间接引用Java时会报类似这样的错误
```
Supertypes of the following classes cannot be resolved. Please make sure you have the required dependencies in the classpath:
    class com.yy.sdk.abooster.Test, unresolved supertypes: com.yy.sdk.abooster.JavaTest
```
如果把编译输出指向kotlin原来的`kotlin-classes`目录就不会报错，放其他任意目录都会报错，没搞懂这个问题的原因，但是我又不想把abooster构建出来的产物放在`kotlin-classes`里，因为这样必然会影响到了`compileDebugKotlin`任务的增量检测，时间关系我只能先把这个依赖检测编译功能给砍了。
 abooster目前也不支持multi variant，要实现multi variant还得实现一套variant data variant manager等等，目前只支持没有配置过flavor的项目，且只有debug环境下才支持(release没有支持的必要)。代码写得比较渣，凑合着来看吧，毕竟从开始研究到落地也仅仅给了几个周末的时间而已，源码只提供一种思路，一种方案跟实现。

