---
title: UnusedAnonymousClass
summary: Instance created but never used
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Creating a side-effect-free anonymous class and never using it is usually
a mistake.

For example:

```java
public static void main(String[] args) {
  new Thread(new Runnable() {
    @Override public void run() {
      preventMissionCriticalDisasters();
    }
  }); // did you mean to call Thread#start()?
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnusedAnonymousClass")` annotation to the enclosing element.
