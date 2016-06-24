---
title: Plugin checks
layout: documentation
---

Error Prone supports custom checks via a plugin mechanism. Plugin checks are
loaded dynamically from the annotation processor path using
[java.util.ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

Using [AutoService](https://github.com/google/auto/tree/master/service) to
specify the service descriptor is recommended.

## Example

Plugin checks are implemented exactly the same way as the built-in checks,
except for the `@AutoService(BugChecker.class)` annotation:

```java
@AutoService(BugChecker.class) // the service descriptor
@BugPattern(
  name = "MyCustomCheck",
  // ...
)
public class MyCustomCheck extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // TODO:
  }
}
```

## Build system support

Plugin checks should work with any build system that allows setting the
annotation processor classpath.

### Bazel

Bazel allows annotation processors to be configured using the
[java_plugin](https://www.bazel.io/docs/be/java.html#java_plugin) rule:

```
java_plugin(
    name = "MyCustomCheckPlugin",
    srcs = ["MyCustomCheck.java"],
    deps = [
        "//third_party/java/auto_service",
        "@error_prone//jar",
        "@guava//jar",
    ],
)
```

For a complete example, see:
[examples/plugin/bazel](https://github.com/google/error-prone/tree/master/examples/plugin/bazel).

### Maven

Starting in version 3.5, maven-compiler-plugin allows the processor path to be
configured with the
[annotationProcessorPaths](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
parameter.

For a complete example, see:
[examples/plugin/maven](https://github.com/google/error-prone/tree/master/examples/plugin/maven).

### Gradle

Gradle does not
([yet](https://github.com/gradle/gradle/blob/master/design-docs/java-annotation-processing.md))
have built-in support for setting the processor path, but its flexibility makes
it relatively easy to do manually:

```gradle
configurations {
  annotationProcessor
}

dependencies {
  annotationProcessor project(':custom-checks')
}

tasks.withType(JavaCompile) {
  options.compilerArgs += [ '-processorpath', configurations.annotationProcessor.asPath ]
}
```

Gradle plugins exist that also offer this configurability:
[`net.ltgt.apt`](https://plugins.gradle.org/plugin/net.ltgt.apt) for standard
Java projects, or
[`android-apt`](https://bitbucket.org/hvisser/android-apt) or the
[experimental new Android toolchain](https://sites.google.com/a/android.com/tools/tech-docs/jackandjill)
for Android projects.

For a complete example using the `net.ltgt.apt` plugin, see:
[examples/plugin/gradle](https://github.com/google/error-prone/tree/master/examples/plugin/gradle).

