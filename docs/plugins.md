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
    name = "MyErrorPronePlugin",
    # a no-op annotation processor; the processor class can't be empty
    processor_class = "com.google.errorprone.sample.NullAnnotationProcessor",
    deps = [
        ":MyErrorPronePluginImplementation",
        # ...
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
