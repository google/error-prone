---
title: CompileTimeConstant
layout: bugpattern
category: GUAVA
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>GUAVA</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: CompileTimeConstant
__Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation.__

## The problem
A method or constructor with one or more parameters whose declaration is annotated with the @CompileTimeConstant type annotation must only be invoked with corresponding actual parameters that are computed as compile-time constant expressions, such as a literal or static final constant.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CompileTimeConstant")` annotation to the enclosing element.
