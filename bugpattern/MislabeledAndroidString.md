---
title: MislabeledAndroidString
summary: Certain resources in `android.R.string` have names that do not match their content
layout: bugpattern
category: ANDROID
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Certain resources in `android.R.string` have names that do not match their content: `android.R.string.yes` is actually "OK" and `android.R.string.no` is "Cancel". Avoid these string resources and prefer ones whose names *do* match their content. If you need "Yes" or "No" you must create your own string resources.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MislabeledAndroidString")` annotation to the enclosing element.
