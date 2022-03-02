---
title: StaticAssignmentOfThrowable
summary: Saving instances of Throwable in static fields is discouraged, prefer to
  create them on-demand when an exception is thrown
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The problem we're trying to prevent is unhelpful stack traces that don't contain
information about where the Exception was thrown from. This probem can sometimes
arise when an attempt is being made to cache or reuse a Throwable (often, a
particular Exception). In this case, consider whether this is really is
necessary: it often isn't. Could a Throwable simply be instantiated when needed?

``` {.bad}
// this always has the same stack trace
static final MY_EXCEPTION = new MyException("something terrible has happened!");
```

``` {.good}
throw new MyException("something terrible has happened!");
```

``` {.good}
static MyException myException() {
 return new MyException("something terrible has happened!");
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StaticAssignmentOfThrowable")` to the enclosing element.
