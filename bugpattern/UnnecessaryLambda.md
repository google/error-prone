---
title: UnnecessaryLambda
summary: Returning a lambda from a helper method or saving it in a constant is unnecessary; prefer to implement the functional interface method directly and use a method reference instead.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Prefer method references to constant lambda expressions, or to a helper method
that does nothing but return a lambda.

That is, prefer this:

```java {.good}
private static Bar getBar(Foo foo) {
  return BarService.lookupBar(foo, defaultCredentials());
}

...

return someStream().map(MyClass::getBar)....;
```

to this:

```java
private static final Function<Foo, Bar> GET_BAR_FUNCTION =
    foo -> BarService.lookupBar(foo, defaultCredentials());

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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryLambda")` to the enclosing element.
