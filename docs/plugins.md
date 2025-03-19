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
java_library(
    name = "demo",
    srcs = ["java/com/example/Demo.java"],
    plugins = [":my_custom_check"],
)

java_plugin(
    name = "my_custom_check",
    srcs = ["java/com/example/MyCustomCheck.java"],
    deps = [
        ":auto_service",
        "@maven//:com_google_errorprone_error_prone_annotation",
        "@maven//:com_google_errorprone_error_prone_check_api",
    ],
)

java_library(
    name = "auto_service",
    exported_plugins = [
        ":auto_service_plugin",
    ],
    exports = [
        "@maven//:com_google_auto_service_auto_service_annotations",
    ],
)

java_plugin(
    name = "auto_service_plugin",
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = [
        "@maven//:com_google_auto_service_auto_service",
    ],
)
```

### Maven

Starting in version 3.5, maven-compiler-plugin allows the processor path to be
configured with the
[annotationProcessorPaths](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
parameter.

### Gradle

Starting in version 4.6, Gradle provides support for [configuring the processor
path](https://docs.gradle.org/4.6/release-notes.html#convenient-declaration-of-annotation-processor-dependencies):

```gradle
dependencies {
  annotationProcessor project(':custom-checks')
}
```

## Command-Line Arguments

Plugin checkers can accept additional configuration flags by defining a
single-argument constructor taking an `ErrorProneFlags` object (see the
[flags docs](https://errorprone.info/docs/flags)). However, note that plugin checkers
must also define a zero-argument constructor, as they are loaded by a
`ServiceLoader`. The actual checker instance used by Error Prone will be
constructed using the `ErrorProneFlags` constructor.
