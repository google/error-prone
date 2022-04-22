---
title: SystemExitOutsideMain
summary: Code that contains System.exit() is untestable.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling `System.exit` terminates the java process and returns a status code.
Since it is disruptive to shut down the process within library code,
`System.exit` should not be called outside of a main method.

Instead of calling `System.exit` consider throwing an unchecked exception to
signal failure.

For example, prefer this:

```java
public static void main(String[] args) {
  try {
    doSomething(args[0]);
  } catch (MyUncheckedException e) {
    System.err.println(e.getMessage());
    System.exit(1);
  }
}

// In library code
public static void doSomething(String s) {
  try {
    doSomethingElse(s);
  } catch (MyCheckedException e) {
    throw new MyUncheckedException(e);
  }
}
```

to this:

```java
public static void main(String[] args) {
  doSomething(args[0]);
}

// In library code
public static void doSomething(String s) {
  try {
    doSomethingElse(s)
  } catch (MyCheckedException e) {
    System.err.println(e.getMessage());
    System.exit(1);
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SystemExitOutsideMain")` to the enclosing element.
