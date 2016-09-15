---
title: ReferenceEquality
summary: Comparison using reference equality instead of value equality
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Reference types that declare an `equals()` method, or that inherit `equals()`
from a type other than `Object`, should not be compared for reference equality
using `==` or `!=`. Instead, compare for value equality using `.equals()`.

## FAQs

### I am comparing interned objects.

It's dangerous to rely on instances being interned. We have no tooling to check
or enforce that, and it's easy to get wrong.

### I am doing a reference equality comparsion before a more expensive content equality comparison, for performance reasons.

We do exempt methods that override `Object#equals()` from this check. That said,
calling `Type#equals()` should be just as fast, because that method will likely
be inlined, and the first thing it will likely do is the same instance
comparison.

Alternatively, if you are OK with accepting null, you could call
`java.util.Objects.equals()`, which first does a reference equality comparison
and then falls back to content equality.

### In a test, I need to assert that two different references point to the same object (or to different objects).

Both Truth and JUnit provide clearer ways to assert this.

Truth:

```
assertThat(a).isSameAs(b);
assertThat(a).isNotSameAs(b);
```

JUnit:

```
assertSame(b, a);
assertNotSame(b, a);
```

### I need to compare against a special marker instance.

Classes override `equals` to express when two instances should be treated as
interchangeable with each other. Predominant Java libraries and practices are
built on that assumption. Defining a "magic instance" for such a type goes
against this whole practice, leaving you vulnerable to unexpected bugs.

Consider choosing a sentinel value within the domain of the type (the moral
equivalent of `-1` for indexOf function calls) that you could compare against
using the normal `equals` method.

### I need to put a special "nothing" value in my map.

Use `Optional<V>` as the value type of your map instead.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ReferenceEquality")` annotation to the enclosing element.
