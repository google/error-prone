---
title: ExpensiveLenientFormatString
summary: String.format is passed to a lenient formatting method, which can be unwrapped
  to improve efficiency.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: PreconditionsExpensiveString_

## The problem
Lenient format strings, such as those accepted by `Preconditions`, are often
constructed lazily. The message is rarely needed, so it should either be cheap
to construct or constructed only when needed. This check ensures that these
messages are not constructed using expensive methods that are evaluated eagerly.

Prefer this:

```java
checkNotNull(foo, "hello %s", name);
```

instead of this:

```java
checkNotNull(foo, String.format("hello %s", name));
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ExpensiveLenientFormatString")` to the enclosing element.
