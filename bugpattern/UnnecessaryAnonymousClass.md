---
title: UnnecessaryAnonymousClass
summary: Implementing a functional interface is unnecessary; prefer to implement the
  functional interface method directly and use a method reference instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer method references to anonymous classes that implement functional
interfaces.

That is, prefer this:

```java
private static Bar getBar(Foo foo) {
  return BarService.lookupBar(foo, defaultCredentials());
}

...

return someStream().map(MyClass::getBar)....;
```

to this:

```java
private static final Function<Foo, Bar> GET_BAR_FUNCTION =
    new Function<Foo, Bar>() {
      @Override
      public Bar apply(Foo foo) {
        return BarService.lookupBar(foo, defaultCredentials());
      }
    };

...

return someStream().map(GET_BAR_FUNCTION)....;
```

Advantages of using a method include:

*   It avoids hardcoding a named dependency on the functional interface type
    (e.g., is it a `com.google.common.base.Function` or a
    `java.util.function.Function`?)
*   It is easier to name the method than the constant.
*   It is more natural to test the method than the constant.
*   If the behavior is nontrivial, it's more natural to write javadoc for the
    method.

Be aware that this change is not purely syntactic: it affects the semantics of
your program in some small ways. In particular, evaluating the same method
reference twice is not guaranteed to return an identical object.

This means that, first, inlining the reference instead of using a constant may
cause additional memory allocations - usually this very slight performance cost
is worth the improved readability, but use your judgment if the performance
matters to you.

Secondly, if the correctness of your program depends on reference equality,
inlining the method reference may break you. Ideally, you should *not* depend on
reference equality, but if you are doing so, consider not making this change.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryAnonymousClass")` to the enclosing element.
