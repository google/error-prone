---
title: PackageLocation
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: PackageLocation
__Package names should match the directory they are declared in__

## The problem
Java files should be located in a directory that matches the fully qualified name of the package. For example, classes in the package `edu.oswego.cs.dl.util.concurrent` should be located in: `.../edu/oswego/cs/dl/util/concurrent`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PackageLocation")` annotation to the enclosing element.
