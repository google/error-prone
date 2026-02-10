---
title: UnnecessarySemicolon
summary: Unnecessary semicolons should be omitted. For empty block statements, prefer
  {}.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Java language allows optional semicolons in a few places where they serve no
purpose and can be distracting:

*   at the top level of a file, by [JLS §7.6]:

    ```java
    class Test {
    };
    ```

    > Extra ";" tokens appearing at the level of class and interface
    > declarations in a compilation unit have no effect on the meaning of the
    > compilation unit. Stray semicolons are permitted in the Java programming
    > language solely as a concession to C++ programmers who are used to placing
    > ";" after a class declaration. They should not be used in new Java code.

*   inside a class declaration, by [JLS §8.1.7]

    ```java
    class Test {
      ;
    }
    ```

*   as an empty statement, by [JLS §14.6]

    ```java
    class Test {
      void f() {
        ;
      }
    }
    ```

When a statement is required as the body of a control flow statement, for
example an `if` or `while`, prefer using `{}` to `;` for empty control flow
statements. That is, prefer this:

```java
while (true) {}
```

to this:

```java
while (true) ;
```

[JLS §7.6]: https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html#jls-7.6
[JLS §8.1.7]: https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.1.7
[JLS §14.6]: https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.6

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessarySemicolon")` to the enclosing element.
