---
title: Installation
layout: documentation
---

Our goal is to make it simple to add Error Prone checks to your existing Java
compilation. Please note that Error Prone must be run on JDK 11 or newer. (It
can still be used to build Java 8 code by setting the appropriate `-source` /
`-target` / `-bootclasspath` flags.)

Please join our
[mailing list](http://groups.google.com/group/error-prone-announce) to know when
a new version is released!

## Bazel

Error Prone works out of the box with [Bazel](http://bazel.io).

```
java_library(
    name = "hello",
    srcs = ["Hello.java"],
)
```

```
$ bazel build :hello
ERROR: example/myproject/BUILD:29:1: Java compilation in rule '//example/myproject:hello'
examples/maven/error_prone_should_flag/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
BazelJavaBuilder threw exception: java compilation returned status ERROR
INFO: Elapsed time: 1.989s, Critical Path: 1.69s
```

## Maven

Edit your `pom.xml` file to add settings to the maven-compiler-plugin:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.10.1</version>
        <configuration>
          <source>8</source>
          <target>8</target>
          <encoding>UTF-8</encoding>
          <compilerArgs>
            <arg>-XDcompilePolicy=simple</arg>
            <arg>-Xplugin:ErrorProne</arg>
          </compilerArgs>
          <annotationProcessorPaths>
            <path>
              <groupId>com.google.errorprone</groupId>
              <artifactId>error_prone_core</artifactId>
              <version>${error-prone.version}</version>
            </path>
            <!-- Other annotation processors go here.

            If 'annotationProcessorPaths' is set, processors will no longer be
            discovered on the regular -classpath; see also 'Using Error Prone
            together with other annotation processors' below. -->
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

On JDK 16 and newer, additional flags are required due to
[JEP 396: Strongly Encapsulate JDK Internals by Default](https://openjdk.java.net/jeps/396).

If your `maven-compiler-plugin` uses an external executable, e.g. because `<fork>` is `true` or because the `maven-toolchains-plugin` is enabled,
add the following under `compilerArgs` in the configuration above:

```xml
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
            <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>
```

Otherwise, add the following to the [.mvn/jvm.config](https://maven.apache.org/configure.html#mvn-jvm-config-file) file:

```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
```

See the [flags documentation](http://errorprone.info/docs/flags#maven) for details on
how to customize the plugin's behavior.

## Gradle

The gradle plugin is an external contribution. The documentation and code is at
[tbroyer/gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin).

## Ant

Download the following artifacts from maven:

*   `error_prone_core-${EP_VERSION?}-with-dependencies.jar` from https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/
*   [dataflow-errorprone-3.15.0.jar](https://repo1.maven.org/maven2/org/checkerframework/dataflow-errorprone/3.15.0/dataflow-errorprone-3.15.0.jar)

and add the following javac task to your project's `build.xml` file:

```xml
    <path id="processorpath.ref">
      <pathelement location="${user.home}/.m2/repository/com/google/errorprone/error_prone_core/${error-prone.version}/error_prone_core-${error-prone.version}-with-dependencies.jar"/>
      <pathelement location="${user.home}/.m2/repository/org/checkerframework/dataflow-errorprone/3.15.0/dataflow-errorprone-3.15.0.jar"/>
    </path>

    <javac srcdir="src" destdir="build" fork="yes" includeantruntime="no">
      <compilerarg value="-XDcompilePolicy=simple"/>
      <compilerarg value="-processorpath"/>
      <compilerarg pathref="processorpath.ref"/>
      <compilerarg value="-Xplugin:ErrorProne -Xep:DeadException:ERROR" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED" />
      <compilerarg value="-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" />
      <compilerarg value="-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED" />
      <compilerarg value="-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED" />
    </javac>
```

Setting the following `--add-exports=` flags is required on JDK 16 and newer due
to
[JEP 396: Strongly Encapsulate JDK Internals by Default](https://openjdk.java.net/jeps/396):

## IntelliJ IDEA

To add the plugin, start the IDE and find the Plugins dialog. Browse
Repositories, choose Category: Build, and find the Error-prone plugin.
Right-click and choose "Download and install". The IDE will restart after you've
exited these dialogs.

To enable Error Prone, choose `Settings | Compiler | Java Compiler | Use
compiler: Javac with error-prone` and also make sure `Settings | Compiler | Use
external build` is NOT selected.

## Eclipse

Ideally, you should find out about failed Error Prone checks as you code in
eclipse, thanks to the continuous compilation by ECJ (eclipse compiler for
Java). But this is an architectural challenge, as Error Prone currently relies
heavily on the `com.sun.*` APIs for accessing the AST and symbol table.

For now, Eclipse users should use the Findbugs eclipse plugin instead, as it
catches many of the same issues.

## Command Line

Error Prone supports the
[`com.sun.source.util.Plugin`](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html)
API, and can be used with JDK 9 and up by adding Error Prone to the
`-processorpath` and setting the `-Xplugin` flag.

Example:

```bash
wget https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/${EP_VERSION?}/error_prone_core-${EP_VERSION?}-with-dependencies.jar
wget https://repo1.maven.org/maven2/org/checkerframework/dataflow-errorprone/3.15.0/dataflow-errorprone-3.15.0.jar
javac \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  -J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
  -J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
  -XDcompilePolicy=simple \
  -processorpath error_prone_core-${EP_VERSION?}-with-dependencies.jar:dataflow-errorprone-3.15.0.jar \
  '-Xplugin:ErrorProne -XepDisableAllChecks -Xep:CollectionIncompatibleType:ERROR' \
  ShortSet.java
```

```
ShortSet.java:8: error: [CollectionIncompatibleType] Argument 'i - 1' should not be passed to this method; its type int is not compatible with its collection's type argument Short
      s.remove(i - 1);
              ^
    (see http://errorprone.info/bugpattern/CollectionIncompatibleType)
1 error
```

The `--add-exports` and `--add-opens` flags are required when using JDK 16 and
newer due to
[JEP 396: Strongly Encapsulate JDK Internals by Default](https://openjdk.java.net/jeps/396):

## My build system isn't listed here

If you're an end-user of the build system, you can
[file a bug to request integration](https://github.com/google/error-prone/issues).

If you develop a build system, you should create an integration for your users!
Here are some basics to get you started:

Error-prone is implemented as a compiler hook, using an internal mechanism in
javac. To install our hook, we override the `main()` method in
`com.sun.tools.javac.main.Main`.

Find the spot in your build system where javac's main method is called. This is
assuming you call javac in-process, rather than shell'ing out to the javac
executable on the machine (which would be pretty lame since it's hard to know
where that's located).

First, add Error Prone's core library to the right classpath. It will need to be
visible to the classloader which currently locates the javac Main class. Then
replace the call of `javac.main.Main.main()` with the Error Prone compiler:

`return new ErrorProneCompiler.Builder().build().compile(args) == 0`

## Using Error Prone together with other annotation processors

All of the above instructions use the `javac` option `-processorpath` which has
side-effect of causing `javac` to no longer scan the compile classpath for
annotation processors. If you are using other annotation processors in addition
to Error Prone, such as
[AutoValue](https://github.com/google/auto/tree/master/value), then you will
need to add their JARs to your `-processorpath` argument. The mechanics of this
will vary according to the build tool you are using.

## JDK 8

Error Prone 2.10.0 is the latest version to support running on JDK 8. (Compiling
the Java 8 language level is still supported by using a javac from a newer JDK,
and setting the appropriate `-source`/`-target`/`-bootclasspath` or `--release`
flags).

For instructions on using Error Prone 2.10.0 with JDK 8, see
[this older version of the installation instructions](https://github.com/google/error-prone/blob/f8e33bc460be82ab22256a7ef8b979d7a2cacaba/docs/installation.md).
