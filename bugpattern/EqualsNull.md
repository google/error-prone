---
title: EqualsNull
summary: The contract of Object.equals() states that for any non-null reference value
  x, x.equals(null) should return false. If x is null, a NullPointerException is thrown.
  Consider replacing equals() with the == operator.
layout: bugpattern
tags: FragileCode
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The contract of `Object.equals()` states that for any non-null reference value
`x`, `x.equals(null)` should return `false`. Thus code such as

```
if (x.equals(null)) {
  ...
}
```

either returns `false`, or throws a `NullPointerException` if `x` is `null`. The
nested block may never execute.

This check replaces `x.equals(null)` with `x == null`, and `!x.equals(null)`
with `x != null`. If the author intended for `x.equals(null)` to return `true`,
consider this as fragile code as it breaks the contract of `Object.equals()`.

See [Effective Java 3rd Edition §10: Objey the general contract when overriding
equals][ej3e-10] for more details.

[ej3e-10]: https://books.google.com/books?id=BIpDDwAAQBAJ

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsNull")` to the enclosing element.
