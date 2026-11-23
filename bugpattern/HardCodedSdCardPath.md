---
title: HardCodedSdCardPath
summary: Hardcoded reference to /sdcard
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: SdCardPath_

## The problem
Your code should not reference the `/sdcard` path directly, which is
platform-dependent. You should use
`Environment.getExternalStorageDirectory().getPath()` instead.

Similarly, do not reference the `/data/data/` path directly, as it can vary in
multi-user scenarios. You should use `Context.getFilesDir().getPath()` instead.

For more information, please see the documentation for
[android.os.Environment](https://developer.android.com/reference/android/os/Environment.html)
and
[android.content.Context](https://developer.android.com/reference/android/content/Context.html).

This check is an adaptation of the `SdCardPath` rule of
[Android Lint](https://tools.android.com/tips/lint-checks).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("HardCodedSdCardPath")` to the enclosing element.
