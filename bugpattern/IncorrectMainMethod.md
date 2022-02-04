---
title: IncorrectMainMethod
summary: '''main'' methods must be public, static, and void'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A `main` method must be `public`, `static`, and return `void` (see
[JLS §12.1.4]).

For example, the following method is confusing, because it is an overload of a
valid `main` method (it has the same name and signature), but is not a valid
`main` method:

```java
class Test {
  static void main(String[] args) {
    System.err.println("hello world");
  }
}
```

```
$ java T.java
error: 'main' method is not declared 'public static'
```

[JLS §12.1.4]: https://docs.oracle.com/javase/specs/jls/se11/html/jls-12.html#jls-12.1.4

TIP: If you're declaring a method that isn't intended to be used as the main
method of your program, prefer to use a name other than `main`. It's confusing
to humans and static analysis to see methods like `private int main(String[]
args)`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IncorrectMainMethod")` to the enclosing element.
