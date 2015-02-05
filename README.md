# error-prone

Catch common Java mistakes as compile-time errors

[![Build Status](https://travis-ci.org/google/error-prone.svg?branch=master)](https://travis-ci.org/google/error-prone)

## Getting Started

Our documentation for users is at http://errorprone.info

### Maven

Add the following configuration to your `pom.xml`.

```xml
<build>
  <plugins>
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
          <groupId>org.codehaus.plexus</groupId>
          <artifactId>plexus-compiler-javac-errorprone</artifactId>
          <version>2.5</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

### Ant

Download the [latest release of error-prone](https://repo1.maven.org/maven2/com/google/errorprone/error_prone_ant)
from maven, and add the following javac task in your `build.xml`.

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

See `examples/ant` for alternate ant configurations.

### Gradle

The gradle plugin is an external contribution. The documentation and code is
at [tbroyer/gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin)


## Developing error-prone

To develop and build error-prone, see our documentation on the
[wiki](https://github.com/google/error-prone/wiki/For-Developers).

## Links

- [Javadoc](http://errorprone.info/api/latest/)
- Pre-release snapshots are available from [Sonatype's snapshot
  repository](https://oss.sonatype.org/content/repositories/snapshots/com/google/errorprone/).
