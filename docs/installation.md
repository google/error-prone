---
title: Installation
layout: documentation
---

Our goal is to make it simple to add Error Prone checks to your existing Java
compilation. Please note that Error Prone must be run on JDK 8 or newer. It can be used to build Java 6 or 7 code by setting the appropriate `-source` / `-target` / `-bootclasspath` flags.

Please join our [mailing
list](http://groups.google.com/group/error-prone-announce) to know when a new
version is released!



# Bazel

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

# Maven

Edit your `pom.xml` file to add settings to the maven-compiler-plugin:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.3</version>
      <configuration>
        <compilerId>javac-with-errorprone</compilerId>
        <forceJavacCompilerUse>true</forceJavacCompilerUse>
        <!-- maven-compiler-plugin defaults to targeting Java 5, but our javac
             only supports >=6 -->
        <source>7</source>
        <target>7</target>
      </configuration>
      <dependencies>
        <dependency>
          <groupId>org.codehaus.plexus</groupId>
          <artifactId>plexus-compiler-javac-errorprone</artifactId>
          <version>2.8</version>
        </dependency>
        <!-- override plexus-compiler-javac-errorprone's dependency on
             Error Prone with the latest version -->
        <dependency>
          <groupId>com.google.errorprone</groupId>
          <artifactId>error_prone_core</artifactId>
          <version>2.0.19</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

See the
[examples/maven](https://github.com/google/error-prone/tree/master/examples/maven)
directory for a working example:

```
../examples/maven/error_prone_should_flag$ mvn compile
[INFO] Compiling 1 source file to .../examples/maven/error_prone_should_flag/target/classes
.../examples/maven/error_prone_should_flag/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

# Gradle
The gradle plugin is an external contribution. The documentation and code is at
[tbroyer/gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin).

See the
[examples/gradle](https://github.com/google/error-prone/tree/master/examples/gradle)
directory for a working example:

```
$ gradle compileJava
.../examples/gradle/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
:compileJava FAILED

FAILURE: Build failed with an exception.
```

# Ant

Download the [latest release of Error Prone](https://repo1.maven.org/maven2/com/google/errorprone/error_prone_ant)
from maven, and add the following javac task to your project's `build.xml` file.

```xml
<javac destdir="build"
  compiler="com.google.errorprone.ErrorProneAntCompilerAdapter"
  encoding="UTF-8" debug="true"
  includeantruntime="false">
  <src path="src"/>
  <compilerclasspath>
    <pathelement location="./path/to/error_prone_ant.jar"/>
  </compilerclasspath>
</javac>
```

See [examples/ant](https://github.com/google/error-prone/tree/master/examples/ant) for alternate ant configurations.

```
examples/ant/compilerclasspath$ ant
Buildfile: .../examples/ant/compilerclasspath/build.xml

compile:
    [javac] Compiling 1 source file to .../examples/ant/compilerclasspath/build
    [javac] .../examples/ant/compilerclasspath/src/Main.java:20: error: [DeadException] Exception created but not thrown
    [javac]       new IllegalArgumentException("Missing required argument");
    [javac]       ^
    [javac]     (see http://errorprone.info/bugpattern/DeadException)
    [javac]   Did you mean 'throw new IllegalArgumentException("Missing required argument");'?
    [javac] 1 error

BUILD FAILED
```

# IntelliJ IDEA

To add the plugin, start the IDE and find the Plugins dialog. Browse Repositories, choose Category: Build, and find the Error-prone plugin. Right-click and choose "Download and install". The IDE will restart after you've exited these dialogs.

To enable Error Prone, choose `Settings | Compiler | Java Compiler | Use compiler: Javac with error-prone` and also make sure `Settings | Compiler | Use external build` is NOT selected.

# Eclipse

Ideally, you should find out about failed Error Prone checks as you code in eclipse, thanks to the continuous compilation by ECJ (eclipse compiler for Java). But this is an architectural challenge, as Error Prone currently relies heavily on the `com.sun.*` APIs for accessing the AST and symbol table.

For now, Eclipse users should use the Findbugs eclipse plugin instead, as it catches many of the same issues.

# Command Line

To use Error Prone from the command line as a javac replacement:

```
wget https://repo1.maven.org/maven2/com/google/errorprone/error_prone_ant/2.0.19/error_prone_ant-2.0.19.jar
java -Xbootclasspath/p:error_prone_ant-2.0.19.jar com.google.errorprone.ErrorProneCompiler Test.java
```

```
Test.java:1: error: [InfiniteRecursion] This method always recurses, and will cause a StackOverflowError
class Test { void f() { f(); } }
                        ^
    (see http://errorprone.info/bugpattern/InfiniteRecursion)
```


# My build system isn't listed here

If you're an end-user of the build system, you can [file a bug to request integration](https://github.com/google/error-prone/issues).

If you develop a build system, you should create an integration for your users! Here are some basics to get you started:

Error-prone is implemented as a compiler hook, using an internal mechanism in javac. To install our hook, we override the `main()` method in `com.sun.tools.javac.main.Main`.

Find the spot in your build system where javac's main method is called. This is assuming you call javac in-process, rather than shell'ing out to the javac executable on the machine (which would be pretty lame since it's hard to know where that's located). 

First, add Error Prone's core library to the right classpath. It will need to be visible to the classloader which currently locates the javac Main class. Then replace the call of `javac.main.Main.main()` with the Error Prone compiler:

`return new ErrorProneCompiler.Builder().build().compile(args) == 0`
