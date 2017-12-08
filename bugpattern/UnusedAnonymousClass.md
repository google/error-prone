---
title: UnusedAnonymousClass
summary: Instance created but never used
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnusedAnonymousClass")` to the enclosing element.
