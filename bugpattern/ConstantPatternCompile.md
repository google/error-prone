---
title: ConstantPatternCompile
summary: Variables initialized with Pattern#compile calls on constants can be constants
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Method bodies should generally not call
`java.util.regex.Pattern#compile(String)` with constant arguments. Instead,
define a constant to store that Pattern. This can avoid recompilation of the
regex every time the method is invoked.

That is, prefer this:

```java
private static final Pattern REGEX_PATTERN = Pattern.compile("a+");

public static boolean doSomething(String input) {
  Matcher matcher = REGEX_PATTERN.matcher(input);
  if (matcher.matches()) {
    ...
  }
}
```

to this:

```java
public static boolean doSomething(String input) {
  Matcher matcher = Pattern.compile("a+").matcher(input);
  if (matcher.matches()) {
    ...
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ConstantPatternCompile")` to the enclosing element.

