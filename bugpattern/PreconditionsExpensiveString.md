---
title: PreconditionsExpensiveString
summary: Second argument to Preconditions.* is a call to String.format(), which can
  be unwrapped
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Preconditions checks take an error message to display if the check fails. The
error message is rarely needed, so it should either be cheap to construct or
constructed only when needed. This check ensures that these error messages are
not constructed using expensive methods that are evaluated eagerly.

Prefer this:

```java
checkNotNull(foo, "hello %s", name);
```

instead of this:

```java
checkNotNull(foo, String.format("hello %s", name));
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreconditionsExpensiveString")` to the enclosing element.
