<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

---
title: NonFinalCompileTimeConstant
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

# Bug pattern: NonFinalCompileTimeConstant
__@CompileTimeConstant parameters should be final__

## The problem
If a method's formal parameter is annotated with @CompileTimeConstant, the method will always be invoked with an argument that is a static constant. If the parameter itself is non-final, then it is a mutable reference to immutable data. This is rarely useful, and can be confusing when trying to use the parameter in a context that requires an compile-time constant. For example:

    void f(@CompileTimeConstant y) {}
    void g(@CompileTimeConstant x) {
      f(x); // x is not a constant, did you mean to declare it as final?
    }



## Suppression
Suppress false positives by adding an `@SuppressWarnings("NonFinalCompileTimeConstant")` annotation to the enclosing element.
