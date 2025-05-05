---
title: MissingCasesInEnumSwitch
summary: Switches on enum types should either handle all values, or have a default
  case.
layout: bugpattern
tags: ''
severity: WARNING
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
    `default: throw new AssertionError(color);`

2.  The code intentionally 'falls out' of the switch on the default case, and
    execution continues below. This could be made clear by adding: \
    `default: // fall out`

3.  The code has a bug, and the missing cases should have been handled.

To avoid this ambiguity, the Google Java Style Guide [requires][style] each
switch statement on an enum type to either handle all values of the enum, or
have a default statement group.

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4.3-switch-default

## Library skew

If libraries are compiled against different versions of the same enum it's
possible for the switch statement to encounter an enum value despite it
otherwise being thought to be exhaustive. If there is no default branch code
execution will simply fall out of the switch statement.

Since developers may have assumed this to be impossible, it may be helpful to
add a default branch when library skew is a concern, however, you may not want
to give up checking to ensure that all cases are handled. Therefore if a default
branch exists with a comment containing "skew", the default will not be
considered for exhaustiveness. For example:

```java
enum TrafficLightColour { RED, GREEN, YELLOW }

void approachIntersection(TrafficLightColour state) {
  switch (state) {
    case GREEN:
      proceed();
      break;
    case YELLOW:
    case RED:
      stop();
      break;
    default: // In case of skew we may get an unknown value, always stop.
      stop();
      break;
  }
}
```

In this case the default branch is providing runtime safety for unknown enum
values while also still enforcing that all known enum values are handled.

Note: The [UnnecessaryDefaultInEnumSwitch](UnnecessaryDefaultInEnumSwitch.md)
check will not classify the default as unnecessary if it has the "skew" comment.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MissingCasesInEnumSwitch")` to the enclosing element.
