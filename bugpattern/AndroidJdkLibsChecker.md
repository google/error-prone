---
title: AndroidJdkLibsChecker
summary: Use of class, field, or method that is not compatible with legacy Android
  devices
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: AndroidApiChecker_

## The problem
Code that needs to be compatible with Android cannot use types or members that
only the latest or unreleased devices can handle

## Suppression

WARNING: We *strongly* recommend checking your code with Android Lint if
suppressing or disabling this check.

The check can be suppressed in code that deliberately only targets newer Android
SDK versions.

To suppress for a particular statement, method, or class, use
`@SuppressWarnings`:

```
@SuppressWarnings("AndroidJdkLibsChecker") // TODO(user): document suppression
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AndroidJdkLibsChecker")` to the enclosing element.
