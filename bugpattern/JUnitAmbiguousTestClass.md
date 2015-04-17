<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

---
title: JUnitAmbiguousTestClass
layout: bugpattern
category: JUNIT
severity: WARNING
maturity: EXPERIMENTAL
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JUNIT</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: JUnitAmbiguousTestClass
__Test class mixes JUnit 3 and JUnit 4 idioms__

## The problem
The test class could execute either as a JUnit 3 class or a JUnit 4 class, and tests could behave differently depending on whether it runs in JUnit 3 or JUnit 4.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnitAmbiguousTestClass")` annotation to the enclosing element.
