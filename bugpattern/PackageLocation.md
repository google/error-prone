---
title: PackageLocation
summary: Package names should match the directory they are declared in
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
Java files should be located in a directory that matches the fully qualified name of the package. For example, classes in the package `edu.oswego.cs.dl.util.concurrent` should be located in: `.../edu/oswego/cs/dl/util/concurrent`.

## Suppression
This check may not be suppressed.
