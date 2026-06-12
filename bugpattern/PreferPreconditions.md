---
title: PreferPreconditions
summary: Consider using Preconditions instead of explicit if-throw for parameter validation.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer Guava's [`Preconditions`](https://guava.dev/Preconditions) for checking
whether a constructor or method was invoked correctly (that is, whether its
*preconditions* were met). It is more concise than an explicit `if`-`throw`
block and clearly communicates the intent of the check.

## Why use `Preconditions`?

1.  **Conciseness**: What takes several lines with an `if`-`throw` block can
    often be done in a single line.
2.  **Clearer Intent**: Methods like `checkArgument` (for
    `IllegalArgumentException`) and `checkState` (for `IllegalStateException`)
    explicitly state what kind of validation is being performed.
3.  **Return Value**: `checkNotNull` returns the validated object, making it
    convenient for assignment or usage in a constructor (e.g., `this.foo =
    checkNotNull(foo);`)
4.  **Message Formatting**: `Preconditions` support simple `%s` formatting
    placeholders, avoiding the verbosity of `String.format` and the performance
    pitfalls of manual string concatenation for simple failures.

## Examples

### `checkNotNull`

```java
// Before
if (arg == null) {
  throw new NullPointerException("arg must not be null");
}

// After
checkNotNull(arg, "arg must not be null");
```

### `checkArgument`

```java
// Before
if (index < 0) {
  throw new IllegalArgumentException("index must be non-negative: " + index);
}

// After
checkArgument(index >= 0, "index must be non-negative: %s", index);
```

## Caveats

This check is conservative and will not suggest a refactoring if:

*   The failure message involves complex computation (like method calls) to
    avoid eager evaluation of the message string.
*   The `if` statement contains comments that might be lost during refactoring.
*   The `if` statement has an `else` block or is part of an `if-else-if` chain.
*   The condition contains logical operators (like `&&` or `||`) that would
    require a complex/unreadable negation.
*   The check is inside a method with the same name as the suggested
    `Preconditions` method (e.g., inside `checkNotNull` itself) to avoid
    infinite recursion.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreferPreconditions")` to the enclosing element.
