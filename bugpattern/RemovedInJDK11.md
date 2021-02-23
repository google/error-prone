---
title: RemovedInJDK11
summary: This API is no longer supported in JDK 11
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The following methods are not available in JDK 11:

*   `SecurityManager.checkTopLevelWindow`,
    `SecurityManager.checkSystemClipboardAccess`,
    `SecurityManager.checkAwtEventQueueAccess`,
    `SecurityManager.checkMemberAccess`
    (https://bugs.openjdk.java.net/browse/JDK-8193032)

*   `Runtime.runFinalizersOnExit`, `System.runFinalizersOnExit`
    (https://bugs.openjdk.java.net/browse/JDK-8198250)

*   `Thread.destroy`, `Thread.stop(Throwable)`
    (https://bugs.openjdk.java.net/browse/JDK-8204243)

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RemovedInJDK11")` to the enclosing element.
