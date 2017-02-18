---
title: MissingCasesInEnumSwitch
summary: Switches on enum types should either handle all values, or have a default case.
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Consider a switch statement that doesn't handle all possible values and doesn't
have a default:

```java
enum Colors { RED, GREEN, BLUE }

switch (color) {
  case RED:
  case GREEN:
    paint(color);
    break;
}
```

The author's intent isn't clear. There are three possibilities:

1.  The default case is known to be impossible. This could be made clear by
    adding: \
    `default: throw new AssertionError();`

2.  The code intentionally 'falls out' of the switch on the default case, and
    execution continues below. This could be made clear by adding: \
    `default: // intentionally continue below`

3.  The code has a bug, and the missing cases should have been handled.

To avoid this ambiguity, the Google Java Style Guide [requires][style] each
switch statement to include a `default` statement group, even if it contains no
code.

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4.3-switch-default

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MissingCasesInEnumSwitch")` annotation to the enclosing element.
