---
title: DuplicateBranches
summary: Both branches contain identical code
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Branching constructs (`if` statements, `conditional` expressions) should contain
difference code in the two branches. Repeating identical code in both branches
is usually a bug.

For example:

```java
condition ? same : same
```

```java
if (condition) {
  same();
} else {
  same();
}
```

this usually indicates a typo where one of the branches was supposed to contain
different logic:

```java
condition ? something : somethingElse
```

```java
if (condition) {
  doSomething();
} else {
  doSomethingElse();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DuplicateBranches")` to the enclosing element.
