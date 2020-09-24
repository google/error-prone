---
title: SwigMemoryLeak
summary: SWIG generated code that can't call a C++ destructor will leak memory
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
SWIG is a tool that will automatically generate Java bindings to C++ code. It is
possible to %ignore in SWIG a C++ object's destructor, this is trivially
achieved when using %ignoreall and then selectively %unignore-ing an API. SWIG
cleans up C++ objects using a delete method, which is most commonly called by a
finalizer. When a SWIG generated delete method can't call a destructor, as it is
hidden, the delete method throws an exception. However, in the case of a hidden
C++ destructor SWIG also doesn't generate a finalizer, and so the most common
call to the delete method is removed. The consequence of this is that the SWIG
objects leak their C++ counterpart and no warnings or exceptions are thrown.

This check looks for the pattern of a memory leaking SWIG generated object and
warns about the potential memory leak. The most straightforward fix is to the
SWIG input code to tell it not to %ignore the C++ code's destructor.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SwigMemoryLeak")` to the enclosing element.
