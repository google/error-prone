---
title: ForOverride
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

# Bug pattern: ForOverride
__Method annotated @ForOverride must be protected or package-private and only invoked from declaring class__

## The problem
A method that overrides a @ForOverride method should not be invoked directly. Instead, it should be invoked only from the class in which it was declared. For example, if overriding Converter.doForward, you should invoke it through Converter.convert. For testing, factor out the code you want to run to a separate method.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ForOverride")` annotation to the enclosing element.
