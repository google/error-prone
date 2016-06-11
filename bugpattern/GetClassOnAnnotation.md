---
title: GetClassOnAnnotation
summary: Calling getClass() on an annotation may return a proxy class
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Instances of an annotation interface generally return a random proxy class when `getClass()` is called on them; to get the actual annotation type use `annotationType()`.

In the following example, calling `getClass()` on the annotation instance
returns a proxy class like `com.sun.proxy.$Proxy1`, while `annotationType()`
returns `Deprecated`.

```java
@Deprecated
public class Test {
  static void printAnnotationClass(Annotation annotation) {
    System.err.println(annotation.getClass());
    System.err.println(annotation.annotationType());
  }

  public static void main(String[] args) {
    printAnnotationClass(Test.class.getAnnotation(Deprecated.class));
  }
}
```

Prints:

```
class com.sun.proxy.$Proxy1
interface java.lang.Deprecated
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("GetClassOnAnnotation")` annotation to the enclosing element.
