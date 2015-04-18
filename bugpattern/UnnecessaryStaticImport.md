---
title: UnnecessaryStaticImport
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: UnnecessaryStaticImport
__Using static imports for types is unnecessary__

## The problem
Using static imports for types is unnecessary, since they can always be replaced by equivalent non-static imports.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnnecessaryStaticImport")` annotation to the enclosing element.
