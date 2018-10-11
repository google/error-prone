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

Starting in version 4.6, Gradle provides support for [configuring the processor
path](https://docs.gradle.org/4.6/release-notes.html#convenient-declaration-of-annotation-processor-dependencies):

```gradle
dependencies {
  annotationProcessor project(':custom-checks')
}
```

## Command-Line Arguments

Plugin checkers can accept additional configuration flags by defining
a single-argument constructor taking an `ErrorProneFlags` object (see
the [flags docs](http://errorprone.info/docs/flags)).  However, note
that plugin checkers must also define a zero-argument constructor, as
they are loaded by a `ServiceLoader`.  The actual checker instance
used by Error Prone will be constructed using the `ErrorProneFlags`
constructor.

