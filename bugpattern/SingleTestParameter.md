---
title: SingleTestParameter
summary: '@TestParameter on the parameter is simpler than @TestParameters if there''s
  only one test parameter.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When a test method or constructor has only a single parameter, annotating that
parameter directly with `@TestParameter` is simpler and more concise than
annotating the method with `@TestParameters` YAML mappings.

```java
@Test
@TestParameters("{count: 1}")
@TestParameters("{count: 2}")
@TestParameters("{count: 3}")
public void myTest(int count) {
  ...
}
```

```java
@Test
public void myTest(@TestParameter({"1", "2", "3"}) int count) {
  ...
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SingleTestParameter")` to the enclosing element.
