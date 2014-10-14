---
title: Installing error-prone
layout: master
---

Our goal is to make it simple to add error-prone checks to your existing Java compilation.
Please join our [mailing list](http://groups.google.com/group/error-prone-announce) to know when a new version is released!

# Ant

1. Add error-prone to the compile-time classpath. There are two ways:
  * Include a `compilerclasspath` element in your `<javac />` task definition
  * Modify your Ant installation by copying the `error_prone_ant.jar` to your `${user.home}/.ant/lib` or `ANT_HOME/lib` directory.
2. Modify your `<javac>` tasks to include this attribute: `compiler="com.google.errorprone.ErrorProneAntCompilerAdapter"`

See the [examples/ant](http://code.google.com/p/error-prone/source/browse/#git%2Fexamples%2Fant) directory for a working example:
{% highlight bash %}
examples/ant$ ant
Buildfile: /Users/alexeagle/Projects/error-prone/examples/ant/build.xml

compile:
    [javac] Compiling 1 source file to /Users/alexeagle/Projects/error-prone/examples/ant/build
    [javac] /Users/alexeagle/Projects/error-prone/examples/ant/src/Main.java:6: error: [Empty if] Empty statement after if
    [javac]     if (a == 3); // BUG!
    [javac]     ^
    [javac] 1 error

BUILD FAILED
/Users/alexeagle/Projects/error-prone/examples/ant/build.xml:21: Compile failed; see the compiler error output for details.

Total time: 0 seconds
{% endhighlight %}

# Gradle
The gradle plugin is an external contribution. The documentation and code is at:
[tbroyer/gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin)

See the [examples/gradle](http://code.google.com/p/error-prone/source/browse/#git%2Fexamples%2Fgradle) directory for a working example:
{% highlight bash %}
$ gradle compileJava
:compileJava
error-prone/examples/gradle/src/main/java/Main.java:3: error: [EmptyIf] Empty statement after if
    if (args.length < 1);
                        ^
    (see http://code.google.com/p/error-prone/wiki/EmptyIf)
  Did you mean 'if (args.length < 1)'?
error-prone/examples/gradle/src/main/java/Main.java:3: warning: [EmptyStatement] Empty statement
    if (args.length < 1);
                        ^
    (see http://code.google.com/p/error-prone/wiki/EmptyStatement)
  Did you mean 'if (args.length < 1)'?
1 error
1 warning
:compileJava FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileJava'.
> Compilation failed with exit code 1; see the compiler error output for details.

{% endhighlight %}

# Maven

Edit your `pom.xml` file to add settings to the maven-compiler-plugin:
{% highlight xml %}
  <build>
  [...]
    <plugins>
    [...]
      <!-- Turn on error-prone -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.1</version>
        <configuration>
          <compilerId>javac-with-errorprone</compilerId>
          <forceJavacCompilerUse>true</forceJavacCompilerUse>
        </configuration>
        <dependencies>
          <dependency>
            <groupId>com.google.errorprone</groupId>
            <artifactId>error_prone_core</artifactId>
            <version>1.1.2</version>
          </dependency>
          <dependency>
            <groupId>org.codehaus.plexus</groupId>
            <artifactId>plexus-compiler-javac</artifactId>
            <version>2.3</version>
          </dependency>
          <dependency>
            <groupId>org.codehaus.plexus</groupId>
            <artifactId>plexus-compiler-javac-errorprone</artifactId>
            <version>2.3</version>
          </dependency>
        </dependencies>
      </plugin>
    [...]
    </plugins>
  [...]
  </build>
{% endhighlight %}

See the [examples/maven](http://code.google.com/p/error-prone/source/browse/#git%2Fexamples%2Fmaven) directory for a working example:
{% highlight bash %}
examples/maven$ mvn compile
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building maven-plugin-example 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] Compiling 1 source file to /Users/alexeagle/Projects/error-prone/examples/maven/target/classes
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] /Users/alexeagle/Projects/error-prone/examples/maven/src/main/java/Main.java:[20,5] [EmptyIf] Empty statement after if
    (see http://code.google.com/p/error-prone/wiki/EmptyIf)
  did you mean 'if (a == 3) // BUG!'?
[INFO] 1 error
[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.110s
[INFO] Finished at: Sun May 20 21:11:19 PDT 2012
[INFO] Final Memory: 13M/152M
[INFO] ------------------------------------------------------------------------
{% endhighlight %}

# IntelliJ IDEA

To add the plugin, start the IDE and find the Plugins dialog. Browse Repositories, choose Category: Build, and find the Error-prone plugin. Right-click and choose "Download and install". The IDE will restart after you've exited these dialogs.

To enable error-prone, choose _Settings | Compiler | Java Compiler | Use compiler: Javac with error-prone_ and also make sure _Settings | Compiler | Use external build_ is NOT selected.

# Eclipse
Ideally, you should find out about failed error-prone checks as you code in eclipse, thanks to the continuous compilation by ECJ (eclipse compiler for Java). But this is an architectural challenge, as error-prone currently relies heavily on the `com.sun.*` APIs for accessing the AST and symbol table.

For now, Eclipse users should use the Findbugs eclipse plugin instead, as it catches many of the same issues.

# My build system isn't listed here

If you're an end-user of the build system, you can [file a bug to request integration](http://code.google.com/p/error-prone/issues/entry?template=Request%20build%20system%20integration).

If you develop a build system, you should create an integration for your users! Here are some basics to get you started:

Error-prone is implemented as a compiler hook, using an internal mechanism in javac. To install our hook, we override the `main()` method in `com.sun.tools.javac.main.Main`.

Find the spot in your build system where javac's main method is called. This is assuming you call javac in-process, rather than shell'ing out to the javac executable on the machine (which would be pretty lame since it's hard to know where that's located). 

First, add error-prone's core library to the right classpath. It will need to be visible to the classloader which currently locates the javac Main class. Then replace the call of `javac.main.Main.main()` with the error-prone compiler:

`return new ErrorProneCompiler.Builder().build().compile(args) == 0`
