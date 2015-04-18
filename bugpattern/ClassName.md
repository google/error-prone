---
title: ClassName
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: ClassName
__The source file name should match the name of the top-level class it contains__

## The problem
Google Java Style Guide § 2.1 states, "The source file name consists of the case-sensitive name of the top-level class it contains, plus the .java extension."

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ClassName")` annotation to the enclosing element.
